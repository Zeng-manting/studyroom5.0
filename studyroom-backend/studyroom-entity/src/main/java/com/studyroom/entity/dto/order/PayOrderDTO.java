package com.studyroom.entity.dto.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 订单支付 DTO（FR-UC-023 支付方式：1余额 2模拟微信）
 */
@Data
@ApiModel("订单支付请求")
public class PayOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "支付方式不能为空")
    @ApiModelProperty(value = "支付方式：1余额 2模拟微信支付", required = true, allowableValues = "1,2")
    private Integer payType;
}
