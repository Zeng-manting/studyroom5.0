package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券新建/编辑 DTO —— FR-AD-027
 */
@Data
@ApiModel("优惠券保存请求")
public class CouponSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("优惠券ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "券名称不能为空")
    @Size(max = 64)
    @ApiModelProperty(value = "券名称", required = true)
    private String name;

    @NotNull(message = "券类型不能为空")
    @Min(1)
    @Max(3)
    @ApiModelProperty(value = "类型：1新人券 2时长抵扣券 3满减券", required = true)
    private Integer type;

    @NotNull(message = "优惠方式不能为空")
    @Min(1)
    @Max(3)
    @ApiModelProperty(value = "优惠方式：1定额 2折扣 3满减", required = true)
    private Integer discountType;

    @Min(value = 0, message = "门槛不能为负")
    @ApiModelProperty("满减门槛（满减券必填，其余 0）")
    private BigDecimal thresholdAmount;

    @Min(value = 0, message = "抵扣金额不能为负")
    @ApiModelProperty("定额抵扣金额（定额/满减方式必填）")
    private BigDecimal discountAmount;

    @javax.validation.constraints.DecimalMin(value = "0.01", message = "折扣比例应在 0.01~1.00 之间")
    @javax.validation.constraints.DecimalMax(value = "1.00", message = "折扣比例应在 0.01~1.00 之间")
    @ApiModelProperty("折扣比例（折扣方式必填，如 0.85 = 85 折）")
    private BigDecimal discountRatio;

    @Min(value = 1, message = "有效天数至少 1 天")
    @Max(value = 365, message = "有效天数最多 365 天")
    @ApiModelProperty("领取后有效天数（相对有效期，默认 30）")
    private Integer validDays;

    @ApiModelProperty("固定起期（与 validDays 二选一，都传则以固定期为准）")
    private LocalDateTime validStart;

    @ApiModelProperty("固定止期")
    private LocalDateTime validEnd;

    @Min(value = 0, message = "发行量不能为负")
    @ApiModelProperty("总发行量（0=不限量）")
    private Integer totalQuota;

    @Min(value = 1, message = "每人限领至少 1 张")
    @ApiModelProperty("每人限领（默认 1）")
    private Integer perUserLimit;

    @Size(max = 255)
    @ApiModelProperty("使用说明")
    private String description;

    @Min(0)
    @Max(1)
    @ApiModelProperty("0下架 1上架（默认 1 上架）")
    private Integer status;
}
