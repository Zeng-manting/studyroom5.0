package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端退款记录 VO —— FR-AD-024
 */
@Data
@ApiModel("管理端-退款记录")
public class AdminRefundRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("退款ID")
    private Long refundId;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("退款类型：1阶梯取消退款 2提前离场退差 3申诉退款 4管理补偿 5手动退款")
    private Integer type;

    @ApiModelProperty("类型描述")
    private String typeDesc;

    @ApiModelProperty("退款金额")
    private BigDecimal amount;

    @ApiModelProperty("退回方式：1余额 2微信原路 3系统")
    private Integer refundWay;

    @ApiModelProperty("处理人（管理员ID，手动退款时）")
    private Long operatorId;

    @ApiModelProperty("退款原因/备注")
    private String reason;

    @ApiModelProperty("退款时间")
    private LocalDateTime createTime;
}
