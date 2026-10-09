package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端优惠券 VO —— FR-AD-027
 */
@Data
@ApiModel("管理端-优惠券")
public class AdminCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("优惠券ID")
    private Long id;

    @ApiModelProperty("券名称")
    private String name;

    @ApiModelProperty("类型：1新人券 2时长抵扣券 3满减券")
    private Integer type;

    @ApiModelProperty("类型描述")
    private String typeDesc;

    @ApiModelProperty("优惠方式：1定额 2折扣 3满减")
    private Integer discountType;

    @ApiModelProperty("优惠方式描述")
    private String discountTypeDesc;

    @ApiModelProperty("满减门槛（满减券）")
    private BigDecimal thresholdAmount;

    @ApiModelProperty("定额抵扣金额")
    private BigDecimal discountAmount;

    @ApiModelProperty("折扣比例（如 0.85）")
    private BigDecimal discountRatio;

    @ApiModelProperty("领取后有效天数")
    private Integer validDays;

    @ApiModelProperty("固定起期")
    private LocalDateTime validStart;

    @ApiModelProperty("固定止期")
    private LocalDateTime validEnd;

    @ApiModelProperty("总发行量（0=不限）")
    private Integer totalQuota;

    @ApiModelProperty("已领取量")
    private Integer claimCount;

    @ApiModelProperty("已使用量")
    private Integer usedCount;

    @ApiModelProperty("每人限领")
    private Integer perUserLimit;

    @ApiModelProperty("使用说明")
    private String description;

    @ApiModelProperty("0下架 1上架")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /** 领取明细条目 */
    @Data
    @ApiModel("领取明细条目")
    public static class ClaimRecordItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("领取记录ID")
        private Long claimId;

        @ApiModelProperty("用户ID")
        private Long userId;

        @ApiModelProperty("用户昵称")
        private String nickname;

        @ApiModelProperty("状态：0未使用 1已使用 2已过期")
        private Integer status;

        @ApiModelProperty("状态描述")
        private String statusDesc;

        @ApiModelProperty("领取时间")
        private LocalDateTime claimTime;

        @ApiModelProperty("过期时间")
        private LocalDateTime expireTime;
    }
}
