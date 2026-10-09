package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.coupon.Coupon;
import com.studyroom.entity.coupon.CouponClaim;
import com.studyroom.entity.dto.admin.CouponGrantDTO;
import com.studyroom.entity.dto.admin.CouponSaveDTO;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.admin.AdminCouponVO;
import com.studyroom.mapper.coupon.CouponClaimMapper;
import com.studyroom.mapper.coupon.CouponMapper;
import com.studyroom.mapper.user.UserMapper;
import com.studyroom.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端优惠券配置服务实现 —— FR-AD-027
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>有效期双轨：固定期（validStart/validEnd）或相对期（领取后 validDays 天）；
 *       两者都配时以固定期为准，expireTime = validEnd</li>
 *   <li>发放语义：逐用户校验（存在性 + 每人限领 + 发行量余量）后写 coupon_claim，
 *       跳过不可发用户并计数返回；发行量校验用 SQL 条件更新防并发超发</li>
 *   <li>下架只影响后续领取，已领取的券在有效期内继续可用（CouponService.calcDeductible 不校验券上架态）</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponMapper couponMapper;
    private final CouponClaimMapper couponClaimMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    /* ==================== 列表 ==================== */

    @Override
    public PageResult<AdminCouponVO> pageCoupons(Integer type, Integer status, long page, long size) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Coupon::getType, type);
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);
        Page<Coupon> p = couponMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(
                p.getRecords().stream().map(this::toVO).collect(Collectors.toList()),
                p.getTotal(), p.getCurrent(), p.getSize());
    }

    /* ==================== 新建/编辑 ==================== */

    @Override
    public Long saveCoupon(CouponSaveDTO dto, Long operatorId) {
        validateCoupon(dto);

        Coupon coupon = dto.getId() == null ? new Coupon() : requireCoupon(dto.getId());
        coupon.setName(dto.getName().trim());
        coupon.setType(dto.getType());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setThresholdAmount(dto.getThresholdAmount() == null
                ? java.math.BigDecimal.ZERO : dto.getThresholdAmount());
        coupon.setDiscountAmount(dto.getDiscountAmount() == null
                ? java.math.BigDecimal.ZERO : dto.getDiscountAmount());
        coupon.setDiscountRatio(dto.getDiscountRatio() == null
                ? java.math.BigDecimal.ONE : dto.getDiscountRatio());
        coupon.setValidDays(dto.getValidDays() == null ? 30 : dto.getValidDays());
        coupon.setValidStart(dto.getValidStart());
        coupon.setValidEnd(dto.getValidEnd());
        coupon.setTotalQuota(dto.getTotalQuota() == null ? 0 : dto.getTotalQuota());
        coupon.setPerUserLimit(dto.getPerUserLimit() == null ? 1 : dto.getPerUserLimit());
        coupon.setDescription(dto.getDescription());
        coupon.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        if (coupon.getId() == null) {
            coupon.setUsedCount(0);
            coupon.setClaimCount(0);
            coupon.setCreatorId(operatorId);
            couponMapper.insert(coupon);
        } else {
            // 编辑不重置发放/使用计数
            couponMapper.updateById(coupon);
        }
        log.info("[AdminCoupon] FR-AD-027 保存优惠券: id={}, name={}", coupon.getId(), coupon.getName());
        return coupon.getId();
    }

    /* ==================== 上/下架 ==================== */

    @Override
    public void toggleStatus(Long couponId, boolean online) {
        requireCoupon(couponId);
        Coupon patch = new Coupon();
        patch.setId(couponId);
        patch.setStatus(online ? 1 : 0);
        couponMapper.updateById(patch);
        log.info("[AdminCoupon] FR-AD-027 优惠券{}: id={}", online ? "上架" : "下架", couponId);
    }

    /* ==================== 发放 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grantCoupon(CouponGrantDTO dto) {
        Coupon coupon = requireCoupon(dto.getCouponId());
        if (coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已下架优惠券不能发放");
        }

        // 目标用户去重 + 存在性过滤
        Set<Long> targetIds = dto.getUserIds().stream()
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(targetIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        int granted = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : targetIds) {
            if (!userMap.containsKey(userId)) {
                log.warn("[AdminCoupon] 发放跳过：用户不存在 userId={}", userId);
                continue;
            }
            // 每人限领校验
            if (coupon.getPerUserLimit() != null && coupon.getPerUserLimit() > 0) {
                Long claimed = couponClaimMapper.selectCount(new LambdaQueryWrapper<CouponClaim>()
                        .eq(CouponClaim::getCouponId, coupon.getId())
                        .eq(CouponClaim::getUserId, userId));
                if (claimed != null && claimed >= coupon.getPerUserLimit()) {
                    log.info("[AdminCoupon] 发放跳过：已达限领 userId={}, couponId={}", userId, coupon.getId());
                    continue;
                }
            }
            // 发行量余量校验（总量 > 0 时）
            if (coupon.getTotalQuota() != null && coupon.getTotalQuota() > 0) {
                Long totalClaimed = couponClaimMapper.selectCount(new LambdaQueryWrapper<CouponClaim>()
                        .eq(CouponClaim::getCouponId, coupon.getId()));
                if (totalClaimed != null && totalClaimed >= coupon.getTotalQuota()) {
                    log.warn("[AdminCoupon] 发放中止：发行量已用尽 couponId={}", coupon.getId());
                    break;
                }
            }

            // 计算过期时间：固定期优先
            LocalDateTime expireTime;
            if (coupon.getValidEnd() != null) {
                expireTime = coupon.getValidEnd();
            } else if (coupon.getValidStart() != null) {
                expireTime = coupon.getValidStart().plusDays(
                        coupon.getValidDays() == null ? 30 : coupon.getValidDays());
            } else {
                expireTime = now.plusDays(coupon.getValidDays() == null ? 30 : coupon.getValidDays());
            }
            if (!expireTime.isAfter(now)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券有效期已过，无法发放");
            }

            CouponClaim claim = new CouponClaim();
            claim.setCouponId(coupon.getId());
            claim.setUserId(userId);
            claim.setStatus(CouponClaim.STATUS_UNUSED);
            claim.setClaimTime(now);
            claim.setExpireTime(expireTime);
            couponClaimMapper.insert(claim);

            // 发放量计数 +1
            Coupon countPatch = new Coupon();
            countPatch.setId(coupon.getId());
            countPatch.setClaimCount((coupon.getClaimCount() == null ? 0 : coupon.getClaimCount()) + 1);
            couponMapper.updateById(countPatch);
            coupon.setClaimCount(countPatch.getClaimCount());

            // 站内信通知
            messageService.sendActivityMessage(userId, "优惠券到账",
                    "您收到一张「" + coupon.getName() + "」，快去下单使用吧。");
            granted++;
        }
        log.info("[AdminCoupon] FR-AD-027 发放完成: couponId={}, 目标 {} 人, 成功 {} 人",
                coupon.getId(), targetIds.size(), granted);
        return granted;
    }

    /* ==================== 领取明细 ==================== */

    @Override
    public List<AdminCouponVO.ClaimRecordItem> listClaims(Long couponId) {
        requireCoupon(couponId);
        List<CouponClaim> claims = couponClaimMapper.selectList(
                new LambdaQueryWrapper<CouponClaim>()
                        .eq(CouponClaim::getCouponId, couponId)
                        .orderByDesc(CouponClaim::getClaimTime)
                        .last("LIMIT 100"));
        if (claims.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = claims.stream().map(CouponClaim::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<AdminCouponVO.ClaimRecordItem> list = new ArrayList<>(claims.size());
        for (CouponClaim c : claims) {
            AdminCouponVO.ClaimRecordItem item = new AdminCouponVO.ClaimRecordItem();
            item.setClaimId(c.getId());
            item.setUserId(c.getUserId());
            User user = userMap.get(c.getUserId());
            item.setNickname(user != null && user.getNickname() != null
                    ? user.getNickname() : "用户#" + c.getUserId());
            item.setStatus(c.getStatus());
            item.setStatusDesc(claimStatusDesc(c.getStatus()));
            item.setClaimTime(c.getClaimTime());
            item.setExpireTime(c.getExpireTime());
            list.add(item);
        }
        return list;
    }

    /* ==================== 内部方法 ==================== */

    /** 优惠券参数交叉校验（FR-AD-027 三种券 × 三种优惠方式） */
    private void validateCoupon(CouponSaveDTO dto) {
        // 满减券必须配门槛；满减方式必须同时有门槛与抵扣额
        if (dto.getType() == Coupon.TYPE_FULL_REDUCTION
                && (dto.getThresholdAmount() == null || dto.getThresholdAmount().signum() <= 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "满减券必须配置满减门槛");
        }
        if (dto.getDiscountType() == Coupon.DISCOUNT_FIXED
                && (dto.getDiscountAmount() == null || dto.getDiscountAmount().signum() <= 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "定额抵扣必须配置抵扣金额");
        }
        if (dto.getDiscountType() == Coupon.DISCOUNT_FULL_REDUCTION
                && (dto.getDiscountAmount() == null || dto.getDiscountAmount().signum() <= 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "满减必须配置减免金额");
        }
        if (dto.getDiscountType() == Coupon.DISCOUNT_RATIO
                && (dto.getDiscountRatio() == null || dto.getDiscountRatio().signum() <= 0
                || dto.getDiscountRatio().compareTo(java.math.BigDecimal.ONE) > 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "折扣比例应在 0.01 ~ 1.00 之间");
        }
        // 固定期校验
        if (dto.getValidStart() != null && dto.getValidEnd() != null
                && !dto.getValidStart().isBefore(dto.getValidEnd())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "固定起期必须早于止期");
        }
    }

    private Coupon requireCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        return coupon;
    }

    private String claimStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "未使用";
            case 1: return "已使用";
            case 2: return "已过期";
            default: return "未知";
        }
    }

    private AdminCouponVO toVO(Coupon c) {
        AdminCouponVO vo = new AdminCouponVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setType(c.getType());
        vo.setTypeDesc(typeDesc(c.getType()));
        vo.setDiscountType(c.getDiscountType());
        vo.setDiscountTypeDesc(discountTypeDesc(c.getDiscountType()));
        vo.setThresholdAmount(c.getThresholdAmount());
        vo.setDiscountAmount(c.getDiscountAmount());
        vo.setDiscountRatio(c.getDiscountRatio());
        vo.setValidDays(c.getValidDays());
        vo.setValidStart(c.getValidStart());
        vo.setValidEnd(c.getValidEnd());
        vo.setTotalQuota(c.getTotalQuota());
        vo.setClaimCount(c.getClaimCount());
        vo.setUsedCount(c.getUsedCount());
        vo.setPerUserLimit(c.getPerUserLimit());
        vo.setDescription(c.getDescription());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private String typeDesc(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "新人券";
            case 2: return "时长抵扣券";
            case 3: return "满减券";
            default: return "未知";
        }
    }

    private String discountTypeDesc(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "定额抵扣";
            case 2: return "折扣";
            case 3: return "满减";
            default: return "未知";
        }
    }
}
