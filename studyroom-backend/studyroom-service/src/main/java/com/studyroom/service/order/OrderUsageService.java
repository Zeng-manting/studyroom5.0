package com.studyroom.service.order;

import com.studyroom.entity.vo.order.OrderUsageVO;

/**
 * 订单使用中服务（FR-UC-031 使用中页 / FR-UC-033 暂停 / FR-UC-034 离场结算）
 *
 * @author studyroom
 */
public interface OrderUsageService {

    /**
     * 使用中/已完成订单的使用状态（FR-UC-031 使用中页数据源）
     * <p>含剩余时间、暂停状态与截止时间、累计暂停统计。</p>
     */
    OrderUsageVO getUsage(Long userId, Long orderId);

    /**
     * 中途临时离座：暂停计时（FR-UC-033）
     * <ul>
     *   <li>CAS 订单置暂停态（pause_start_time / pause_end_time=+30min / pause_count+1）</li>
     *   <li>Redis 记录 studyroom:pause:{orderId}（30 分钟 TTL）</li>
     *   <li>投递暂停超时自动结算延迟任务（FR-SY-002）</li>
     * </ul>
     */
    OrderUsageVO pause(Long userId, Long orderId);

    /**
     * 回座恢复使用（FR-UC-033）：CAS 清除暂停态、暂停记录 0→1、删除 Redis 计时器
     */
    OrderUsageVO resume(Long userId, Long orderId);

    /**
     * 离场结束使用（FR-UC-034）：
     * 实际时长 = 签到→实际结束 - 累计暂停分钟；按比例退还未使用差额至余额；
     * 座位转打扫中（供管理端保洁任务流转）。
     */
    OrderUsageVO end(Long userId, Long orderId);

    /**
     * 暂停超时自动结算（FR-UC-033 + FR-SY-002，由延迟队列触发）
     * <p>暂停记录 CAS 0→2（超时自动结算），命中后以 pause_start 为实际结束时间结算订单。</p>
     *
     * @return true 表示本次任务实际完成结算（幂等：已恢复/已结算返回 false）
     */
    boolean settlePauseTimeout(Long orderId, Long pauseId);
}
