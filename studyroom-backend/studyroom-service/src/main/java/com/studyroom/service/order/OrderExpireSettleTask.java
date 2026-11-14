package com.studyroom.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.entity.order.Orders;
import com.studyroom.mapper.order.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约到期自动结算定时任务（FR-UC-034 / FR-SY-002）
 * <p>每分钟扫描一次「使用中且已过预约结束时间」的订单，调用结算逻辑：
 * 订单转已完成、座位转打扫中，需管理端「打扫完成」后才重新开放预约。
 * 避免用户忘记点「结束使用」导致座位长期停留在使用中。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireSettleTask {

    private final OrderMapper orderMapper;
    private final OrderUsageService orderUsageService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void autoSettleExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Orders> orders = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getStatus, Orders.STATUS_IN_USE)
                .le(Orders::getEndTime, now)
                .orderByAsc(Orders::getEndTime)
                .last("LIMIT 50"));
        if (orders.isEmpty()) {
            return;
        }
        int settled = 0;
        for (Orders order : orders) {
            try {
                if (orderUsageService.settleExpired(order.getId())) {
                    settled++;
                }
            } catch (Exception e) {
                // 单笔失败不影响其余订单，下一轮继续尝试
                log.warn("[OrderExpireSettleTask] 到期自动结算失败: orderNo={}, reason={}",
                        order.getOrderNo(), e.getMessage());
            }
        }
        log.info("[OrderExpireSettleTask] 本轮到期结算完成: 扫描={}, 成功={}", orders.size(), settled);
    }
}
