package com.studyroom.service.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.entity.order.Orders;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.service.coupon.CouponService;
import com.studyroom.service.message.MessageService;
import com.studyroom.service.user.CreditService;
import com.studyroom.service.user.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 订单超时任务处理器（FR-SY-002 延迟队列消费端）
 * <ul>
 *   <li>FR-UC-025 支付超时：15 分钟未支付自动取消 + 释放优惠券绑定</li>
 *   <li>FR-UC-026 未签到取消：开始后 30 分钟未签到自动取消 + 标记爽约 + 按比例退款</li>
 *   <li>FR-UC-033 暂停超时：暂停 30 分钟未回座自动结算（委托 {@link OrderUsageService}）</li>
 * </ul>
 * <p>所有状态流转均使用 CAS（WHERE status = 期望值），天然幂等：
 * 任务重复投递 / 与用户操作并发时不会重复处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final CouponService couponService;
    private final WalletService walletService;
    private final OrderUsageService orderUsageService;
    private final MessageService messageService;
    private final EmailService emailService;
    private final CreditService creditService;

    /**
     * 支付超时处理（FR-UC-025）
     * <p>座位占用由订单状态保证：订单取消后座位自动"释放"（座位图占用查询不再命中）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePayTimeout(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("[OrderTimeoutTask] 订单不存在，跳过: {}", orderId);
            return;
        }
        // 幂等：仅待支付状态需要取消
        if (order.getStatus() != Orders.STATUS_PENDING_PAY) {
            log.info("[OrderTimeoutTask] 订单已非待支付(status={})，跳过支付超时: {}",
                    order.getStatus(), order.getOrderNo());
            return;
        }
        // CAS：status 0 → 4
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                .set(Orders::getStatus, Orders.STATUS_CANCELED)
                .set(Orders::getCancelTime, LocalDateTime.now())
                .set(Orders::getCancelReason, "支付超时自动取消（15分钟未支付）"));
        if (rows == 0) {
            return; // 并发：用户恰好此刻支付/取消成功
        }
        // 释放优惠券绑定（FR-UC-024）
        couponService.releaseFromOrder(order.getCouponId(), orderId);
        // FR-UC-045 消息推送（支付超时取消）
        messageService.sendOrderMessage(order.getUserId(), "订单已超时取消",
                "订单 " + order.getOrderNo() + " 15 分钟未支付已自动取消，占用的座位已释放。",
                orderId);
        log.info("[OrderTimeoutTask] FR-UC-025 支付超时取消成功: orderNo={}", order.getOrderNo());
    }

    /**
     * 未签到自动取消（FR-UC-026）
     * <p>已预约(status=1) 且开始后 30 分钟仍未签到 → 标记爽约、
     * 按阶梯退款（此时距开始已超时 → 70% 档）、取消订单、释放券，
     * 并触发爽约信用记账（FR-UC-053/054：扣分 + 30 天计数，达 3 次限制预约 7 天）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleNoShowCancel(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        // 幂等：仅"已预约且未签到"需要处理；进行中/已完成/已取消全部跳过
        if (order.getStatus() != Orders.STATUS_RESERVED || order.getSignTime() != null) {
            log.info("[OrderTimeoutTask] 订单无需未签到取消(status={}, signed={})，跳过: {}",
                    order.getStatus(), order.getSignTime() != null, order.getOrderNo());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        // FR-UC-027 阶梯退款：已过开始时间 → <15 分钟档 70%
        BigDecimal ratio = refundRatio(now, order.getStartTime());
        BigDecimal refund = order.getPayAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);

        // CAS：status 1 → 6（已退款），同时标记爽约
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_RESERVED)
                .set(Orders::getStatus, Orders.STATUS_REFUNDED)
                .set(Orders::getIsNoShow, 1)
                .set(Orders::getCancelTime, now)
                .set(Orders::getCancelReason, "开始后30分钟未签到，系统自动取消")
                .set(Orders::getRefundAmount, refund)
                .set(Orders::getRefundTime, now)
                .set(Orders::getRefundReason,
                        "未签到自动取消，按" + ratio.multiply(BigDecimal.valueOf(100))
                                .stripTrailingZeros().toPlainString() + "%比例退款"));
        if (rows == 0) {
            return; // 并发：用户恰好此刻签到/取消成功
        }
        // 退款回充余额（FR-UC-026 按比例退款）
        walletService.refundToBalance(order.getUserId(), refund, orderId,
                "未签到自动取消退款 " + order.getOrderNo());
        // 释放优惠券
        couponService.releaseFromOrder(order.getCouponId(), orderId);
        // FR-UC-053/054 爽约信用记账：扣信用分 + 30 天计数，达 3 次自动置 7 天预约限制
        creditService.recordNoShow(order);
        // FR-UC-045 消息推送 + FR-UC-046 邮件兜底（爽约取消退款）
        messageService.sendOrderMessage(order.getUserId(), "未签到订单已取消",
                "订单 " + order.getOrderNo() + " 开始后 30 分钟未签到，已自动取消并标记爽约，"
                        + "退款 ¥" + refund + " 已退回账户余额。", orderId);
        emailService.sendRefundNotify(order.getUserId(), order.getOrderNo(), refund,
                "开始后30分钟未签到自动取消");
        log.info("[OrderTimeoutTask] FR-UC-026 未签到自动取消: orderNo={}, refund={}",
                order.getOrderNo(), refund);
    }

    /**
     * 暂停超时自动结算（FR-UC-033 + FR-SY-002）
     * <p>暂停 30 分钟未回座 → 以暂停开始时刻结算订单（实际时长扣除暂停），
     * 结算细节（退差额/座位转打扫中）由 {@link OrderUsageService#settlePauseTimeout} 完成。</p>
     */
    public void handlePauseTimeout(Long orderId, Long pauseId) {
        orderUsageService.settlePauseTimeout(orderId, pauseId);
    }

    /**
     * 阶梯退款比例（FR-UC-027）：
     * 距开始 &gt;60min → 100%；15~60min → 90%；&lt;15min（含已开始）→ 70%
     */
    private BigDecimal refundRatio(LocalDateTime cancelTime, LocalDateTime startTime) {
        long minutes = java.time.Duration.between(startTime, cancelTime).toMinutes();
        // minutes < 0 表示尚未开始（提前取消）
        long before = minutes < 0 ? -minutes : 0;
        if (before > CommonConstants.REFUND_FULL_MINUTES) {
            return BigDecimal.ONE;
        } else if (before >= 15) {
            return CommonConstants.REFUND_RATIO_90;
        } else {
            return CommonConstants.REFUND_RATIO_70;
        }
    }
}
