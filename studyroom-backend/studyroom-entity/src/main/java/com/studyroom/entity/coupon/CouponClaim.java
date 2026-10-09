package com.studyroom.entity.coupon;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户领券记录实体（coupon_claim 表）
 * <p>FR-UC-024 下单时绑定（0→1）、取消/超时释放（1→0）、支付成功记 use_time。</p>
 */
@Data
@TableName("coupon_claim")
@ApiModel("用户领券记录")
public class CouponClaim implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 0未使用 1已使用 2已过期 */
    public static final int STATUS_UNUSED = 0;
    public static final int STATUS_USED = 1;
    public static final int STATUS_EXPIRED = 2;

    @TableId
    @ApiModelProperty("领取记录ID")
    private Long id;

    @ApiModelProperty("券ID")
    private Long couponId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("0未使用 1已使用 2已过期")
    private Integer status;

    @ApiModelProperty("领取时间")
    private LocalDateTime claimTime;

    @ApiModelProperty("使用时间")
    private LocalDateTime useTime;

    @ApiModelProperty("使用订单")
    private Long orderId;

    @ApiModelProperty("过期时间")
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
