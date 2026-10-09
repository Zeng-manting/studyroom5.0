package com.studyroom.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户 Mapper —— FR-UC-001/002/003/004
 *
 * <p>基础 CRUD 由 MyBatis-Plus BaseMapper 提供；
 * openid 查询虽可走 LambdaQuery，但登录为最高频路径，
 * 单独提供方法便于后续按需增加缓存或 SQL 优化。</p>
 *
 * @author studyroom
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按 openid 查询有效用户（登录主路径 —— FR-UC-001）
     *
     * @param openid 微信 openid
     * @return 用户；不存在返回 null
     */
    @Select("SELECT * FROM user WHERE openid = #{openid} AND deleted = 0 LIMIT 1")
    User selectByOpenid(@Param("openid") String openid);

    /**
     * 余额原子扣减（FR-UC-023 余额支付）
     * <p>WHERE 条件保证余额充足才扣款，防止并发超扣；返回 0 表示余额不足。</p>
     *
     * @return 影响行数（1=扣款成功 0=余额不足）
     */
    @Update("UPDATE user SET balance = balance - #{amount} " +
            "WHERE id = #{userId} AND balance >= #{amount} AND deleted = 0")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 余额回充（FR-UC-027 阶梯退款退回余额）
     */
    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{userId} AND deleted = 0")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 爽约信用扣减（FR-UC-053）
     * <p>信用分原子扣减（下限 0），同步冗余 30 天内爽约次数；返回 0 表示用户不存在。</p>
     *
     * @param delta          单次爽约扣减分值（正数传入，SQL 内做减法）
     * @param noShowCount30d 周期内爽约次数（service 层实时统计后写入冗余字段）
     */
    @Update("UPDATE user SET credit_score = GREATEST(0, credit_score - #{delta}), " +
            "no_show_count_30d = #{noShowCount30d} WHERE id = #{userId} AND deleted = 0")
    int deductCreditOnNoShow(@Param("userId") Long userId,
                             @Param("delta") int delta,
                             @Param("noShowCount30d") int noShowCount30d);

    /**
     * 置信用限制（FR-UC-054：30 天内爽约达上限）
     * <p>CAS：仅当前为正常状态(0)时置为限制中(1)，防止重复限制。</p>
     *
     * @return 影响行数（1=本次成功置限制 0=已处于限制中）
     */
    @Update("UPDATE user SET credit_status = 1, credit_limit_end = #{limitEnd} " +
            "WHERE id = #{userId} AND credit_status = 0 AND deleted = 0")
    int casStartCreditLimit(@Param("userId") Long userId,
                            @Param("limitEnd") java.time.LocalDateTime limitEnd);

    /**
     * 解除信用限制（FR-UC-054：到期自动恢复 / 管理端手动解除共用）
     * <p>CAS：仅限制中(1)可解除；同时按剩余爽约次数冗余字段重置展示。</p>
     *
     * @param noShowCount30d 解除后的周期内爽约次数（自动恢复=实时统计，手动解除=0）
     * @return 影响行数（1=解除成功 0=非限制中，幂等）
     */
    @Update("UPDATE user SET credit_status = 0, credit_limit_end = NULL, " +
            "no_show_count_30d = #{noShowCount30d} " +
            "WHERE id = #{userId} AND credit_status = 1 AND deleted = 0")
    int casRemoveCreditLimit(@Param("userId") Long userId,
                             @Param("noShowCount30d") int noShowCount30d);

    /**
     * 申诉通过恢复信用分（FR-AD-011 标记签到成功 → 撤销爽约）
     * <p>信用分原子回加（上限 100，与初始分一致），冗余 30 天爽约计数同步 -1（下限 0）。
     * 幂等由 CreditService.restoreNoShowCredit 的流水判重保证。</p>
     *
     * @param delta 单次回加分值（正数传入，SQL 内做加法）
     */
    @Update("UPDATE user SET credit_score = LEAST(100, credit_score + #{delta}), " +
            "no_show_count_30d = GREATEST(0, no_show_count_30d - 1) " +
            "WHERE id = #{userId} AND deleted = 0")
    int addCreditOnRestore(@Param("userId") Long userId, @Param("delta") int delta);
}
