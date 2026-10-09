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

/**
 * 套餐新建/编辑 DTO —— FR-AD-025 套餐价格
 */
@Data
@ApiModel("套餐保存请求")
public class PackageSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("套餐ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 64)
    @ApiModelProperty(value = "套餐名称", required = true)
    private String name;

    @NotNull(message = "套餐类型不能为空")
    @Min(1)
    @Max(4)
    @ApiModelProperty(value = "类型：1按小时 2日卡 3周卡 4月卡", required = true)
    private Integer type;

    @NotNull(message = "时长值不能为空")
    @Min(value = 1, message = "时长值至少为 1")
    @ApiModelProperty(value = "时长值（小时/天/周/月）", required = true)
    private Integer durationValue;

    @ApiModelProperty(value = "单位：hour/day/week/month", required = true)
    @NotBlank(message = "单位不能为空")
    private String unit;

    @NotNull(message = "售价不能为空")
    @Min(value = 0, message = "售价不能为负")
    @ApiModelProperty(value = "售价（元）", required = true)
    private BigDecimal price;

    @Min(value = 0, message = "原价不能为负")
    @ApiModelProperty("划线原价（可空）")
    private BigDecimal originalPrice;

    @Size(max = 255)
    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("排序（默认 0）")
    private Integer sort;

    @Min(0)
    @Max(1)
    @ApiModelProperty("0下架 1上架（默认 1）")
    private Integer status;
}
