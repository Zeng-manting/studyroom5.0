package com.studyroom.service.order;

import com.studyroom.entity.dto.order.OrderExtendDTO;
import com.studyroom.entity.vo.order.OrderExtendPreviewVO;
import com.studyroom.entity.vo.order.OrderUsageVO;

/**
 * 进行中续时服务（FR-UC-055）
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>计费：按订单当前套餐单价折算（套餐价 ÷ 套餐总小时 = 元/小时 × 续时小时数）</li>
 *   <li>并发控制：Redisson 锁 order:extend:lock:{orderId} 保护"校验 + 支付 + 顺延"，
 *       与离场结算/暂停超时结算/重复续时并发安全；订单更新附 end_time CAS 双保险</li>
 *   <li>续占座位：顺延 [原结束, 新结束) 时段前校验座位无新占用与本人无新预约冲突</li>
 * </ul>
 *
 * @author studyroom
 */
public interface OrderExtendService {

    /**
     * 续时费用预览（FR-UC-055 使用中页续时弹层）
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @param hours   续时小时数（1~12）
     */
    OrderExtendPreviewVO preview(Long userId, Long orderId, Integer hours);

    /**
     * 执行续时（FR-UC-055）
     * <p>支付（余额/模拟微信）→ order_extension 流水 → 订单 end_time 顺延 + extend_count+1；
     * 支付后顺延结束时间并续占座位。</p>
     */
    OrderUsageVO extend(Long userId, Long orderId, OrderExtendDTO dto);
}
