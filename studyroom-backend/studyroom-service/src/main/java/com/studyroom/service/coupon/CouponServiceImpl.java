package com.studyroom.service.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.coupon.Coupon;
import com.studyroom.entity.coupon.CouponClaim;
import com.studyroom.entity.vo.order.UsableCouponVO;
import com.studyroom.mapper.coupon.CouponClaimMapper;
import com.studyroom.mapper.coupon.CouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 优惠券服务实现（FR-UC-024）
 * <p>三种券：1新人券 2时长抵扣券 3满减券；
 * 抵扣方式：1定额 2折扣 3满减。绑定/释放均走 CAS，保证并发安全。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final CouponClaimMapper claimMapper;

    @Override
    public List<UsableCouponVO> listUsableCoupons(Long userId, BigDecimal orderAmount) {
        // 1. 用户未使用且未过期的领取记录
        LambdaQueryWrapper<CouponClaim> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponClaim::getUserId, userId)
                .eq(CouponClaim::getStatus, CouponClaim.STATUS_UNUSED)
                .gt(CouponClaim::getExpireTime, LocalDateTime.now());
        List<CouponClaim> claims = claimMapper.selectList(wrapper);
        if (claims.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 逐张计算抵扣额，过滤不可用
        List<UsableCouponVO> result = new ArrayList<>();
        for (CouponClaim claim : claims) {
            Coupon coupon = couponMapper.selectById(claim.getCouponId());
            if (coupon == null || coupon.getStatus() != 1) {
                continue;
            }
            BigDecimal deductible = calcDeductible(coupon, orderAmount);
            if (deductible.compareTo(BigDecimal.ZERO) > 0) {
                result.add(toVO(claim, coupon, deductible));
            }
        }
        return result;
    }

    @Override
    public BigDecimal calcDeductible(Long claimId, Long userId, BigDecimal orderAmount) {
        CouponClaim claim = claimMapper.selectById(claimId);
        if (claim == null || !claim.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券不存在");
        }
        if (claim.getStatus() != CouponClaim.STATUS_UNUSED || claim.getExpireTime().isBefore(LocalDateTime.now())) {
            return BigDecimal.ZERO;
        }
        Coupon coupon = couponMapper.selectById(claim.getCouponId());
        if (coupon == null || coupon.getStatus() != 1) {
            return BigDecimal.ZERO;
        }
        return calcDeductible(coupon, orderAmount);
    }

    @Override
    public BigDecimal bindToOrder(Long claimId, Long userId, Long orderId, BigDecimal orderAmount) {
        // 1. 计算抵扣额（含归属/状态校验）
        BigDecimal deductible = calcDeductible(claimId, userId, orderAmount);
        if (deductible.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "优惠券不可用或未达到使用门槛");
        }
        // 2. CAS 绑定：并发下单只有一单能绑定成功（FR-SY-001 语义延伸）
        int rows = claimMapper.bindToOrder(claimId, userId, orderId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "优惠券已被其他订单使用");
        }
        return deductible;
    }

    @Override
    public void releaseFromOrder(Long claimId, Long orderId) {
        if (claimId == null) {
            return;
        }
        int rows = claimMapper.releaseFromOrder(claimId, orderId);
        if (rows == 0) {
            log.warn("[CouponService] 释放券失败（可能已释放）: claimId={}, orderId={}", claimId, orderId);
        }
    }

    @Override
    public void markPaid(Long claimId, Long orderId) {
        if (claimId == null) {
            return;
        }
        claimMapper.markUsedTime(claimId, orderId);
    }

    /**
     * 抵扣计算核心（FR-UC-024）：
     * <ul>
     *   <li>discountType=1 定额：min(discountAmount, orderAmount)</li>
     *   <li>discountType=2 折扣：orderAmount × (1 - discountRatio)，向下取 2 位</li>
     *   <li>discountType=3 满减：orderAmount ≥ threshold 时减 discountAmount，否则 0</li>
     * </ul>
     * 抵扣额不超过订单金额。
     */
    private BigDecimal calcDeductible(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal deductible;
        switch (coupon.getDiscountType()) {
            case Coupon.DISCOUNT_FIXED:
                deductible = nvl(coupon.getDiscountAmount());
                break;
            case Coupon.DISCOUNT_RATIO:
                BigDecimal ratio = nvl(coupon.getDiscountRatio());
                deductible = orderAmount.multiply(BigDecimal.ONE.subtract(ratio))
                        .setScale(2, RoundingMode.DOWN);
                break;
            case Coupon.DISCOUNT_FULL_REDUCTION:
                BigDecimal threshold = nvl(coupon.getThresholdAmount());
                deductible = orderAmount.compareTo(threshold) >= 0 ? nvl(coupon.getDiscountAmount()) : BigDecimal.ZERO;
                break;
            default:
                deductible = BigDecimal.ZERO;
        }
        // 封顶：不超订单金额
        if (deductible.compareTo(orderAmount) > 0) {
            deductible = orderAmount;
        }
        return deductible.max(BigDecimal.ZERO);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private UsableCouponVO toVO(CouponClaim claim, Coupon coupon, BigDecimal deductible) {
        UsableCouponVO vo = new UsableCouponVO();
        vo.setClaimId(claim.getId());
        vo.setCouponId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setDiscountDesc(discountDesc(coupon));
        vo.setDeductibleAmount(deductible);
        vo.setExpireTime(claim.getExpireTime());
        vo.setDescription(coupon.getDescription());
        return vo;
    }

    private String discountDesc(Coupon coupon) {
        switch (coupon.getDiscountType()) {
            case Coupon.DISCOUNT_FIXED:
                return "立减 ¥" + nvl(coupon.getDiscountAmount()).stripTrailingZeros().toPlainString();
            case Coupon.DISCOUNT_RATIO:
                return (BigDecimal.ONE.subtract(nvl(coupon.getDiscountRatio()))
                        .multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString()) + " 折";
            case Coupon.DISCOUNT_FULL_REDUCTION:
                return "满 ¥" + nvl(coupon.getThresholdAmount()).stripTrailingZeros().toPlainString()
                        + " 减 ¥" + nvl(coupon.getDiscountAmount()).stripTrailingZeros().toPlainString();
            default:
                return "";
        }
    }
}
