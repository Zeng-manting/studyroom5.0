package com.studyroom.entity.dto.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 创建订单 DTO（FR-UC-018 下单原子锁座）
 * <p>接口幂等由 @Idempotent(biz = "order:create") + X-Idempotent-Token 保证（FR-SY-001）。</p>
 */
@Data
@ApiModel("创建订单请求")
public class CreateOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "门店ID不能为空")
    @ApiModelProperty(value = "门店ID", required = true)
    private Long storeId;

    @NotNull(message = "座位ID不能为空")
    @ApiModelProperty(value = "座位ID", required = true)
    private Long seatId;

    @NotNull(message = "套餐ID不能为空")
    @ApiModelProperty(value = "套餐ID", required = true)
    private Long packageId;

    @NotNull(message = "开始时间不能为空")
    @ApiModelProperty(value = "预约开始时间（yyyy-MM-dd HH:mm，30分钟粒度）", required = true)
    private String startTime;

    @ApiModelProperty("优惠券领取记录ID（coupon_claim.id，不使用不传）")
    private Long couponClaimId;

    @ApiModelProperty("用户备注")
    private String remark;
}
