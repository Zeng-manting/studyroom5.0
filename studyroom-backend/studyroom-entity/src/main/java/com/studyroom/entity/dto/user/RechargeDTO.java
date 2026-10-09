package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充值 DTO —— FR-UC-048（模拟微信支付充值）
 */
@Data
@ApiModel("充值参数")
public class RechargeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "1.00", message = "单次充值最低1元")
    @DecimalMax(value = "2000.00", message = "单次充值最高2000元")
    @ApiModelProperty("充值金额（1~2000元）")
    private BigDecimal amount;
}
