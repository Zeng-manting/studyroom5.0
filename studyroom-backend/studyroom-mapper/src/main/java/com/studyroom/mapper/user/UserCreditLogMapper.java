package com.studyroom.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.user.UserCreditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 信用变更流水 Mapper（FR-UC-053/054）
 *
 * @author studyroom
 */
@Mapper
public interface UserCreditLogMapper extends BaseMapper<UserCreditLog> {

    /**
     * 统计用户在滚动周期内的爽约次数（FR-UC-053/054）
     *
     * @param userId 用户ID
     * @param since  周期起点（now - 30 天）
     * @return 周期内爽约次数
     */
    @Select("SELECT COUNT(*) FROM user_credit_log " +
            "WHERE user_id = #{userId} AND type = 1 AND create_time >= #{since}")
    int countNoShowSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 统计订单的爽约流水数（FR-AD-011 撤销爽约前判重：有爽约流水才允许恢复）
     */
    @Select("SELECT COUNT(*) FROM user_credit_log WHERE order_id = #{orderId} AND type = 1")
    int countNoShowByOrderId(@Param("orderId") Long orderId);

    /**
     * 统计订单的申诉恢复流水数（FR-AD-011 幂等：已恢复过则跳过，防重复回加信用分）
     */
    @Select("SELECT COUNT(*) FROM user_credit_log WHERE order_id = #{orderId} AND type = 2 " +
            "AND reason LIKE '申诉通过%'")
    int countAppealRestoreByOrderId(@Param("orderId") Long orderId);
}
