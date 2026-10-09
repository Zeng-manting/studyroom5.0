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
import java.time.LocalTime;

/**
 * 价格加价规则实体（price_rule 表）
 * <p>FR-AD-025 价格规则；FR-UC-019 计费时按优先级匹配（包间/VIP/晚间时段加价）</p>
 */
@Data
@TableName("price_rule")
@ApiModel("价格加价规则")
public class PriceRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("规则ID")
    private Long id;

    @ApiModelProperty("座位类型ID（NULL=全部）")
    private Long seatTypeId;

    @ApiModelProperty("套餐ID（NULL=全部）")
    private Long packageId;

    @ApiModelProperty("时段起点（如 18:00）")
    private LocalTime timeSlotStart;

    @ApiModelProperty("时段终点（如 22:00）")
    private LocalTime timeSlotEnd;

    @ApiModelProperty("加价金额")
    private BigDecimal extraPrice;

    @ApiModelProperty("加价倍数")
    private BigDecimal extraRatio;

    @ApiModelProperty("优先级（数字越大越优先）")
    private Integer priority;

    @ApiModelProperty("0禁用 1启用")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
