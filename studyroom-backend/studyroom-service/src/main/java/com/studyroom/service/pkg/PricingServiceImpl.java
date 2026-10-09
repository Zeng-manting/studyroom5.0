package com.studyroom.service.pkg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.pkg.Package;
import com.studyroom.entity.pkg.PriceRule;
import com.studyroom.entity.vo.order.PackageVO;
import com.studyroom.mapper.pkg.PackageMapper;
import com.studyroom.mapper.pkg.PriceRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐计费服务实现（FR-UC-019 / FR-AD-025）
 * <p>计费模型：套餐固定价（package.price）+ 加价规则（price_rule）。
 * 规则匹配优先级：priority 降序取第一条命中（座位类型/套餐精确匹配优先于通配 NULL）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final PackageMapper packageMapper;
    private final PriceRuleMapper priceRuleMapper;

    @Override
    public List<PackageVO> listPackages(Integer type) {
        LambdaQueryWrapper<Package> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Package::getStatus, 1);
        if (type != null) {
            wrapper.eq(Package::getType, type);
        }
        wrapper.orderByAsc(Package::getSort).orderByAsc(Package::getId);
        return packageMapper.selectList(wrapper).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public BigDecimal calcOriginalPrice(Package pkg, Long seatTypeId, LocalDateTime startTime) {
        BigDecimal price = pkg.getPrice();
        PriceRule rule = matchRule(pkg.getId(), seatTypeId, startTime.toLocalTime());
        if (rule == null) {
            return price;
        }
        // 最终价 = 套餐价 × extra_ratio + extra_price（FR-AD-025）
        BigDecimal ratio = rule.getExtraRatio() == null ? BigDecimal.ONE : rule.getExtraRatio();
        BigDecimal extra = rule.getExtraPrice() == null ? BigDecimal.ZERO : rule.getExtraPrice();
        return price.multiply(ratio).add(extra).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public LocalDateTime calcEndTime(Package pkg, LocalDateTime startTime) {
        int v = pkg.getDurationValue() == null ? 1 : pkg.getDurationValue();
        String unit = pkg.getUnit() == null ? "hour" : pkg.getUnit();
        switch (unit) {
            case "hour":
                return startTime.plusHours(v);
            case "day":
                return startTime.plusDays(v);
            case "week":
                return startTime.plusWeeks(v);
            case "month":
                return startTime.plusMonths(v);
            default:
                throw new BusinessException(ResultCode.PARAM_ERROR, "未知套餐单位：" + unit);
        }
    }

    @Override
    public String durationDesc(Package pkg) {
        int v = pkg.getDurationValue() == null ? 1 : pkg.getDurationValue();
        String unit = pkg.getUnit() == null ? "hour" : pkg.getUnit();
        switch (unit) {
            case "hour": return v + "小时";
            case "day": return v + "天";
            case "week": return v + "周";
            case "month": return v + "个月";
            default: return v + unit;
        }
    }

    /**
     * 匹配优先级最高的加价规则：
     * 规则需满足（seat_type_id 为 NULL 或命中）、（package_id 为 NULL 或命中）、时段重叠；
     * priority 降序，同 priority 时座位/套餐精确匹配优先。
     */
    private PriceRule matchRule(Long packageId, Long seatTypeId, LocalTime start) {
        LambdaQueryWrapper<PriceRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceRule::getStatus, 1)
                .and(w -> w.isNull(PriceRule::getSeatTypeId).or().eq(PriceRule::getSeatTypeId, seatTypeId))
                .and(w -> w.isNull(PriceRule::getPackageId).or().eq(PriceRule::getPackageId, packageId));
        List<PriceRule> rules = priceRuleMapper.selectList(wrapper);
        return rules.stream()
                // 时段规则：起止为 NULL 表示全时段；否则需与预约开始时间重叠
                .filter(r -> r.getTimeSlotStart() == null || r.getTimeSlotEnd() == null
                        || r.getTimeSlotStart().isBefore(r.getTimeSlotEnd())
                            ? isInRange(r, start)
                            : crossMidnight(r, start))
                .max(Comparator
                        .comparingInt((PriceRule r) -> r.getPriority() == null ? 0 : r.getPriority())
                        .thenComparingInt(r -> specificity(r, seatTypeId, packageId)))
                .orElse(null);
    }

    private boolean isInRange(PriceRule r, LocalTime t) {
        return !t.isBefore(r.getTimeSlotStart()) && t.isBefore(r.getTimeSlotEnd());
    }

    /** 跨午夜时段（如 22:00-06:00） */
    private boolean crossMidnight(PriceRule r, LocalTime t) {
        return !t.isBefore(r.getTimeSlotStart()) || t.isBefore(r.getTimeSlotEnd());
    }

    /** 精确匹配程度：2=双精确 1=单精确 0=全通配 */
    private int specificity(PriceRule r, Long seatTypeId, Long packageId) {
        int score = 0;
        if (seatTypeId != null && seatTypeId.equals(r.getSeatTypeId())) {
            score++;
        }
        if (packageId != null && packageId.equals(r.getPackageId())) {
            score++;
        }
        return score;
    }

    private PackageVO toVO(Package pkg) {
        PackageVO vo = new PackageVO();
        vo.setId(pkg.getId());
        vo.setName(pkg.getName());
        vo.setType(pkg.getType());
        vo.setTypeDesc(typeDesc(pkg.getType()));
        vo.setDurationValue(pkg.getDurationValue());
        vo.setUnit(pkg.getUnit());
        vo.setPrice(pkg.getPrice());
        vo.setOriginalPrice(pkg.getOriginalPrice());
        vo.setDescription(pkg.getDescription());
        return vo;
    }

    private String typeDesc(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case 1: return "按时长";
            case 2: return "日卡";
            case 3: return "周卡";
            case 4: return "月卡";
            default: return "未知";
        }
    }
}
