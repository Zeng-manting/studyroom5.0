package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO（FR-UC-022 确认页 / FR-UC-028 支付成功页共用）
 */
@Data
@ApiModel("订单详情")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("业务订单号")
    private String orderNo;

    @ApiModelProperty("订单状态：0待支付 1已预约 2进行中 3已完成 4已取消 5退款中 6已退款 7申诉中")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;

    @ApiModelProperty("门店名称")
    private String storeName;

    @ApiModelProperty("门店地址")
    private String storeAddress;

    @ApiModelProperty("座位编号")
    private String seatNo;

    @ApiModelProperty("座位类型名称")
    private String seatTypeName;

    @ApiModelProperty("套餐名称")
    private String packageName;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("预约结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("原价")
    private BigDecimal originalPrice;

    @ApiModelProperty("套餐优惠金额")
    private BigDecimal discountAmount;

    @ApiModelProperty("券抵扣金额")
    private BigDecimal couponAmount;

    @ApiModelProperty("实付金额")
    private BigDecimal payAmount;

    @ApiModelProperty("支付方式：1余额 2微信 3混合")
    private Integer payType;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("退款金额")
    private BigDecimal refundAmount;

    @ApiModelProperty("支付截止时间（待支付状态返回，FR-UC-025）")
    private LocalDateTime payDeadline;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /* ==================== 模块7 扩展（FR-UC-038 详情页） ==================== */

    @ApiModelProperty("状态时间轴（FR-UC-038）：创建→支付→签到→暂停/恢复→结束/取消/退款")
    private List<TimelineVO> timeline;

    @ApiModelProperty("签到 GPS 距离（米），FR-UC-038 签到记录")
    private Integer signGpsDistance;

    @ApiModelProperty("实际使用分钟数（FR-UC-034 结算后）")
    private Integer actualDurationMinutes;

    @ApiModelProperty("是否爽约（FR-UC-026）")
    private Boolean noShow;

    @ApiModelProperty("申诉状态：0无 1待审 2通过 3驳回（FR-UC-032）")
    private Integer appealStatus;

    @ApiModelProperty("是否已评价（FR-UC-039）")
    private Boolean reviewed;
}
