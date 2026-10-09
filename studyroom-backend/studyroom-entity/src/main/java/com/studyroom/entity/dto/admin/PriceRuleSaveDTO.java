package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 时段加价规则新建/编辑 DTO —— FR-AD-025 时段加价
 *
 * <p>extraPrice（定额加价）与 extraRatio（倍数加价）二选一：
 * 两者均传时按优先级规则叠加（PricingService 决定），常规场景只填其一。</p>
 */
@Data
@ApiModel("加价规则保存请求")
public class PriceRuleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("规则ID（编辑时必传）")
    private Long id;

    @ApiModelProperty("座位类型ID（空=适用全部类型）")
    private Long seatTypeId;

    @ApiModelProperty("套餐ID（空=适用全部套餐）")
    private Long packageId;

    @NotNull(message = "时段起点不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "时段起点格式应为 HH:mm")
    @ApiModelProperty(value = "时段起点 HH:mm（如 18:00）", required = true)
    private String timeSlotStart;

    @NotNull(message = "时段终点不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "时段终点格式应为 HH:mm")
    @ApiModelProperty(value = "时段终点 HH:mm（如 22:00）", required = true)
    private String timeSlotEnd;

    @DecimalMin(value = "0", message = "加价金额不能为负")
    @ApiModelProperty("定额加价（元），与倍数二选一")
    private BigDecimal extraPrice;

    @DecimalMin(value = "1.0", message = "加价倍数至少为 1.0")
    @DecimalMax(value = "3.0", message = "加价倍数不能超过 3.0")
    @ApiModelProperty("倍数加价（1.00=原价，1.20=上浮20%），与定额二选一")
    private BigDecimal extraRatio;

    @Min(0)
    @Max(100)
    @ApiModelProperty("优先级（越大越优先，默认 0）")
    private Integer priority;

    @Min(0)
    @Max(1)
    @ApiModelProperty("0禁用 1启用（默认 1）")
    private Integer status;
}
