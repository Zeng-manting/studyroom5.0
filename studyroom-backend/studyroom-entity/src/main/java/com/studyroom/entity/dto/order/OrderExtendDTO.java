package com.studyroom.entity.dto.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 进行中续时 DTO（FR-UC-055）
 *
 * @author studyroom
 */
@Data
@ApiModel("进行中续时请求")
public class OrderExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "续时时长不能为空")
    @Min(value = 1, message = "续时时长至少 1 小时")
    @Max(value = 12, message = "单次续时最长 12 小时")
    @ApiModelProperty(value = "续时时长（小时，1~12）", required = true)
    private Integer hours;

    @NotNull(message = "支付方式不能为空")
    @Min(value = 1, message = "支付方式不合法")
    @Max(value = 2, message = "支付方式不合法")
    @ApiModelProperty(value = "支付方式：1余额 2微信（模拟）", required = true)
    private Integer payType;
}
