package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单使用中 VO（FR-UC-031 使用中页 / FR-UC-033 暂停状态 / FR-UC-034 结算结果）
 */
@Data
@ApiModel("订单使用状态")
public class OrderUsageVO {

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("订单状态：2进行中 3已完成")
    private Integer status;

    @ApiModelProperty("门店名称")
    private String storeName;

    @ApiModelProperty("门店地址")
    private String storeAddress;

    @ApiModelProperty("座位编号")
    private String seatNo;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("预约结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("签到时间")
    private LocalDateTime signTime;

    @ApiModelProperty("剩余使用分钟数（含暂停态展示，结束订单后为0）")
    private Integer remainingMinutes;

    @ApiModelProperty("是否暂停中（FR-UC-033）")
    private Boolean paused;

    @ApiModelProperty("暂停截止时间（超时自动结算）")
    private LocalDateTime pauseDeadline;

    @ApiModelProperty("距离暂停截止剩余分钟数（未暂停为空）")
    private Integer pauseRemainingMinutes;

    @ApiModelProperty("累计暂停次数")
    private Integer pauseCount;

    @ApiModelProperty("累计暂停分钟数（结束结算时计算）")
    private Integer totalPausedMinutes;

    @ApiModelProperty("实际结束时间（FR-UC-034）")
    private LocalDateTime actualEndTime;

    @ApiModelProperty("实际使用分钟数（FR-UC-034）")
    private Integer actualDurationMinutes;

    @ApiModelProperty("提前离场退款金额（FR-UC-034 按实际时长退差额）")
    private BigDecimal refundAmount;

    @ApiModelProperty("已续时次数（FR-UC-055）")
    private Integer extendCount;

    @ApiModelProperty("是否可续时（FR-UC-055：进行中且未暂停）")
    private Boolean extendable;
}
