package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单结果 VO（FR-UC-018）
 * <p>返回订单号与 15 分钟支付截止时间（FR-UC-025），前端确认页倒计时展示。</p>
 */
@Data
@ApiModel("创建订单结果")
public class OrderCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("业务订单号")
    private String orderNo;

    @ApiModelProperty("订单状态：0待支付")
    private Integer status;

    @ApiModelProperty("应付金额")
    private BigDecimal payAmount;

    @ApiModelProperty("支付截止时间（超时自动取消并释放座位，FR-UC-025）")
    private LocalDateTime payDeadline;

    @ApiModelProperty("原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("券抵扣金额")
    private BigDecimal couponAmount;
}
