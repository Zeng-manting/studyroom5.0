package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端退款审核预览 VO —— FR-AD-012
 *
 * <p>系统自动计算建议退款金额，管理端可在建议值基础上手动调整（0 &lt; 金额 ≤ 可退余额）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-退款审核预览")
public class AdminRefundPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("业务订单号")
    private String orderNo;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("订单状态描述")
    private String statusDesc;

    @ApiModelProperty("实付金额")
    private BigDecimal payAmount;

    @ApiModelProperty("已退金额（含阶梯退款/提前离场退差/历史手动退款）")
    private BigDecimal refundedAmount;

    @ApiModelProperty("本次可退上限 = 实付 - 已退")
    private BigDecimal refundableAmount;

    @ApiModelProperty("系统建议退款金额（管理员可调整）")
    private BigDecimal suggestedAmount;

    @ApiModelProperty("建议依据说明")
    private String suggestionDesc;

    @ApiModelProperty("是否可退款（false 时前端禁用退款按钮）")
    private Boolean refundable;
}
