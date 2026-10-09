package com.studyroom.service.order;

import java.math.BigDecimal;

/**
 * 钱包服务（余额操作 + 流水记录）
 * <p>FR-UC-023 余额支付扣减；FR-UC-027 阶梯退款回充；FR-UC-048 充值/消费流水（模块7复用）。</p>
 */
public interface WalletService {

    /**
     * 余额支付扣减（原子，余额不足抛业务异常）
     *
     * @param userId   用户ID
     * @param amount   扣减金额
     * @param orderId  关联订单ID
     * @param orderNo  关联订单号
     */
    void payFromBalance(Long userId, BigDecimal amount, Long orderId, String orderNo);

    /**
     * 退款回充余额（幂等由调用方订单状态保证）
     *
     * @param userId  用户ID
     * @param amount  回充金额
     * @param orderId 关联订单ID
     * @param remark  备注（如"阶梯退款 90%"）
     */
    void refundToBalance(Long userId, BigDecimal amount, Long orderId, String remark);

    /**
     * 查询用户当前余额
     */
    BigDecimal getBalance(Long userId);
}
