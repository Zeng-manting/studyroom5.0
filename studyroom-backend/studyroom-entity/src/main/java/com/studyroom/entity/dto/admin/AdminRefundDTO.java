package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端退款审核提交 DTO —— FR-AD-012
 *
 * <p>amount 为管理员最终确认金额（默认取系统建议值，可手动调整）；
 * 服务端二次校验 0 &lt; amount ≤ 实付 - 已退。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-退款审核提交")
public class AdminRefundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "退款金额（元，最多两位小数）", required = true, example = "18.00")
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于 0")
    @DecimalMax(value = "999999.99", message = "退款金额超出上限")
    private BigDecimal amount;

    @ApiModelProperty(value = "退款原因（操作备注）", required = true)
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因不能超过 200 字")
    private String reason;
}
