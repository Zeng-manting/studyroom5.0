package com.studyroom.service.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.common.utils.RedisUtils;
import com.studyroom.entity.order.OrderPause;
import com.studyroom.entity.order.OrderRefund;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.vo.order.OrderUsageVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.order.OrderPauseMapper;
import com.studyroom.mapper.order.OrderRefundMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.StoreMapper;
import com.studyroom.service.message.MessageService;
import com.studyroom.service.user.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 订单使用中服务实现（FR-UC-031 / FR-UC-033 / FR-UC-034）
 *
 * <p>所有状态流转均 CAS（WHERE 期望状态），天然幂等：
 * 重复点击暂停/结束、与延迟队列超时任务并发均安全。</p>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUsageServiceImpl implements OrderUsageService {

    private final OrderMapper orderMapper;
    private final OrderPauseMapper orderPauseMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final SeatMapper seatMapper;
    private final StoreMapper storeMapper;
    private final WalletService walletService;
    private final RedisUtils redisUtils;
    private final OrderDelayQueue orderDelayQueue;
    private final MessageService messageService;
    private final EmailService emailService;

    /* ==================== FR-UC-031：使用中页数据 ==================== */

    @Override
    public OrderUsageVO getUsage(Long userId, Long orderId) {
        Orders order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != Orders.STATUS_IN_USE && order.getStatus() != Orders.STATUS_FINISHED) {
            throw new BusinessException("订单不在使用中/已完成状态，无法查看使用详情");
        }
        return buildUsageVO(order);
    }

    /* ==================== FR-UC-033：中途临时离座（暂停） ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderUsageVO pause(Long userId, Long orderId) {
        Orders order = requireOwnedOrder(userId, orderId);
        LocalDateTime now = LocalDateTime.now();
        if (order.getStatus() != Orders.STATUS_IN_USE) {
            throw new BusinessException("仅进行中的订单可暂停");
        }
        if (order.getPauseStartTime() != null) {
            throw new BusinessException("订单已在暂停中，请先恢复使用");
        }
        if (!now.isBefore(order.getEndTime())) {
            throw new BusinessException("预约时段已结束，请直接结束使用");
        }

        LocalDateTime deadline = now.plusMinutes(CommonConstants.PAUSE_KEEP_MINUTES);
        // CAS：status=2 且未在暂停 → 置暂停态（防重复点击并发）
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_IN_USE)
                .isNull(Orders::getPauseStartTime)
                .set(Orders::getPauseStartTime, now)
                .set(Orders::getPauseEndTime, deadline)
                .setSql("pause_count = pause_count + 1"));
        if (rows == 0) {
            throw new BusinessException("暂停失败，订单状态已变更，请刷新重试");
        }

        // 暂停记录落库（FR-UC-033 status=0 暂停中）
        OrderPause pause = new OrderPause();
        pause.setId(IdGenerator.nextId());
        pause.setOrderId(orderId);
        pause.setPauseStart(now);
        pause.setStatus(OrderPause.STATUS_PAUSING);
        pause.setAutoEnd(0);
        orderPauseMapper.insert(pause);

        // Redis 计时器：studyroom:pause:{orderId}，30 分钟 TTL（FR-UC-033）
        String redisKey = CommonConstants.SEAT_PAUSE_KEY + orderId;
        redisUtils.set(redisKey, String.valueOf(pause.getId()),
                Duration.ofMinutes(CommonConstants.PAUSE_KEEP_MINUTES));

        // 投递暂停超时自动结算延迟任务（FR-SY-002）
        orderDelayQueue.offerPauseTimeout(orderId, pause.getId(), deadline);

        log.info("[OrderUsageService] FR-UC-033 暂停成功: orderNo={}, deadline={}",
                order.getOrderNo(), deadline);
        return buildUsageVO(orderMapper.selectById(orderId));
    }

    /* ==================== FR-UC-033：回座恢复 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderUsageVO resume(Long userId, Long orderId) {
        Orders order = requireOwnedOrder(userId, orderId);
        LocalDateTime now = LocalDateTime.now();
        if (order.getStatus() != Orders.STATUS_IN_USE) {
            throw new BusinessException("订单不在进行中，无法恢复");
        }
        if (order.getPauseStartTime() == null) {
            throw new BusinessException("订单不在暂停中");
        }

        // CAS：清除订单暂停态
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, Orders.STATUS_IN_USE)
                .isNotNull(Orders::getPauseStartTime)
                .set(Orders::getPauseStartTime, null)
                .set(Orders::getPauseEndTime, null));
        if (rows == 0) {
            throw new BusinessException("恢复失败，订单状态已变更，请刷新重试");
        }

        // 暂停记录 0暂停中 → 1已恢复（CAS：与超时自动结算并发安全）
        orderPauseMapper.update(null, new LambdaUpdateWrapper<OrderPause>()
                .eq(OrderPause::getOrderId, orderId)
                .eq(OrderPause::getStatus, OrderPause.STATUS_PAUSING)
                .set(OrderPause::getStatus, OrderPause.STATUS_RESUMED)
                .set(OrderPause::getPauseEnd, now));

        // 删除 Redis 计时器（延迟队列任务到点后 CAS 不命中，自然幂等失效）
        redisUtils.delete(CommonConstants.SEAT_PAUSE_KEY + orderId);

        log.info("[OrderUsageService] FR-UC-033 恢复使用: orderNo={}", order.getOrderNo());
        return buildUsageVO(orderMapper.selectById(orderId));
    }

    /* ==================== FR-UC-034：离场结束使用 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderUsageVO end(Long userId, Long orderId) {
        Orders order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != Orders.STATUS_IN_USE) {
            throw new BusinessException("订单不在进行中，无法结束");
        }
        // 暂停中结束：实际结束时间回退到暂停开始时刻（暂停期间未占用座位）
        LocalDateTime actualEnd = order.getPauseStartTime() != null
                ? order.getPauseStartTime() : LocalDateTime.now();
        return settle(order, actualEnd, false);
    }

    /* ==================== FR-SY-002：暂停超时自动结算 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean settlePauseTimeout(Long orderId, Long pauseId) {
        OrderPause pause = orderPauseMapper.selectById(pauseId);
        if (pause == null || !pause.getOrderId().equals(orderId)) {
            log.warn("[OrderUsageService] 暂停记录不存在，跳过: pauseId={}", pauseId);
            return false;
        }
        // CAS：0暂停中 → 2超时自动结算（已恢复/重复投递返回 false，天然幂等）
        int rows = orderPauseMapper.update(null, new LambdaUpdateWrapper<OrderPause>()
                .eq(OrderPause::getId, pauseId)
                .eq(OrderPause::getStatus, OrderPause.STATUS_PAUSING)
                .set(OrderPause::getStatus, OrderPause.STATUS_TIMEOUT_SETTLED)
                .set(OrderPause::getPauseEnd, pause.getPauseStart())
                .set(OrderPause::getAutoEnd, 1));
        if (rows == 0) {
            log.info("[OrderUsageService] 暂停已恢复/已结算，跳过超时任务: pauseId={}", pauseId);
            return false;
        }

        Orders order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != Orders.STATUS_IN_USE) {
            log.info("[OrderUsageService] 订单非进行中，暂停超时任务无需结算: orderNo={}",
                    order != null ? order.getOrderNo() : orderId);
            return false;
        }
        // 以暂停开始时刻为实际结束时间结算（用户自暂停起未回座）
        settle(order, pause.getPauseStart(), true);
        log.info("[OrderUsageService] FR-UC-033 暂停超时自动结算完成: orderNo={}", order.getOrderNo());
        return true;
    }

    /* ==================== 结算核心（end 与超时结算共用） ==================== */

    /**
     * 结算订单（FR-UC-034）
     * <ul>
     *   <li>实际时长 = 签到→实际结束 − 累计暂停分钟（封顶预约时长，不产生额外费用）</li>
     *   <li>未使用部分按比例退差额至余额 + order_refund 留痕</li>
     *   <li>订单 CAS 2→3 已完成；座位 1使用中 → 3打扫中</li>
     * </ul>
     */
    private OrderUsageVO settle(Orders order, LocalDateTime actualEnd, boolean autoEnd) {
        // 累计暂停分钟（含本次未闭合的暂停记录，按 pause_start→actualEnd 计）
        int pausedMinutes = orderPauseMapper.sumPausedMinutes(order.getId(), actualEnd);

        long bookedMinutes = Duration.between(order.getStartTime(), order.getEndTime()).toMinutes();
        long usedMinutes = order.getSignTime() == null ? 0
                : Duration.between(order.getSignTime(), actualEnd).toMinutes() - pausedMinutes;
        if (usedMinutes < 0) {
            usedMinutes = 0;
        }
        if (usedMinutes > bookedMinutes) {
            // 预付制：超出预约时长不额外扣费（续时能力属 FR-UC-055 模块8）
            usedMinutes = bookedMinutes;
        }
        int actualDuration = (int) usedMinutes;

        // 提前离场退差额：未使用分钟 / 预约分钟 × 实付（FR-UC-034 按实际时长扣费）
        BigDecimal refund = BigDecimal.ZERO;
        long unusedMinutes = bookedMinutes - actualDuration;
        if (unusedMinutes > 0 && order.getPayAmount().compareTo(BigDecimal.ZERO) > 0) {
            refund = order.getPayAmount()
                    .multiply(BigDecimal.valueOf(unusedMinutes))
                    .divide(BigDecimal.valueOf(bookedMinutes), 2, RoundingMode.HALF_UP);
        }

        // CAS：订单 2→3 已完成（幂等），同步写回退款金额（无差额为 0）
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, Orders.STATUS_IN_USE)
                .set(Orders::getStatus, Orders.STATUS_FINISHED)
                .set(Orders::getActualEndTime, actualEnd)
                .set(Orders::getActualDurationMinutes, actualDuration)
                .set(Orders::getRefundAmount, refund)
                .set(Orders::getPauseStartTime, null)
                .set(Orders::getPauseEndTime, null));
        if (rows == 0) {
            throw new BusinessException("结算失败，订单状态已变更，请刷新重试");
        }

        // 退款：回充余额 + order_refund 留痕（FR-UC-034）
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            walletService.refundToBalance(order.getUserId(), refund, order.getId(),
                    "提前离场退差额 " + order.getOrderNo());
            OrderRefund refundRecord = new OrderRefund();
            refundRecord.setId(IdGenerator.nextId());
            refundRecord.setOrderId(order.getId());
            refundRecord.setRefundAmount(refund);
            refundRecord.setRefundType(OrderRefund.TYPE_EARLY_LEAVE);
            // 退款比例 = 未使用分钟 / 预约分钟
            refundRecord.setRatio(BigDecimal.valueOf(unusedMinutes)
                    .divide(BigDecimal.valueOf(bookedMinutes), 2, RoundingMode.HALF_UP));
            refundRecord.setRefundReason((autoEnd ? "暂停超时自动结算" : "用户提前离场")
                    + "，实际使用" + actualDuration + "分钟，退未使用差额");
            refundRecord.setStatus(1);
            refundRecord.setRefundTime(LocalDateTime.now());
            orderRefundMapper.insert(refundRecord);
        }

        // 座位 1使用中 → 3打扫中（FR-UC-034；CAS 未命中时兜底强置，避免座位滞留使用中）
        if (seatMapper.casUpdateStatus(order.getSeatId(), 1, 3) == 0) {
            seatMapper.update(null, new LambdaUpdateWrapper<Seat>()
                    .eq(Seat::getId, order.getSeatId())
                    .set(Seat::getStatus, 3));
            log.warn("[OrderUsageService] 座位 CAS(1→3) 未命中，已兜底置打扫中: seatId={}", order.getSeatId());
        }

        // 清理暂停 Redis 计时器（若有）
        redisUtils.delete(CommonConstants.SEAT_PAUSE_KEY + order.getId());

        // FR-UC-045 消息推送（离场结算结果；有退款时附 FR-UC-046 邮件兜底）
        messageService.sendOrderMessage(order.getUserId(),
                autoEnd ? "暂停超时已自动结算" : "使用结束",
                "订单 " + order.getOrderNo() + " 已结算：实际使用 " + actualDuration + " 分钟"
                        + (refund.compareTo(BigDecimal.ZERO) > 0
                        ? "，退未使用差额 ¥" + refund + " 已退回账户余额。" : "。"),
                order.getId());
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            emailService.sendRefundNotify(order.getUserId(), order.getOrderNo(), refund,
                    (autoEnd ? "暂停超时自动结算" : "用户提前离场") + "退未使用差额");
        }

        log.info("[OrderUsageService] FR-UC-034 结算完成: orderNo={}, used={}min, paused={}min, refund={}, autoEnd={}",
                order.getOrderNo(), actualDuration, pausedMinutes, refund, autoEnd);
        return buildUsageVO(orderMapper.selectById(order.getId()));
    }

    /* ==================== 私有辅助 ==================== */

    private Orders requireOwnedOrder(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private OrderUsageVO buildUsageVO(Orders order) {
        OrderUsageVO vo = new OrderUsageVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        vo.setSignTime(order.getSignTime());
        vo.setPauseCount(order.getPauseCount());

        Store store = storeMapper.selectById(order.getStoreId());
        vo.setStoreName(store != null ? store.getName() : null);
        vo.setStoreAddress(store != null ? store.getAddress() : null);
        Seat seat = seatMapper.selectById(order.getSeatId());
        vo.setSeatNo(seat != null ? seat.getSeatNo() : null);

        LocalDateTime now = LocalDateTime.now();
        // FR-UC-055 续时信息：进行中且未暂停（且时段未走完）可续时
        vo.setExtendCount(order.getExtendCount());
        if (order.getStatus() == Orders.STATUS_IN_USE) {
            boolean paused = order.getPauseStartTime() != null;
            vo.setPaused(paused);
            vo.setExtendable(!paused && now.isBefore(order.getEndTime()));
            if (paused) {
                vo.setPauseDeadline(order.getPauseEndTime());
                long pauseRemaining = Duration.between(now, order.getPauseEndTime()).toMinutes();
                vo.setPauseRemainingMinutes((int) Math.max(pauseRemaining, 0));
                // 剩余使用时间：暂停期间不流逝，按暂停开始时刻口径计算
                long remaining = Duration.between(order.getPauseStartTime(), order.getEndTime()).toMinutes();
                vo.setRemainingMinutes((int) Math.max(remaining, 0));
            } else {
                long remaining = Duration.between(now, order.getEndTime()).toMinutes();
                vo.setRemainingMinutes((int) Math.max(remaining, 0));
                vo.setTotalPausedMinutes(orderPauseMapper.sumPausedMinutes(order.getId(), now));
            }
        } else {
            // 已完成：展示结算结果（FR-UC-034）
            vo.setPaused(false);
            vo.setRemainingMinutes(0);
            vo.setExtendable(false);
            vo.setActualEndTime(order.getActualEndTime());
            vo.setActualDurationMinutes(order.getActualDurationMinutes());
            vo.setRefundAmount(order.getRefundAmount() != null
                    ? order.getRefundAmount() : BigDecimal.ZERO);
        }
        return vo;
    }
}
