package com.studyroom.service.pkg;

import com.studyroom.entity.pkg.Package;
import com.studyroom.entity.vo.order.PackageVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 套餐计费服务（FR-UC-019 套餐计费入口）
 */
public interface PricingService {

    /**
     * 上架套餐列表（FR-UC-019 四种选项卡：按时长/日卡/周卡/月卡）
     *
     * @param type 套餐类型（可空 = 全部）
     */
    List<PackageVO> listPackages(Integer type);

    /**
     * 计算订单原价（含 price_rule 加价，FR-UC-019 / FR-AD-025）
     * <p>套餐定价 + 匹配优先级最高的加价规则（extra_ratio 倍数 + extra_price 加价）。</p>
     *
     * @param pkg        套餐
     * @param seatTypeId 座位类型ID（用于匹配加价规则）
     * @param startTime  预约开始时间（用于匹配时段规则）
     * @return 原价（套餐价 × 倍数 + 加价）
     */
    BigDecimal calcOriginalPrice(Package pkg, Long seatTypeId, LocalDateTime startTime);

    /**
     * 根据套餐计算预约结束时间
     * <p>按小时 → start + durationValue 小时；日/周/月卡 → start + N 天/周/月。</p>
     */
    LocalDateTime calcEndTime(Package pkg, LocalDateTime startTime);

    /**
     * 套餐时长描述（如 "3小时" / "1天" / "1周" / "1月"）
     */
    String durationDesc(Package pkg);
}
