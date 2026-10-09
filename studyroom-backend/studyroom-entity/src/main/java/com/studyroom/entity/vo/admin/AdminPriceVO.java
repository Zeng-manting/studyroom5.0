package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端价格配置 VO —— FR-AD-025
 *
 * <p>套餐 / 时段加价规则 / 座位类型三块共用一个页面，字段取并集。</p>
 */
@Data
@ApiModel("管理端-价格配置项")
public class AdminPriceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ---------- 套餐 ---------- */

    @ApiModelProperty("套餐ID")
    private Long packageId;

    @ApiModelProperty("套餐名称")
    private String packageName;

    @ApiModelProperty("套餐类型：1按小时 2日卡 3周卡 4月卡")
    private Integer packageType;

    @ApiModelProperty("套餐类型描述")
    private String packageTypeDesc;

    @ApiModelProperty("时长值（小时/天/周/月）")
    private Integer durationValue;

    @ApiModelProperty("单位：hour/day/week/month")
    private String unit;

    @ApiModelProperty("售价")
    private BigDecimal price;

    @ApiModelProperty("划线原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("0下架 1上架")
    private Integer status;

    /* ---------- 加价规则 ---------- */

    @ApiModelProperty("规则ID")
    private Long ruleId;

    @ApiModelProperty("座位类型ID（NULL=全部类型）")
    private Long seatTypeId;

    @ApiModelProperty("座位类型名（全部类型时为「全部」）")
    private String seatTypeName;

    @ApiModelProperty("套餐ID（NULL=全部套餐）")
    private Long rulePackageId;

    @ApiModelProperty("套餐名（全部套餐时为「全部」）")
    private String rulePackageName;

    @ApiModelProperty("时段起点 HH:mm")
    private String timeSlotStart;

    @ApiModelProperty("时段终点 HH:mm")
    private String timeSlotEnd;

    @ApiModelProperty("加价金额（元）")
    private BigDecimal extraPrice;

    @ApiModelProperty("加价倍数（如 1.20 = 上浮 20%）")
    private BigDecimal extraRatio;

    @ApiModelProperty("优先级（越大越优先）")
    private Integer priority;

    @ApiModelProperty("0禁用 1启用")
    private Integer ruleStatus;

    /* ---------- 座位类型 ---------- */

    @ApiModelProperty("座位类型ID")
    private Long seatTypeIdDup;

    @ApiModelProperty("座位类型名")
    private String typeName;

    @ApiModelProperty("类型编码")
    private String typeCode;

    @ApiModelProperty("类型描述")
    private String typeDescription;

    @ApiModelProperty("图标")
    private String icon;

    @ApiModelProperty("类型排序")
    private Integer typeSort;

    @ApiModelProperty("类型状态：0下架 1上架")
    private Integer typeStatus;

    @ApiModelProperty("创建时间（通用）")
    private LocalDateTime createTime;
}
