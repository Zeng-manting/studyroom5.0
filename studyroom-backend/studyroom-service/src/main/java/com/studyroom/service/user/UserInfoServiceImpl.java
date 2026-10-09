package com.studyroom.service.user;

import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.properties.CryptoProperties;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.CryptoUtils;
import com.studyroom.common.utils.RedisUtils;
import com.studyroom.entity.dto.user.BindEmailDTO;
import com.studyroom.entity.dto.user.SendEmailCodeDTO;
import com.studyroom.entity.dto.user.UpdateProfileDTO;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.user.EmailStatusVO;
import com.studyroom.entity.vo.user.UserVO;
import com.studyroom.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;

/**
 * 用户信息服务实现
 *
 * <h3>邮箱绑定安全设计（FR-UC-004）</h3>
 * <ul>
 *   <li>验证码 6 位数字，有效期 5 分钟，一次性消费（验证即删）</li>
 *   <li>同一邮箱 60 秒内只能发一次（key TTL 兼做冷却）</li>
 *   <li>每用户每天最多发送 5 次（Redis INCR + 当日过期）</li>
 *   <li>邮箱明文仅存在于请求与内存，落库前 AES-256-CBC 加密（密文 Base64 存 VARBINARY）</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements UserInfoService {

    /** 验证码有效期（分钟） */
    private static final int CODE_TTL_MINUTES = 5;

    /** 每用户每日发送上限 */
    private static final int DAILY_LIMIT = 5;

    private final UserMapper userMapper;
    private final RedisUtils redisUtils;
    private final CryptoProperties cryptoProperties;
    private final EmailService emailService;

    // ==================== FR-UC-003 用户信息 ====================

    @Override
    public UserVO getMyInfo(Long userId) {
        User user = requireUser(userId);
        return toUserVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = requireUser(userId);

        // FR-UC-003：仅更新传入字段（微信 chooseAvatar + nickname 输入框）
        User patch = new User();
        patch.setId(userId);
        if (dto.getAvatar() != null && !dto.getAvatar().isEmpty()) {
            patch.setAvatar(dto.getAvatar());
        }
        if (dto.getNickname() != null && !dto.getNickname().isEmpty()) {
            patch.setNickname(dto.getNickname().trim());
        }
        if (dto.getGender() != null) {
            patch.setGender(dto.getGender());
        }
        userMapper.updateById(patch);
        log.info("[UserInfoServiceImpl] 资料更新 uid={}", userId);
        return toUserVO(requireUser(userId));
    }

    // ==================== FR-UC-004 邮箱绑定 ====================

    @Override
    public void sendEmailCode(Long userId, SendEmailCodeDTO dto) {
        requireUser(userId);
        String email = dto.getEmail().trim().toLowerCase();

        // 1) 冷却校验：验证码 key 存在即 60s 内已发过（key TTL=5min 不可兼做 60s 冷却，
        //    故冷却独立 key，60 秒过期）
        String cooldownKey = CommonConstants.EMAIL_CODE_KEY + "cd:" + email;
        if (Boolean.TRUE.equals(redisUtils.hasKey(cooldownKey))) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "发送过于频繁，请 1 分钟后再试");
        }

        // 2) 每日上限：每用户 5 次/天
        String dailyKey = CommonConstants.EMAIL_CODE_DAILY_KEY + userId + ":" + LocalDate.now();
        Long sent = redisUtils.incr(dailyKey);
        if (sent != null && sent == 1L) {
            redisUtils.expire(dailyKey, Duration.ofDays(1));
        }
        if (sent != null && sent > DAILY_LIMIT) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "今日验证码发送次数已达上限，请明天再试");
        }

        // 3) 生成 6 位数字验证码并写入 Redis（5 分钟有效）
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redisUtils.set(CommonConstants.EMAIL_CODE_KEY + email, code, Duration.ofMinutes(CODE_TTL_MINUTES));
        // 写冷却标记（60 秒）
        redisUtils.set(cooldownKey, "1", Duration.ofSeconds(60));

        // 4) SMTP 发送
        emailService.sendVerifyCode(email, code);
        log.info("[UserInfoServiceImpl] 验证码已发送 uid={} email={}", userId, maskEmail(email));
    }

    @Override
    public void bindEmail(Long userId, BindEmailDTO dto) {
        User user = requireUser(userId);
        String email = dto.getEmail().trim().toLowerCase();

        // 1) 验证码校验（一次性消费）
        String codeKey = CommonConstants.EMAIL_CODE_KEY + email;
        String cached = redisUtils.get(codeKey);
        if (cached == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码已过期，请重新获取");
        }
        if (!cached.equals(dto.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误");
        }
        redisUtils.delete(codeKey);

        // 2) AES-256-CBC 加密落库 —— FR-UC-004（Base64 字节存 VARBINARY）
        String cipher = CryptoUtils.aesEncrypt(email, cryptoProperties.getAesKey());
        User patch = new User();
        patch.setId(userId);
        patch.setEmailEncrypted(cipher.getBytes(StandardCharsets.UTF_8));
        userMapper.updateById(patch);
        log.info("[UserInfoServiceImpl] 邮箱绑定成功 uid={} email={}", userId, maskEmail(email));

        // 预热：立即置空提示用的缓存信息（无需）
        // 邮件通知（FR-UC-046 邮件兜底在关键节点复用 EmailService）
        emailService.sendBindSuccess(email, user.getNickname());
    }

    @Override
    public EmailStatusVO getEmailStatus(Long userId) {
        User user = requireUser(userId);
        EmailStatusVO vo = new EmailStatusVO();
        boolean bound = user.getEmailEncrypted() != null && user.getEmailEncrypted().length > 0;
        vo.setBound(bound);
        vo.setEmailMasked(bound ? decryptAndMask(user.getEmailEncrypted()) : null);
        vo.setCodeTtlSeconds(CODE_TTL_MINUTES * 60);
        return vo;
    }

    // ==================== 公共转换 ====================

    @Override
    public UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        boolean bound = user.getEmailEncrypted() != null && user.getEmailEncrypted().length > 0;
        vo.setEmailBound(bound);
        vo.setEmailMasked(bound ? decryptAndMask(user.getEmailEncrypted()) : null);
        vo.setBalance(user.getBalance());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreditStatus(user.getCreditStatus());
        vo.setCreditLimitEnd(user.getCreditLimitEnd());
        vo.setNoShowCount30d(user.getNoShowCount30d());
        vo.setPrivacyAgreed(user.getPrivacyAgreed() != null && user.getPrivacyAgreed() == 1);
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    // ==================== 内部方法 ====================

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已注销");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return user;
    }

    /** 解密邮箱并脱敏：a***@example.com */
    private String decryptAndMask(byte[] cipherBytes) {
        try {
            String cipher = new String(cipherBytes, StandardCharsets.UTF_8);
            String email = CryptoUtils.aesDecrypt(cipher, cryptoProperties.getAesKey());
            return maskEmail(email);
        } catch (Exception e) {
            log.error("[UserInfoServiceImpl] 邮箱解密失败（密钥是否变更？）: {}", e.getMessage());
            return null;
        }
    }

    /** 邮箱脱敏：保留首字符与域名 */
    static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
