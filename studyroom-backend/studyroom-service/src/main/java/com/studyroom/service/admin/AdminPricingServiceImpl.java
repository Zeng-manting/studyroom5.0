package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.pkg.Package;
import com.studyroom.entity.pkg.PriceRule;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.SeatType;
import com.studyroom.entity.dto.admin.PackageSaveDTO;
import com.studyroom.entity.dto.admin.PriceRuleSaveDTO;
import com.studyroom.entity.dto.admin.SeatTypeSaveDTO;
import com.studyroom.entity.vo.admin.AdminPriceVO;
import com.studyroom.mapper.pkg.PackageMapper;
import com.studyroom.mapper.pkg.PriceRuleMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.SeatTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端价格配置服务实现 —— FR-AD-025
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>套餐下架为软状态（status=0），用户端套餐列表只读 status=1</li>
 *   <li>规则删除为物理删除（price_rule 无 deleted 列）</li>
 *   <li>时段校验：起点 < 终点；跨零点时段拆两条规则配置</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPricingServiceImpl implements AdminPricingService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final PackageMapper packageMapper;
    private final PriceRuleMapper priceRuleMapper;
    private final SeatTypeMapper seatTypeMapper;
    private final SeatMapper seatMapper;

    /* ==================== 套餐 ==================== */

    @Override
    public List<AdminPriceVO> listPackages() {
        List<Package> packages = packageMapper.selectList(
                new LambdaQueryWrapper<Package>().orderByAsc(Package::getSort).orderByAsc(Package::getId));
        return packages.stream().map(this::toPackageVO).collect(Collectors.toList());
    }

    @Override
    public Long savePackage(PackageSaveDTO dto) {
        validatePackage(dto);

        Package pkg = dto.getId() == null ? new Package() : requirePackage(dto.getId());
        pkg.setName(dto.getName().trim());
        pkg.setType(dto.getType());
        pkg.setDurationValue(dto.getDurationValue());
        pkg.setUnit(dto.getUnit());
        pkg.setPrice(dto.getPrice());
        pkg.setOriginalPrice(dto.getOriginalPrice());
        pkg.setDescription(dto.getDescription());
        pkg.setSort(dto.getSort() == null ? 0 : dto.getSort());
        pkg.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        try {
            if (pkg.getId() == null) {
                packageMapper.insert(pkg);
            } else {
                packageMapper.updateById(pkg);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "套餐保存失败：数据冲突");
        }
        log.info("[AdminPricing] FR-AD-025 保存套餐: id={}, name={}", pkg.getId(), pkg.getName());
        return pkg.getId();
    }

    @Override
    public void togglePackageStatus(Long packageId, boolean online) {
        requirePackage(packageId);
        Package patch = new Package();
        patch.setId(packageId);
        patch.setStatus(online ? 1 : 0);
        packageMapper.updateById(patch);
        log.info("[AdminPricing] FR-AD-025 套餐{}: id={}", online ? "上架" : "下架", packageId);
    }

    /* ==================== 时段加价规则 ==================== */

    @Override
    public List<AdminPriceVO> listPriceRules() {
        List<PriceRule> rules = priceRuleMapper.selectList(
                new LambdaQueryWrapper<PriceRule>().orderByDesc(PriceRule::getPriority).orderByAsc(PriceRule::getId));
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> typeNames = seatTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(SeatType::getId, SeatType::getName, (a, b) -> a));
        Map<Long, String> pkgNames = packageMapper.selectList(null).stream()
                .collect(Collectors.toMap(Package::getId, Package::getName, (a, b) -> a));

        return rules.stream().map(r -> {
            AdminPriceVO vo = new AdminPriceVO();
            vo.setRuleId(r.getId());
            vo.setSeatTypeId(r.getSeatTypeId());
            vo.setSeatTypeName(r.getSeatTypeId() == null ? "全部类型"
                    : typeNames.getOrDefault(r.getSeatTypeId(), "类型#" + r.getSeatTypeId()));
            vo.setRulePackageId(r.getPackageId());
            vo.setRulePackageName(r.getPackageId() == null ? "全部套餐"
                    : pkgNames.getOrDefault(r.getPackageId(), "套餐#" + r.getPackageId()));
            vo.setTimeSlotStart(r.getTimeSlotStart() == null ? null : r.getTimeSlotStart().format(TIME));
            vo.setTimeSlotEnd(r.getTimeSlotEnd() == null ? null : r.getTimeSlotEnd().format(TIME));
            vo.setExtraPrice(r.getExtraPrice());
            vo.setExtraRatio(r.getExtraRatio());
            vo.setPriority(r.getPriority());
            vo.setRuleStatus(r.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Long savePriceRule(PriceRuleSaveDTO dto) {
        LocalTime start = LocalTime.parse(dto.getTimeSlotStart());
        LocalTime end = LocalTime.parse(dto.getTimeSlotEnd());
        if (!start.isBefore(end)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "时段起点必须早于终点（跨零点请拆分规则）");
        }
        // 定额与倍数至少填一项
        if (dto.getExtraPrice() == null && dto.getExtraRatio() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "定额加价与倍数加价至少填写一项");
        }
        // 引用有效性
        if (dto.getSeatTypeId() != null && seatTypeMapper.selectById(dto.getSeatTypeId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "座位类型不存在");
        }
        if (dto.getPackageId() != null && packageMapper.selectById(dto.getPackageId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "套餐不存在");
        }

        PriceRule rule = dto.getId() == null ? new PriceRule() : requireRule(dto.getId());
        rule.setSeatTypeId(dto.getSeatTypeId());
        rule.setPackageId(dto.getPackageId());
        rule.setTimeSlotStart(start);
        rule.setTimeSlotEnd(end);
        rule.setExtraPrice(dto.getExtraPrice() == null ? java.math.BigDecimal.ZERO : dto.getExtraPrice());
        rule.setExtraRatio(dto.getExtraRatio() == null ? java.math.BigDecimal.ONE : dto.getExtraRatio());
        rule.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        rule.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        if (rule.getId() == null) {
            priceRuleMapper.insert(rule);
        } else {
            priceRuleMapper.updateById(rule);
        }
        log.info("[AdminPricing] FR-AD-025 保存加价规则: id={}, {}~{}", rule.getId(),
                dto.getTimeSlotStart(), dto.getTimeSlotEnd());
        return rule.getId();
    }

    @Override
    public void deletePriceRule(Long ruleId) {
        requireRule(ruleId);
        priceRuleMapper.deleteById(ruleId);
        log.info("[AdminPricing] FR-AD-025 删除加价规则: id={}", ruleId);
    }

    /* ==================== 座位类型 ==================== */

    @Override
    public List<AdminPriceVO> listSeatTypes() {
        List<SeatType> types = seatTypeMapper.selectList(
                new LambdaQueryWrapper<SeatType>().orderByAsc(SeatType::getSort).orderByAsc(SeatType::getId));
        // 各类型座位数
        Map<Long, Long> seatCounts = new HashMap<>();
        for (Seat seat : seatMapper.selectList(null)) {
            seatCounts.merge(seat.getSeatTypeId(), 1L, Long::sum);
        }
        return types.stream().map(t -> {
            AdminPriceVO vo = new AdminPriceVO();
            vo.setSeatTypeIdDup(t.getId());
            vo.setTypeName(t.getName());
            vo.setTypeCode(t.getCode());
            vo.setTypeDescription(t.getDescription());
            vo.setIcon(t.getIcon());
            vo.setTypeSort(t.getSort());
            vo.setTypeStatus(t.getStatus());
            vo.setCreateTime(t.getCreateTime());
            // 借 count 字段传座位数（前端展示用）
            vo.setDurationValue(seatCounts.getOrDefault(t.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Long saveSeatType(SeatTypeSaveDTO dto) {
        SeatType type = dto.getId() == null ? new SeatType() : requireType(dto.getId());
        type.setName(dto.getName().trim());
        type.setCode(dto.getCode());
        type.setDescription(dto.getDescription());
        type.setIcon(dto.getIcon());
        type.setSort(dto.getSort() == null ? 0 : dto.getSort());
        type.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        try {
            if (type.getId() == null) {
                seatTypeMapper.insert(type);
            } else {
                seatTypeMapper.updateById(type);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "类型编码已存在：" + dto.getCode());
        }
        log.info("[AdminPricing] FR-AD-025 保存座位类型: id={}, code={}", type.getId(), type.getCode());
        return type.getId();
    }

    /* ==================== 内部方法 ==================== */

    private void validatePackage(PackageSaveDTO dto) {
        // 类型-单位匹配
        String expectedUnit = unitOfType(dto.getType());
        if (!expectedUnit.equals(dto.getUnit())) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "套餐类型与单位不匹配（" + typeDesc(dto.getType()) + " 应为单位 " + expectedUnit + "）");
        }
        // 时长值合理性：小时卡 1-24，日/周/月卡固定 1
        if (dto.getType() == 1 && (dto.getDurationValue() < 1 || dto.getDurationValue() > 24)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "小时卡时长应在 1-24 小时之间");
        }
        if (dto.getType() != 1 && dto.getDurationValue() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "日卡/周卡/月卡时长值固定为 1");
        }
        if (dto.getOriginalPrice() != null && dto.getPrice() != null
                && dto.getOriginalPrice().compareTo(dto.getPrice()) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "划线原价不能低于售价");
        }
    }

    private String unitOfType(Integer type) {
        switch (type) {
            case 1: return "hour";
            case 2: return "day";
            case 3: return "week";
            case 4: return "month";
            default: return "hour";
        }
    }

    private String typeDesc(Integer type) {
        switch (type) {
            case 1: return "按小时";
            case 2: return "日卡";
            case 3: return "周卡";
            case 4: return "月卡";
            default: return "未知";
        }
    }

    private AdminPriceVO toPackageVO(Package p) {
        AdminPriceVO vo = new AdminPriceVO();
        vo.setPackageId(p.getId());
        vo.setPackageName(p.getName());
        vo.setPackageType(p.getType());
        vo.setPackageTypeDesc(typeDesc(p.getType()));
        vo.setDurationValue(p.getDurationValue());
        vo.setUnit(p.getUnit());
        vo.setPrice(p.getPrice());
        vo.setOriginalPrice(p.getOriginalPrice());
        vo.setDescription(p.getDescription());
        vo.setSort(p.getSort());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private Package requirePackage(Long id) {
        Package pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "套餐不存在");
        }
        return pkg;
    }

    private PriceRule requireRule(Long id) {
        PriceRule rule = priceRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "加价规则不存在");
        }
        return rule;
    }

    private SeatType requireType(Long id) {
        SeatType type = seatTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "座位类型不存在");
        }
        return type;
    }
}
