package com.studyroom.entity.pkg;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套餐实体（package 表）
 * <p>FR-UC-019 套餐计费入口：按时长/日卡/周卡/月卡；FR-AD-025 套餐管理</p>
 * <p>类名 Package 与 java.lang.Package 同名，因业务语义优先，使用时本工程内自动遮蔽。</p>
 */
@Data
@TableName("package")
@ApiModel("套餐")
public class Package implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("套餐ID")
    private Long id;

    @ApiModelProperty("套餐名称")
    private String name;

    /** 1按小时 2日卡 3周卡 4月卡，FR-UC-019 选项卡 */
    @ApiModelProperty("1按小时 2日卡 3周卡 4月卡")
    private Integer type;

    @ApiModelProperty("时长值：小时/天/周/月")
    private Integer durationValue;

    /** hour/day/week/month */
    @ApiModelProperty("单位：hour/day/week/month")
    private String unit;

    @ApiModelProperty("实际售价")
    private BigDecimal price;

    @ApiModelProperty("划线原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("0下架 1上架")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
