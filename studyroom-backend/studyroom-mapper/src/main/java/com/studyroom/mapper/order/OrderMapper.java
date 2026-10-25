package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper（FR-UC-015~038）
 * <p>核心查询：座位时段重叠（FR-UC-018 锁座校验）、用户预约冲突（FR-UC-021）、批量占用座位（FR-UC-017 座位图）。</p>
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 统计座位在 [start, end) 时段内被占用的情况（FR-UC-018）
     * <p>占用语义订单状态：0待支付(15分钟锁) / 1已预约 / 2进行中。</p>
     * <p>重叠判定：existing.start &lt; reqEnd AND existing.end &gt; reqStart。</p>
     *
     * @return 占用订单数（&gt;0 表示时段冲突）
     */
    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE seat_id = #{seatId} AND deleted = 0 " +
            "AND status IN (0, 1, 2) " +
            "AND start_time < #{endTime} AND end_time > #{startTime}")
    int countSeatOverlap(@Param("seatId") Long seatId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户在时段内的有效预约数（FR-UC-021 预约冲突校验：同一用户同一时段不可重复预约）
     *
     * @return 冲突订单数
     */
    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE user_id = #{userId} AND deleted = 0 " +
            "AND status IN (0, 1, 2) " +
            "AND start_time < #{endTime} AND end_time > #{startTime}")
    int countUserOverlap(@Param("userId") Long userId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 批量查询门店内在 [start, end) 时段被订单占用的座位 ID（FR-UC-017 座位图"占用(灰)"计算）
     *
     * @return 占用座位 ID 列表
     */
    @Select("<script>" +
            "SELECT seat_id FROM orders " +
            "WHERE store_id = #{storeId} AND deleted = 0 " +
            "AND status IN (0, 1, 2) " +
            "AND start_time &lt; #{endTime} AND end_time &gt; #{startTime} " +
            "</script>")
    List<Long> selectOccupiedSeatIds(@Param("storeId") Long storeId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询门店内"此刻正在使用"（已签到、进行中）的座位 ID（FR-UC-017 "使用中(蓝)"计算）
     */
    @Select("SELECT seat_id FROM orders " +
            "WHERE store_id = #{storeId} AND deleted = 0 " +
            "AND status = 2 AND sign_time IS NOT NULL " +
            "AND start_time <= NOW() AND end_time > NOW()")
    List<Long> selectInUseSeatIds(@Param("storeId") Long storeId);

    /**
     * 用户订单累计统计（FR-UC-049 学习统计：累计时长/天数/消费/单数）
     * <p>口径：已支付订单（status IN 2,3,6,7），时长取实际使用分钟（未结算订单回退预约时长估算）。</p>
     *
     * @return 单行 Map：totalMinutes / totalDays / totalConsume / totalOrders
     */
    @Select("SELECT COALESCE(SUM(COALESCE(actual_duration_minutes, " +
            "            TIMESTAMPDIFF(MINUTE, start_time, end_time))), 0) AS totalMinutes, " +
            "COUNT(DISTINCT DATE(start_time)) AS totalDays, " +
            "COALESCE(SUM(pay_amount), 0) AS totalConsume, " +
            "COUNT(*) AS totalOrders " +
            "FROM orders " +
            "WHERE user_id = #{userId} AND deleted = 0 AND status IN (2, 3, 6, 7)")
    Map<String, Object> selectUserOrderStats(@Param("userId") Long userId);

    /**
     * 用户 24 小时时段分布订单数（FR-UC-049 柱状图，按签到/开始小时聚合）
     *
     * @return 每行 Map：hour(0~23) / cnt
     */
    @Select("SELECT HOUR(COALESCE(sign_time, start_time)) AS hour, COUNT(*) AS cnt " +
            "FROM orders " +
            "WHERE user_id = #{userId} AND deleted = 0 AND status IN (2, 3, 6, 7) " +
            "GROUP BY HOUR(COALESCE(sign_time, start_time))")
    List<Map<String, Object>> selectHourDistribution(@Param("userId") Long userId);

    /**
     * 用户近 N 天每日学习分钟数（FR-UC-049 打卡日历热力图）
     * <p>按实际使用日期聚合；未结算订单回退预约时长估算。</p>
     *
     * @return 每行 Map：day(yyyy-MM-dd) / minutes
     */
    @Select("SELECT DATE_FORMAT(COALESCE(actual_end_time, end_time), '%Y-%m-%d') AS day, " +
            "SUM(COALESCE(actual_duration_minutes, " +
            "    TIMESTAMPDIFF(MINUTE, start_time, end_time))) AS minutes " +
            "FROM orders " +
            "WHERE user_id = #{userId} AND deleted = 0 AND status IN (2, 3, 6, 7) " +
            "AND COALESCE(actual_end_time, end_time) >= #{since} " +
            "GROUP BY DATE_FORMAT(COALESCE(actual_end_time, end_time), '%Y-%m-%d')")
    List<Map<String, Object>> selectDailyHeatmap(@Param("userId") Long userId,
                                                 @Param("since") LocalDateTime since);
}
