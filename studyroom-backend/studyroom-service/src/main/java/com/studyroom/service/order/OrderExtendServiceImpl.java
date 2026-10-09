package com.studyroom.service.order;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.common.utils.RedissonUtils;
import com.studyroom.entity.dto.order.OrderExtendDTO;
import com.studyroom.entity.order.OrderExtension;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.pkg.Package;
import com.studyroom.entity.vo.order.OrderExtendPreviewVO;
import com.studyroom.entity.vo.order.OrderUsageVO;
import com.studyroom.mapper.order.OrderExtensionMapper;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.pkg.PackageMapper;
import com.studyroom.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 进行中续时服务实现（FR-UC-055）
 *
 * <p>计费口径：按订单当前套餐单价折算 —— 套餐售价 ÷ 套餐总小时 = 元/小时，再乘续时小时数；
 * 日/周/月卡按 24/168/720 小时折算。续时费用支付后即时顺延 end_time 并续占座位；
 * 续时部分费用不参与提前离场按比例退差（FR-UC-034 结算仍按原订单实付口径）。</p>
 *
 * <p>并发控制：Redisson 锁 order:extend:lock:{orderId}（5s）保护"校验+支付+顺延"临界区，
 * 防止并发续时、与离场/暂停超时结算竞争；订单顺延 SQL 附带 status=2 AND end_time=旧值
 * 双重 CAS 兜底，锁内事务提交后才释放锁。</p>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExtendServiceImpl implements OrderExtendService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderMapper orderMapper;
    private final OrderExtensionMapper orderExtensionMapper;
    private final PackageMapper packageMapper;
    private final WalletService walletService;
    private final OrderUsageService orderUsageService;
    private final MessageService messageService;
    private final RedissonUtils redissonUtils;
    private final TransactionTemplate transactionTemplate;

    /* ==================== FR-UC-055：续时费用预览 ==================== */

    @Override
    public OrderExtendPreviewVO preview(Long userId, Long orderId, Integer hours) {
        Orders order = requireExtendable(userId, orderId);
        Package pkg = packageMapper.selectById(order.getPackageId());
        if (pkg == null) {
            throw new BusinessException("订单套餐已不存在，无法续时");
        }

        BigDecimal unitPrice = unitPrice(pkg);
        LocalDateTime newEnd = order.getEndTime().plusHours(hours);

        // FR-UC-055 续占座位：顺延时段不可与该座位新订单冲突
        checkExtendConflict(order, newEnd);

        OrderExtendPreviewVO vo = new OrderExtendPreviewVO();
        vo.setOrderId(orderId);
        vo.setPackageName(pkg.getName());
        vo.setCurrentEndTime(order.getEndTime());
        vo.setHours(hours);
        vo.setUnitPrice(unitPrice);
        vo.setAmount(unitPrice.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP));
        vo.setNewEndTime(newEnd);
        vo.setBalance(walletService.getBalance(userId));
        vo.setExtendCount(order.getExtendCount());
        return vo;
    }

    /* ==================== FR-UC-055：执行续时 ==================== */

    @Override
    public OrderUsageVO extend(Long userId, Long orderId, OrderExtendDTO dto) {
        int hours = dto.getHours();
        int payType = dto.getPayType();

        // FR-UC-055 Redisson 分布式锁：防并发续时 / 与结算任务竞争
        String lockKey = CommonConstants.ORDER_EXTEND_LOCK_KEY + orderId;
        try (RedissonUtils.RLockWrapper lock = redissonUtils.tryLock(lockKey, Duration.ofSeconds(5))) {
            if (lock == null) {
                throw new BusinessException("续时请求处理中，请勿重复提交");
            }
            // 锁内重读订单（锁外读到的可能已被并发修改）
            Orders order = requireExtendable(userId, orderId);
            Package pkg = packageMapper.selectById(order.getPackageId());
            if (pkg == null) {
                throw new BusinessException("订单套餐已不存在，无法续时");
            }

            BigDecimal amount = unitPrice(pkg)
                    .multiply(BigDecimal.valueOf(hours))
                    .setScale(2, RoundingMode.HALF_UP);
            LocalDateTime oldEnd = order.getEndTime();
            LocalDateTime newEnd = oldEnd.plusHours(hours);

            // 续占座位冲突校验（FR-UC-055）
            checkExtendConflict(order, newEnd);

            // 事务：支付 → 续时流水 → 订单 end_time 顺延（CAS 双保险）
            transactionTemplate.executeWithoutResult(txStatus -> doExtend(order, payType, hours, amount, oldEnd, newEnd));

            // 事务提交后：FR-UC-045 消息推送（best-effort）
            messageService.sendOrderMessage(userId, "续时成功",
                    "订单 " + order.getOrderNo() + " 已续时 " + hours + " 小时（¥" + amount
                            + "），新结束时间 " + newEnd.format(TIME_FORMATTER) + "，座位已为您保留。",
                    orderId);
            log.info("[OrderExtend] FR-UC-055 续时成功: orderNo={}, hours={}, amount={}, newEnd={}",
                    order.getOrderNo(), hours, amount, newEnd);
        }
        // 返回最新使用状态（含新的剩余时间与续时次数）
        return orderUsageService.getUsage(userId, orderId);
    }

    /**
     * 续时事务体：支付 → order_extension 流水 → CAS 顺延订单结束时间
     */
    private void doExtend(Orders order, int payType, int hours, BigDecimal amount,
                          LocalDateTime oldEnd, LocalDateTime newEnd) {
        // FR-UC-023 支付方式：1余额（原子扣减，不足回滚）/ 2模拟微信
        if (payType == Orders.PAY_TYPE_BALANCE) {
            walletService.payFromBalance(order.getUserId(), amount, order.getId(),
                    "续时支付 " + order.getOrderNo());
        } else if (payType != Orders.PAY_TYPE_WECHAT) {
            throw new BusinessException("不支持的支付方式");
        }

        // 续时流水（FR-UC-055）
        OrderExtension ext = new OrderExtension();
        ext.setId(IdGenerator.nextId());
        ext.setOrderId(order.getId());
        ext.setHours(hours);
        ext.setAmount(amount);
        ext.setPayType(payType);
        ext.setPayStatus(OrderExtension.PAY_STATUS_PAID);
        ext.setPayTime(LocalDateTime.now());
        ext.setNewEndTime(newEnd);
        orderExtensionMapper.insert(ext);

        // CAS 顺延：status=2 且 end_time 仍为旧值（与离场结算/超时结算竞争时失败回滚支付）
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, Orders.STATUS_IN_USE)
                .eq(Orders::getEndTime, oldEnd)
                .set(Orders::getEndTime, newEnd)
                .setSql("extend_count = extend_count + 1"));
        if (rows == 0) {
            throw new BusinessException("续时失败，订单状态或结束时间已变更，请刷新后重试");
        }
    }

    /* ==================== 私有辅助 ==================== */

    /**
     * 续时前置校验：归属 + 进行中 + 未暂停（FR-UC-055）
     */
    private Orders requireExtendable(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Orders.STATUS_IN_USE) {
            throw new BusinessException("仅使用中的订单可续时");
        }
        if (order.getPauseStartTime() != null) {
            throw new BusinessException("订单暂停中，请先恢复使用再续时");
        }
        return order;
    }

    /**
     * 续占座位冲突校验（FR-UC-055）：顺延时段 [原结束, 新结束) 内
     * 座位不可有新订单占用、本人不可有新预约
     */
    private void checkExtendConflict(Orders order, LocalDateTime newEnd) {
        // 本订单 end_time = 原结束时间，不满足 end_time > startTime，不会自匹配
        if (orderMapper.countSeatOverlap(order.getSeatId(), order.getEndTime(), newEnd) > 0) {
            throw new BusinessException("顺延时段该座位已被其他用户预约，无法续时");
        }
        if (orderMapper.countUserOverlap(order.getUserId(), order.getEndTime(), newEnd) > 0) {
            throw new BusinessException("顺延时段您已有其他预约，无法续时");
        }
    }

    /**
     * 按当前套餐折算每小时单价（FR-UC-055）
     * <p>小时套餐 = 售价/时长；日卡 24h、周卡 168h、月卡 720h 折算。</p>
     */
    private BigDecimal unitPrice(Package pkg) {
        long totalHours;
        switch (pkg.getUnit() == null ? "hour" : pkg.getUnit()) {
            case "day":
                totalHours = 24L * Math.max(pkg.getDurationValue(), 1);
                break;
            case "week":
                totalHours = 168L * Math.max(pkg.getDurationValue(), 1);
                break;
            case "month":
                totalHours = 720L * Math.max(pkg.getDurationValue(), 1);
                break;
            default: // hour
                totalHours = Math.max(pkg.getDurationValue(), 1);
        }
        return pkg.getPrice()
                .divide(BigDecimal.valueOf(totalHours), 2, RoundingMode.HALF_UP);
    }
}
