package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表项 VO（FR-UC-035/036/037）
 * <p>六个 Tab 分页；倒计时三态：待支付(红 payDeadline)/已预约(蓝 startTime)/进行中(绿 endTime)；
 * actions 由后端按状态计算，前端按此渲染动态操作按钮。</p>
 */
@Data
@ApiModel("订单列表项")
public class OrderListVO implements Serializable {

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

    @ApiModelProperty("座位编号")
    private String seatNo;

    @ApiModelProperty("套餐名称")
    private String packageName;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("预约结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("实付金额")
    private BigDecimal payAmount;

    @ApiModelProperty("退款金额")
    private BigDecimal refundAmount;

    @ApiModelProperty("支付截止时间（status=0 倒计时目标，红）")
    private LocalDateTime payDeadline;

    @ApiModelProperty("签到时间")
    private LocalDateTime signTime;

    @ApiModelProperty("实际结束时间")
    private LocalDateTime actualEndTime;

    @ApiModelProperty("是否爽约")
    private Boolean noShow;

    @ApiModelProperty("是否已评价（status=3 时评价按钮依据，FR-UC-037/039）")
    private Boolean reviewed;

    @ApiModelProperty("可执行操作按钮码：pay/cancel/sign/pause/resume/end/review/appeal（FR-UC-037）")
    private List<String> actions;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
