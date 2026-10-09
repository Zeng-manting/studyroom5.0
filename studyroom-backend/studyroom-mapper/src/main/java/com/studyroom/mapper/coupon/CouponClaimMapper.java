package com.studyroom.mapper.coupon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.coupon.CouponClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户领券记录 Mapper（FR-UC-024）
 * <p>绑定/释放均使用 CAS 更新，防止并发重复用券或误释放他人券。</p>
 */
@Mapper
public interface CouponClaimMapper extends BaseMapper<CouponClaim> {

    /**
     * 下单绑定券：未使用 → 已使用（CAS，成功返回 1）
     */
    @Update("UPDATE coupon_claim SET status = 1, order_id = #{orderId} " +
            "WHERE id = #{claimId} AND user_id = #{userId} AND status = 0")
    int bindToOrder(@Param("claimId") Long claimId,
                    @Param("userId") Long userId,
                    @Param("orderId") Long orderId);

    /**
     * 订单取消释放券：已使用 → 未使用（CAS）
     */
    @Update("UPDATE coupon_claim SET status = 0, order_id = NULL, use_time = NULL " +
            "WHERE id = #{claimId} AND order_id = #{orderId} AND status = 1")
    int releaseFromOrder(@Param("claimId") Long claimId, @Param("orderId") Long orderId);

    /**
     * 支付成功时补记使用时间（幂等）
     */
    @Update("UPDATE coupon_claim SET use_time = NOW() " +
            "WHERE id = #{claimId} AND order_id = #{orderId} AND use_time IS NULL")
    int markUsedTime(@Param("claimId") Long claimId, @Param("orderId") Long orderId);
}
