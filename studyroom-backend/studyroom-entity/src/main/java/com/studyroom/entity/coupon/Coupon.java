package com.studyroom.entity.coupon;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体（coupon 表）
 * <p>FR-UC-024 优惠券抵扣：1新人券 2时长抵扣券 3满减券；FR-AD-027 券管理</p>
 */
@Data
@TableName("coupon")
@ApiModel("优惠券")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 券类型 */
    public static final int TYPE_NEW_USER = 1;
    public static final int TYPE_DURATION = 2;
    public static final int TYPE_FULL_REDUCTION = 3;

    /** 抵扣方式 */
    public static final int DISCOUNT_FIXED = 1;
    public static final int DISCOUNT_RATIO = 2;
    public static final int DISCOUNT_FULL_REDUCTION = 3;

    @TableId
    @ApiModelProperty("券ID")
    private Long id;

    @ApiModelProperty("券名称")
    private String name;

    @ApiModelProperty("1新人券 2时长抵扣券 3满减券")
    private Integer type;

    @ApiModelProperty("1定额抵扣 2折扣 3满减")
    private Integer discountType;

    @ApiModelProperty("满减门槛")
    private BigDecimal thresholdAmount;

    @ApiModelProperty("定额抵扣金额")
    private BigDecimal discountAmount;

    @ApiModelProperty("折扣比例（如0.85）")
    private BigDecimal discountRatio;

    @ApiModelProperty("领取后有效天数")
    private Integer validDays;

    @ApiModelProperty("固定起期")
    private LocalDateTime validStart;

    @ApiModelProperty("固定止期")
    private LocalDateTime validEnd;

    @ApiModelProperty("总发行量（0不限）")
    private Integer totalQuota;

    @ApiModelProperty("已使用量")
    private Integer usedCount;

    @ApiModelProperty("已领取量")
    private Integer claimCount;

    @ApiModelProperty("每人限领")
    private Integer perUserLimit;

    @ApiModelProperty("使用说明")
    private String description;

    @ApiModelProperty("0下架 1上架")
    private Integer status;

    @ApiModelProperty("创建人")
    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
