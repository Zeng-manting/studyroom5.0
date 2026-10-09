package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可用优惠券 VO（FR-UC-024 优惠券抵扣：订单确认页选择）
 */
@Data
@ApiModel("可用优惠券")
public class UsableCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("领取记录ID（下单时传该ID）")
    private Long claimId;

    @ApiModelProperty("券ID")
    private Long couponId;

    @ApiModelProperty("券名称")
    private String name;

    @ApiModelProperty("1新人券 2时长抵扣券 3满减券")
    private Integer type;

    @ApiModelProperty("抵扣方式描述（如 减¥5 / 85折 / 满30减8）")
    private String discountDesc;

    @ApiModelProperty("当前订单可抵扣金额")
    private BigDecimal deductibleAmount;

    @ApiModelProperty("过期时间")
    private LocalDateTime expireTime;

    @ApiModelProperty("使用说明")
    private String description;
}
