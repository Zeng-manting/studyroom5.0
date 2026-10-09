package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端订单列表 VO —— FR-AD-010 / FR-AD-013
 *
 * <p>列表与 Excel 导出共用同一数据结构：
 * 列表接口分页返回本 VO；导出接口取全量（上限 1 万行）由 Web 层转 Excel。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-订单列表项")
public class AdminOrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("业务订单号")
    private String orderNo;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("门店名称")
    private String storeName;

    @ApiModelProperty("座位号")
    private String seatNo;

    @ApiModelProperty("套餐名称")
    private String packageName;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("预约结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("实际结束时间，未结束为 null")
    private LocalDateTime actualEndTime;

    @ApiModelProperty("实际使用分钟数")
    private Integer actualDurationMinutes;

    @ApiModelProperty("实付金额")
    private BigDecimal payAmount;

    @ApiModelProperty("累计退款金额")
    private BigDecimal refundAmount;

    @ApiModelProperty("订单状态：0待支付 1已预约 2进行中 3已完成 4已取消 5退款中 6已退款 7申诉中")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;

    @ApiModelProperty("支付方式：1余额 2微信 3混合")
    private Integer payType;

    @ApiModelProperty("支付方式描述")
    private String payTypeDesc;

    @ApiModelProperty("是否爽约")
    private Boolean noShow;

    @ApiModelProperty("申诉状态：0无 1待审 2通过 3驳回")
    private Integer appealStatus;

    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;
}
