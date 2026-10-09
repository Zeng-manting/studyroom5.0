package com.studyroom.entity.order;

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
 * 订单支付记录实体（order_payment 表）
 * <p>FR-UC-023 支付方式：1余额 2微信（模拟）；退款时按支付记录原路回退。</p>
 */
@Data
@TableName("order_payment")
@ApiModel("订单支付记录")
public class OrderPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("订单号（冗余）")
    private String orderNo;

    /** 1余额 2微信 */
    @ApiModelProperty("1余额 2微信")
    private Integer payType;

    @ApiModelProperty("支付金额")
    private BigDecimal payAmount;

    @ApiModelProperty("微信交易号")
    private String transactionId;

    /** 0失败 1成功 2已退款 */
    @ApiModelProperty("0失败 1成功 2已退款")
    private Integer payStatus;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
