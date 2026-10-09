package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 优惠券发放 DTO —— FR-AD-027
 */
@Data
@ApiModel("优惠券发放请求")
public class CouponGrantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "优惠券ID不能为空")
    @ApiModelProperty(value = "优惠券ID", required = true)
    private Long couponId;

    @NotEmpty(message = "用户ID列表不能为空")
    @Size(max = 200, message = "单次发放最多 200 人")
    @ApiModelProperty(value = "用户ID列表（≤200）", required = true)
    private List<Long> userIds;
}
