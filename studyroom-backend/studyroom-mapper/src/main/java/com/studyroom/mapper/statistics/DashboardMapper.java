package com.studyroom.mapper.statistics;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计 Mapper —— FR-AD-002 / FR-AD-003
 *
 * <p>单店部署场景，统计范围覆盖全表（无 store_id 过滤）；
 * 单店规模 ≤ 200 座位、聚合查询走 create_time/pay_time 索引，性能可控。</p>
 *
 * <h3>口径汇总</h3>
 * <ul>
 *   <li>预约量：create_time 落入窗口且 status != 4（已取消不计）</li>
 *   <li>营收：pay_time 非空且落入窗口的 pay_amount 合计（余额+微信实付，含后续退款）</li>
 *   <li>活跃用户：last_login_time 落入窗口的正常状态用户</li>
 *   <li>座位状态：seat 表实时快照，按 status 分组</li>
 *   <li>座位利用率：已消费订单（status IN 2,3,6,7）按座位聚合，
 *       时长优先 actual_duration_minutes，回退预约时长</li>
 * </ul>
 *
 * @author studyroom
 */
@Mapper
public interface DashboardMapper {

    /* ==================== FR-AD-002 核心指标 ==================== */

    /**
     * 窗口内预约量（创建时间口径，排除已取消）
     *
     * @param start 窗口起点（含）
     * @param end   窗口终点（不含）
     */
    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE deleted = 0 AND status != 4 " +
            "AND create_time &gt;= #{start} AND create_time &lt; #{end}")
    long countOrdersCreatedBetween(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    /**
     * 窗口内营收（支付时间口径的实付合计）
     */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM orders " +
            "WHERE deleted = 0 AND pay_time IS NOT NULL " +
            "AND pay_time &gt;= #{start} AND pay_time &lt; #{end}")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * 窗口内活跃用户数（当日有登录行为的正常状态用户）
     */
    @Select("SELECT COUNT(*) FROM user " +
            "WHERE deleted = 0 AND status = 1 " +
            "AND last_login_time &gt;= #{start} AND last_login_time &lt; #{end}")
    long countActiveUsersBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * 座位状态分布（实时快照）—— FR-AD-003 座位状态总览网格
     *
     * @return 每行 Map：status(0~4) / cnt
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM seat " +
            "WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countSeatByStatus();

    /* ==================== FR-AD-003 趋势与排行 ==================== */

    /**
     * 每日预约量（按创建日聚合，排除已取消）
     *
     * @return 每行 Map：date(yyyy-MM-dd) / cnt
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS cnt " +
            "FROM orders " +
            "WHERE deleted = 0 AND status != 4 AND create_time &gt;= #{since} " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> countOrdersDaily(@Param("since") LocalDateTime since);

    /**
     * 每日营收（按支付日聚合）
     *
     * @return 每行 Map：date(yyyy-MM-dd) / amount
     */
    @Select("SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') AS date, COALESCE(SUM(pay_amount), 0) AS amount " +
            "FROM orders " +
            "WHERE deleted = 0 AND pay_time IS NOT NULL AND pay_time &gt;= #{since} " +
            "GROUP BY DATE_FORMAT(pay_time, '%Y-%m-%d')")
    List<Map<String, Object>> sumRevenueDaily(@Param("since") LocalDateTime since);

    /**
     * 座位利用率排行 —— FR-AD-003 Top10
     * <p>已消费订单按座位聚合，使用时长 = SUM(实际使用分钟，未结算回退预约时长)。</p>
     *
     * @return 每行 Map：seatId / seatNo / typeName / orderCount / usedMinutes / revenue
     */
    @Select("SELECT o.seat_id          AS seatId, " +
            "       s.seat_no          AS seatNo, " +
            "       st.name            AS typeName, " +
            "       COUNT(*)           AS orderCount, " +
            "       COALESCE(SUM(COALESCE(o.actual_duration_minutes, " +
            "           TIMESTAMPDIFF(MINUTE, o.start_time, o.end_time))), 0) AS usedMinutes, " +
            "       COALESCE(SUM(o.pay_amount), 0) AS revenue " +
            "FROM orders o " +
            "JOIN seat s ON o.seat_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN seat_type st ON s.seat_type_id = st.id " +
            "WHERE o.deleted = 0 AND o.status IN (2, 3, 6, 7) " +
            "AND o.start_time &gt;= #{since} " +
            "GROUP BY o.seat_id, s.seat_no, st.name " +
            "ORDER BY usedMinutes DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectSeatTop(@Param("since") LocalDateTime since,
                                            @Param("limit") int limit);
}
