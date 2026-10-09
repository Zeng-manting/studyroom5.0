package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 套餐 VO（FR-UC-019 套餐计费入口：按时长/日卡/周卡/月卡四种选项卡）
 */
@Data
@ApiModel("套餐")
public class PackageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("套餐ID")
    private Long id;

    @ApiModelProperty("套餐名称")
    private String name;

    @ApiModelProperty("1按小时 2日卡 3周卡 4月卡")
    private Integer type;

    @ApiModelProperty("类型描述：按时长/日卡/周卡/月卡")
    private String typeDesc;

    @ApiModelProperty("时长值")
    private Integer durationValue;

    @ApiModelProperty("单位：hour/day/week/month")
    private String unit;

    @ApiModelProperty("实际售价")
    private BigDecimal price;

    @ApiModelProperty("划线原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("描述")
    private String description;
}
