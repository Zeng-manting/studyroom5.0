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
 * 订单续时记录实体（order_extension 表）
 * <p>FR-UC-055 进行中续时：按当前套餐单价折算费用，支付后顺延订单结束时间并续占座位。</p>
 *
 * @author studyroom
 */
@Data
@TableName("order_extension")
@ApiModel("订单续时记录")
public class OrderExtension implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 支付状态常量（FR-UC-055） ==================== */
    /** 待支付（预留：真实微信支付异步回调场景） */
    public static final int PAY_STATUS_WAITING = 0;
    /** 已支付 */
    public static final int PAY_STATUS_PAID = 1;
    /** 已退款 */
    public static final int PAY_STATUS_REFUNDED = 2;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty("续时记录ID（雪花ID）")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("续时时长（小时）")
    private Integer hours;

    @ApiModelProperty("续时金额（按套餐单价折算）")
    private BigDecimal amount;

    /** 1余额 2微信（沿用 orders 支付方式枚举） */
    @ApiModelProperty("支付方式：1余额 2微信")
    private Integer payType;

    /** 0待支付 1已支付 2已退款 */
    @ApiModelProperty("支付状态：0待支付 1已支付 2已退款")
    private Integer payStatus;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("续时后结束时间")
    private LocalDateTime newEndTime;

    /** 本表仅 create_time（无 update_time/deleted，与建表 SQL 一致） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
