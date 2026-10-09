package com.studyroom.entity.order;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
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
 * 退款记录实体（order_refund 表）
 * <p>FR-UC-027 阶梯退款、FR-UC-034 提前离场按实际时长退差额、
 * FR-UC-026 未签到退款；管理端手动退款 FR-AD-012（模块9）。</p>
 */
@Data
@TableName("order_refund")
@ApiModel("退款记录")
public class OrderRefund implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 退款类型常量 ==================== */
    /** 阶梯退款（FR-UC-027 取消订单） */
    public static final int TYPE_LADDER = 1;
    /** 未签到退款（FR-UC-026 自动取消） */
    public static final int TYPE_NO_SHOW = 2;
    /** 申诉通过退款（FR-AD-011） */
    public static final int TYPE_APPEAL = 3;
    /** 提前离场退差额（FR-UC-034） */
    public static final int TYPE_EARLY_LEAVE = 4;
    /** 管理端手动退款（FR-AD-012） */
    public static final int TYPE_MANUAL = 5;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty("退款记录ID（雪花ID）")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("退款金额")
    private BigDecimal refundAmount;

    /** 1阶梯 2未签到 3申诉通过 4提前离场 5管理手动 */
    @ApiModelProperty("退款类型：1阶梯 2未签到 3申诉通过 4提前离场 5手动")
    private Integer refundType;

    @ApiModelProperty("阶梯类型：GT_60/FIFTEEN_TO_60/LT_15（仅阶梯退款）")
    private String stepType;

    @ApiModelProperty("退款比例（0~1）")
    private BigDecimal ratio;

    @ApiModelProperty("退款原因")
    private String refundReason;

    @ApiModelProperty("操作人（系统自动为 NULL）")
    private Long operatorId;

    @ApiModelProperty("操作备注")
    private String operatorRemark;

    /** 0失败 1成功 2处理中 */
    @ApiModelProperty("0失败 1成功 2处理中")
    private Integer status;

    @ApiModelProperty("退款成功时间")
    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
