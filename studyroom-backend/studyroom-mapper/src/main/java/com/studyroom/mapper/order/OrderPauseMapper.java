package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.OrderPause;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 订单暂停记录 Mapper（FR-UC-033）
 */
@Mapper
public interface OrderPauseMapper extends BaseMapper<OrderPause> {

    /**
     * 统计订单累计暂停分钟数（FR-UC-034 实际时长 = 签到→结束 - 累计暂停）
     * <ul>
     *   <li>已恢复/已结算的记录按 pause_start → pause_end 计</li>
     *   <li>仍在暂停中的记录按 pause_start → #{until} 计（实时口径）</li>
     * </ul>
     */
    @Select("SELECT IFNULL(SUM(TIMESTAMPDIFF(MINUTE, pause_start, " +
            "CASE WHEN pause_end IS NULL THEN #{until} ELSE LEAST(pause_end, #{until}) END)), 0) " +
            "FROM order_pause WHERE order_id = #{orderId} AND status IN (0, 1, 2) " +
            "AND pause_start < #{until}")
    int sumPausedMinutes(@Param("orderId") Long orderId, @Param("until") LocalDateTime until);
}
