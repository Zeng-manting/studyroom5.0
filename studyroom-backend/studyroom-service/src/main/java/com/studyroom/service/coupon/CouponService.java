package com.studyroom.service.coupon;

import com.studyroom.entity.vo.order.UsableCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务（FR-UC-024 优惠券抵扣）
 */
public interface CouponService {

    /**
     * 查询用户在某金额下可用的优惠券列表（FR-UC-024 订单确认页）
     *
     * @param userId      用户ID
     * @param orderAmount 订单金额（套券抵扣前的应付基数）
     * @return 可用券列表（含各自可抵扣金额）
     */
    List<UsableCouponVO> listUsableCoupons(Long userId, BigDecimal orderAmount);

    /**
     * 计算某张券在订单金额下的抵扣额（FR-UC-024 三种券）
     * <ul>
     *   <li>1新人券/2时长抵扣券：定额抵扣 或 折扣</li>
     *   <li>3满减券：满 threshold 减 discount</li>
     * </ul>
     *
     * @param claimId 领取记录ID
     * @param userId  用户ID（校验归属）
     * @param orderAmount 订单金额
     * @return 抵扣金额；不可用返回 BigDecimal.ZERO
     */
    BigDecimal calcDeductible(Long claimId, Long userId, BigDecimal orderAmount);

    /**
     * 下单绑定券（CAS 0→1，防止并发重复用券）
     *
     * @return 绑定成功后该券在订单中的抵扣金额；券不可用抛业务异常
     */
    BigDecimal bindToOrder(Long claimId, Long userId, Long orderId, BigDecimal orderAmount);

    /**
     * 订单取消/超时释放券（CAS 1→0）
     */
    void releaseFromOrder(Long claimId, Long orderId);

    /**
     * 支付成功补记 use_time（幂等）
     */
    void markPaid(Long claimId, Long orderId);
}
