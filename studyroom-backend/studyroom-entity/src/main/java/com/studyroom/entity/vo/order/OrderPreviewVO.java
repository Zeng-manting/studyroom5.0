package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单费用预览 VO（FR-UC-022 订单确认页：座位信息、时段、费用明细 原价-优惠-实付）
 */
@Data
@ApiModel("订单费用预览")
public class OrderPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("门店名称")
    private String storeName;

    @ApiModelProperty("门店地址")
    private String storeAddress;

    @ApiModelProperty("座位编号")
    private String seatNo;

    @ApiModelProperty("座位类型名称")
    private String seatTypeName;

    @ApiModelProperty("套餐名称")
    private String packageName;

    @ApiModelProperty("套餐时长描述（如 3小时 / 1天）")
    private String durationDesc;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("预约结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("套餐优惠金额（划线价-售价）")
    private BigDecimal discountAmount;

    @ApiModelProperty("优惠券抵扣金额")
    private BigDecimal couponAmount;

    @ApiModelProperty("券名称（未用券为空）")
    private String couponName;

    @ApiModelProperty("实付金额 = 原价 - 套餐优惠 - 券抵扣")
    private BigDecimal payAmount;

    @ApiModelProperty("当前用户余额（供前端判断余额支付是否可用）")
    private BigDecimal userBalance;
}
