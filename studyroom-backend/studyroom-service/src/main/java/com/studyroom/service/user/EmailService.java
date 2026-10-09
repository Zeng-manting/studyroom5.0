package com.studyroom.service.user;

/**
 * 邮件服务接口（Spring Mail SMTP）
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-004 邮箱验证码</li>
 *   <li>FR-UC-046 邮件兜底（预约成功/退款到账/停业通知，后续模块复用）</li>
 * </ul>
 *
 * @author studyroom
 */
public interface EmailService {

    /** 发送邮箱绑定验证码（6 位数字，5 分钟有效）—— FR-UC-004 */
    void sendVerifyCode(String email, String code);

    /** 发送绑定成功通知 —— FR-UC-004 */
    void sendBindSuccess(String email, String nickname);

    /**
     * 邮件兜底：预约成功通知（FR-UC-046）
     * <p>内部解析用户绑定邮箱；未绑定邮箱直接跳过；发送失败仅记日志。</p>
     *
     * @param userId     用户ID
     * @param orderNo    订单号
     * @param timeWindow 预约时段描述（如 09-13 14:00 ~ 16:00）
     */
    void sendOrderSuccessNotify(Long userId, String orderNo, String timeWindow);

    /**
     * 邮件兜底：退款到账通知（FR-UC-046）
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @param amount  退款金额
     * @param reason  退款原因
     */
    void sendRefundNotify(Long userId, String orderNo, java.math.BigDecimal amount, String reason);

    /**
     * 邮件兜底：停业通知（FR-UC-046，停业公告发布时批量调用）
     *
     * @param userId 用户ID
     * @param title  公告标题
     */
    void sendClosureNotify(Long userId, String title);
}
