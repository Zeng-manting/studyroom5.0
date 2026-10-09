package com.studyroom.mapper.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管理端用户统计聚合 Mapper —— FR-AD-014
 *
 * <p>用户列表需要批量聚合"累计订单数 / 累计学习时长"，
 * 为避免污染领域 Mapper 与 N+1 查询，独立提供 GROUP BY 聚合。</p>
 *
 * @author studyroom
 */
@Mapper
public interface AdminUserStatsMapper {

    /**
     * 批量统计累计订单数（已支付口径：pay_time 非空）
     *
     * @return 元素含 user_id / cnt 两列
     */
    @Select("<script>" +
            "SELECT user_id, COUNT(*) AS cnt FROM orders " +
            "WHERE pay_time IS NOT NULL AND user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY user_id" +
            "</script>")
    List<Map<String, Object>> countPaidOrders(@Param("userIds") List<Long> userIds);

    /**
     * 批量统计累计学习时长（分钟，实际使用时长合计）
     *
     * @return 元素含 user_id / minutes 两列
     */
    @Select("<script>" +
            "SELECT user_id, SUM(COALESCE(actual_duration_minutes, 0)) AS minutes FROM orders " +
            "WHERE pay_time IS NOT NULL AND user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY user_id" +
            "</script>")
    List<Map<String, Object>> sumStudyMinutes(@Param("userIds") List<Long> userIds);
}
