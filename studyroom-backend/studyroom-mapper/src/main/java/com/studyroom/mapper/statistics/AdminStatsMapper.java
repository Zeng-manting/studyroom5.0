package com.studyroom.mapper.statistics;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计 Mapper —— FR-AD-019 ~ FR-AD-024
 *
 * <h3>口径说明（与 DashboardMapper 一致）</h3>
 * <ul>
 *   <li>预约量 = create_time 落在区间且未取消（status != 4）的订单数</li>
 *   <li>营收 = pay_time 落在区间的实付合计（pay_amount）</li>
 *   <li>退款 = order_refund.create_time 落在区间的退款金额合计</li>
 *   <li>活跃用户 = 当日有非取消订单的独立 user_id 数</li>
 * </ul>
 *
 * @author studyroom
 */
@Mapper
public interface AdminStatsMapper {

    /* ==================== FR-AD-019 预约统计 ==================== */

    /**
     * 预约时段分布：按预约开始小时聚合（0-23 点），FR-AD-019 热力图
     */
    @Select("SELECT HOUR(start_time) AS hour, COUNT(*) AS cnt FROM orders " +
            "WHERE status != 4 AND start_time >= #{begin} AND start_time < #{end} " +
            "GROUP BY HOUR(start_time)")
    List<Map<String, Object>> bookingHourDistribution(@Param("begin") LocalDateTime begin,
                                                      @Param("end") LocalDateTime end);

    /**
     * 套餐占比：按套餐聚合订单数与营收，FR-AD-019 饼图
     */
    @Select("SELECT p.name AS name, COUNT(o.id) AS cnt, IFNULL(SUM(o.pay_amount), 0) AS amount " +
            "FROM orders o LEFT JOIN package p ON o.package_id = p.id " +
            "WHERE o.status NOT IN (0, 4) AND o.create_time >= #{begin} AND o.create_time < #{end} " +
            "GROUP BY o.package_id, p.name")
    List<Map<String, Object>> packageShare(@Param("begin") LocalDateTime begin,
                                           @Param("end") LocalDateTime end);

    /* ==================== FR-AD-020 座位统计 ==================== */

    /**
     * 每日使用中时长（分钟）：actual_duration_minutes 优先，未结算回退预约时长，
     * 上座率 = 该时长 / (可用座位数 × 1440 分钟)，Service 层计算
     */
    @Select("SELECT DATE(create_time) AS date, " +
            "SUM(IFNULL(actual_duration_minutes, TIMESTAMPDIFF(MINUTE, start_time, end_time))) AS minutes " +
            "FROM orders WHERE status IN (2, 3, 6, 7) " +
            "AND create_time >= #{begin} AND create_time < #{end} " +
            "GROUP BY DATE(create_time)")
    List<Map<String, Object>> dailyOccupiedMinutes(@Param("begin") LocalDateTime begin,
                                                   @Param("end") LocalDateTime end);

    /**
     * 座位类型占用分布：按座位类型聚合已消费订单数，FR-AD-020
     */
    @Select("SELECT st.name AS name, COUNT(o.id) AS cnt FROM orders o " +
            "JOIN seat s ON o.seat_id = s.id JOIN seat_type st ON s.seat_type_id = st.id " +
            "WHERE o.status IN (2, 3, 6, 7) AND o.create_time >= #{begin} AND o.create_time < #{end} " +
            "GROUP BY st.id, st.name")
    List<Map<String, Object>> seatTypeDistribution(@Param("begin") LocalDateTime begin,
                                                   @Param("end") LocalDateTime end);

    /* ==================== FR-AD-021 营收统计 ==================== */

    /**
     * 收入构成：按套餐聚合营收（支付口径），FR-AD-021
     */
    @Select("SELECT IFNULL(p.name, '未关联套餐') AS name, COUNT(o.id) AS cnt, " +
            "IFNULL(SUM(o.pay_amount), 0) AS amount " +
            "FROM orders o LEFT JOIN package p ON o.package_id = p.id " +
            "WHERE o.pay_time IS NOT NULL AND o.pay_time >= #{begin} AND o.pay_time < #{end} " +
            "GROUP BY o.package_id, p.name")
    List<Map<String, Object>> revenueComposition(@Param("begin") LocalDateTime begin,
                                                 @Param("end") LocalDateTime end);

    /**
     * 每日退款金额，FR-AD-021 退款趋势
     */
    @Select("SELECT DATE(create_time) AS date, SUM(amount) AS amount, COUNT(*) AS cnt " +
            "FROM order_refund WHERE create_time >= #{begin} AND create_time < #{end} " +
            "GROUP BY DATE(create_time)")
    List<Map<String, Object>> dailyRefund(@Param("begin") LocalDateTime begin,
                                          @Param("end") LocalDateTime end);

    /**
     * 每日营收与订单数，FR-AD-021 客单价分析
     */
    @Select("SELECT DATE(pay_time) AS date, SUM(pay_amount) AS amount, COUNT(*) AS cnt " +
            "FROM orders WHERE pay_time IS NOT NULL AND pay_time >= #{begin} AND pay_time < #{end} " +
            "GROUP BY DATE(pay_time)")
    List<Map<String, Object>> dailyRevenue(@Param("begin") LocalDateTime begin,
                                           @Param("end") LocalDateTime end);

    /* ==================== FR-AD-022 用户活跃度 ==================== */

    /**
     * 每日活跃用户（DAU）：当日有非取消订单的独立用户数
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(DISTINCT user_id) AS cnt " +
            "FROM orders WHERE status != 4 AND create_time >= #{begin} AND create_time < #{end} " +
            "GROUP BY DATE(create_time)")
    List<Map<String, Object>> dailyActiveUsers(@Param("begin") LocalDateTime begin,
                                               @Param("end") LocalDateTime end);

    /**
     * 每日新增用户
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS cnt " +
            "FROM user WHERE create_time >= #{begin} AND create_time < #{end} " +
            "GROUP BY DATE(create_time)")
    List<Map<String, Object>> dailyNewUsers(@Param("begin") LocalDateTime begin,
                                            @Param("end") LocalDateTime end);

    /**
     * 学习时长分布直方图：按订单实际/预约时长分桶（Service 层分桶），FR-AD-022
     */
    @Select("SELECT IFNULL(actual_duration_minutes, " +
            "TIMESTAMPDIFF(MINUTE, start_time, end_time)) AS minutes " +
            "FROM orders WHERE status IN (2, 3) " +
            "AND create_time >= #{begin} AND create_time < #{end} " +
            "HAVING minutes IS NOT NULL")
    List<Map<String, Object>> orderDurations(@Param("begin") LocalDateTime begin,
                                             @Param("end") LocalDateTime end);

    /**
     * 次日留存：注册次日仍有订单的用户数 / 当日新增用户数，FR-AD-022
     * <p>按注册日分组，join 注册后 48h 内的首单（简化口径）。</p>
     */
    @Select("SELECT DATE(u.create_time) AS date, COUNT(DISTINCT u.id) AS new_cnt, " +
            "COUNT(DISTINCT CASE WHEN o.id IS NOT NULL THEN u.id END) AS retained_cnt " +
            "FROM user u " +
            "LEFT JOIN orders o ON o.user_id = u.id AND o.status != 4 " +
            "AND o.create_time >= u.create_time + INTERVAL 1 DAY " +
            "AND o.create_time < u.create_time + INTERVAL 8 DAY " +
            "WHERE u.create_time >= #{begin} AND u.create_time < #{end} " +
            "GROUP BY DATE(u.create_time)")
    List<Map<String, Object>> retentionCohorts(@Param("begin") LocalDateTime begin,
                                               @Param("end") LocalDateTime end);

    /* ==================== FR-AD-024 财务总览 ==================== */

    /**
     * 区间营收合计（支付口径）
     */
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM orders " +
            "WHERE pay_time IS NOT NULL AND pay_time >= #{begin} AND pay_time < #{end}")
    BigDecimal sumRevenue(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /**
     * 区间支付订单数
     */
    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE pay_time IS NOT NULL AND pay_time >= #{begin} AND pay_time < #{end}")
    Long countPaidOrders(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /**
     * 区间退款合计
     */
    @Select("SELECT IFNULL(SUM(amount), 0) FROM order_refund " +
            "WHERE create_time >= #{begin} AND create_time < #{end}")
    BigDecimal sumRefund(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
