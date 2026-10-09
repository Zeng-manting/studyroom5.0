package com.studyroom.entity.order;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 订单实体（orders 表）
 * <p>FR-UC-015~038 订单全生命周期；FR-UC-018 version 乐观锁；
 * FR-SY-001 idempotent_key 幂等唯一键；FR-UC-027 refund_* 阶梯退款字段。</p>
 */
@Data
@TableName("orders")
@ApiModel("订单")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 订单状态常量 ==================== */
    public static final int STATUS_PENDING_PAY = 0;
    public static final int STATUS_RESERVED = 1;
    public static final int STATUS_IN_USE = 2;
    public static final int STATUS_FINISHED = 3;
    public static final int STATUS_CANCELED = 4;
    public static final int STATUS_REFUNDING = 5;
    public static final int STATUS_REFUNDED = 6;
    public static final int STATUS_APPEALING = 7;

    /* ==================== 支付方式常量（FR-UC-023） ==================== */
    public static final int PAY_TYPE_BALANCE = 1;
    public static final int PAY_TYPE_WECHAT = 2;
    public static final int PAY_TYPE_MIXED = 3;

    /** 处于"占用座位"语义的订单状态集合（FR-UC-018：待支付 15 分钟 + 已预约 + 进行中） */
    public static final List<Integer> OCCUPYING_STATUSES = Collections.unmodifiableList(
            Arrays.asList(STATUS_PENDING_PAY, STATUS_RESERVED, STATUS_IN_USE));

    @TableId
    @ApiModelProperty("订单ID")
    private Long id;

    @ApiModelProperty("业务订单号（年月日+6位序号）")
    private String orderNo;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("门店ID")
    private Long storeId;

    @ApiModelProperty("座位ID")
    private Long seatId;

    @ApiModelProperty("座位类型ID（冗余便于统计）")
    private Long seatTypeId;

    @ApiModelProperty("套餐ID")
    private Long packageId;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("预约结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("优惠金额")
    private BigDecimal discountAmount;

    @ApiModelProperty("优惠券ID，FR-UC-024")
    private Long couponId;

    @ApiModelProperty("券抵扣金额")
    private BigDecimal couponAmount;

    @ApiModelProperty("实付金额")
    private BigDecimal payAmount;

    /** 0待支付 1已预约 2进行中 3已完成 4已取消 5退款中 6已退款 7申诉中 */
    @ApiModelProperty("0待支付 1已预约 2进行中 3已完成 4已取消 5退款中 6已退款 7申诉中")
    private Integer status;

    /** 1余额 2微信 3混合 */
    @ApiModelProperty("1余额 2微信 3混合")
    private Integer payType;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("取消时间")
    private LocalDateTime cancelTime;

    @ApiModelProperty("取消原因")
    private String cancelReason;

    @ApiModelProperty("签到时间，FR-UC-030")
    private LocalDateTime signTime;

    @ApiModelProperty("签到纬度")
    private BigDecimal signLatitude;

    @ApiModelProperty("签到经度")
    private BigDecimal signLongitude;

    @ApiModelProperty("GPS距离门店米数")
    private Integer signGpsDistance;

    @ApiModelProperty("最近一次暂停开始时间")
    private LocalDateTime pauseStartTime;

    @ApiModelProperty("暂停预计恢复时间，FR-UC-033 默认+30min")
    private LocalDateTime pauseEndTime;

    @ApiModelProperty("累计暂停次数")
    private Integer pauseCount;

    @ApiModelProperty("实际结束时间，FR-UC-034")
    private LocalDateTime actualEndTime;

    @ApiModelProperty("实际使用分钟数")
    private Integer actualDurationMinutes;

    /** 是否爽约：0否 1是，FR-UC-026 */
    @ApiModelProperty("是否爽约：0否 1是")
    private Integer isNoShow;

    @ApiModelProperty("0无 1待审 2通过 3驳回")
    private Integer appealStatus;

    @ApiModelProperty("退款金额，FR-UC-027")
    private BigDecimal refundAmount;

    @ApiModelProperty("退款时间")
    private LocalDateTime refundTime;

    @ApiModelProperty("退款原因")
    private String refundReason;

    @ApiModelProperty("续时次数，FR-UC-055")
    private Integer extendCount;

    @ApiModelProperty("接口幂等键，FR-SY-001")
    private String idempotentKey;

    @ApiModelProperty("用户备注")
    private String remark;

    @Version
    private Integer version;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
