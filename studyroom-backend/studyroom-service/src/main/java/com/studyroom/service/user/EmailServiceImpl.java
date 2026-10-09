package com.studyroom.service.user;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.properties.CryptoProperties;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.CryptoUtils;
import com.studyroom.entity.user.User;
import com.studyroom.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 邮件服务实现（Spring Mail + SMTP）
 *
 * <p>发送走 @Async 线程池，SMTP 抖动不阻塞登录/绑定主流程；
 * 发送失败抛业务异常由全局异常处理返回提示（验证码已写入 Redis，可让用户重试）。</p>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-004 邮箱验证码（验证码内容只经邮件渠道下发）</li>
 *   <li>FR-UC-046 邮件兜底（本类后续扩展预约成功/退款到账/停业通知模板）</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final UserMapper userMapper;
    private final CryptoProperties cryptoProperties;

    /** 发件人地址（application.yml studyroom.email.from） */
    @Value("${studyroom.email.from:noreply@studyroom.local}")
    private String from;

    @Async
    @Override
    public void sendVerifyCode(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【无人自习室】邮箱验证码");
            message.setText("您正在绑定邮箱，验证码为：" + code + "\n\n"
                    + "验证码 5 分钟内有效，请勿泄露给他人。\n"
                    + "如非本人操作，请忽略本邮件。");
            mailSender.send(message);
            log.info("[EmailServiceImpl] 验证码邮件已发送 to={}", UserInfoServiceImpl.maskEmail(email));
        } catch (Exception e) {
            log.error("[EmailServiceImpl] 验证码邮件发送失败 to={}: {}", email, e.getMessage());
            throw new BusinessException(ResultCode.ERROR, "邮件发送失败，请确认邮箱地址或稍后重试");
        }
    }

    @Async
    @Override
    public void sendBindSuccess(String email, String nickname) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【无人自习室】邮箱绑定成功");
            message.setText((nickname == null ? "亲爱的用户" : "亲爱的 " + nickname) + "：\n\n"
                    + "您的邮箱已成功绑定，后续关键通知（预约成功、退款到账等）将通过该邮箱发送。\n\n"
                    + "—— 无人自习室");
            mailSender.send(message);
        } catch (Exception e) {
            // 通知类邮件失败不影响主流程
            log.warn("[EmailServiceImpl] 绑定成功通知发送失败 to={}: {}", email, e.getMessage());
        }
    }

    /* ==================== FR-UC-046 邮件兜底（三个关键节点） ==================== */

    @Async
    @Override
    public void sendOrderSuccessNotify(Long userId, String orderNo, String timeWindow) {
        String email = resolveEmail(userId);
        if (email == null) {
            return; // 未绑定邮箱，跳过
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【无人自习室】预约成功");
            message.setText("您的预约已支付成功：\n\n"
                    + "订单号：" + orderNo + "\n"
                    + "预约时段：" + timeWindow + "\n\n"
                    + "请在开始时间前后 15 分钟内到店扫码签到，超时 30 分钟未签到将自动取消并扣退款。\n\n"
                    + "—— 无人自习室");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("[EmailServiceImpl] 预约成功兜底邮件发送失败 userId={}: {}", userId, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendRefundNotify(Long userId, String orderNo, BigDecimal amount, String reason) {
        String email = resolveEmail(userId);
        if (email == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【无人自习室】退款到账通知");
            message.setText("您的订单退款已到账：\n\n"
                    + "订单号：" + orderNo + "\n"
                    + "退款金额：¥" + amount + "\n"
                    + "退款原因：" + reason + "\n\n"
                    + "款项已退回您的账户余额，请注意查收。\n\n"
                    + "—— 无人自习室");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("[EmailServiceImpl] 退款兜底邮件发送失败 userId={}: {}", userId, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendClosureNotify(Long userId, String title) {
        String email = resolveEmail(userId);
        if (email == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("【无人自习室】停业通知");
            message.setText("尊敬的用户：\n\n"
                    + "门店发布停业通知：" + title + "\n\n"
                    + "详情请打开小程序查看公告。已预约受影响的订单将按规则退款。\n\n"
                    + "—— 无人自习室");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("[EmailServiceImpl] 停业兜底邮件发送失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 解析用户绑定邮箱（AES 解密；未绑定返回 null）
     */
    private String resolveEmail(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null || user.getEmailEncrypted() == null || user.getEmailEncrypted().length == 0) {
                return null;
            }
            return CryptoUtils.aesDecrypt(new String(user.getEmailEncrypted(),
                    java.nio.charset.StandardCharsets.UTF_8), cryptoProperties.getAesKey());
        } catch (Exception e) {
            log.warn("[EmailServiceImpl] 邮箱解密失败 userId={}: {}", userId, e.getMessage());
            return null;
        }
    }
}
