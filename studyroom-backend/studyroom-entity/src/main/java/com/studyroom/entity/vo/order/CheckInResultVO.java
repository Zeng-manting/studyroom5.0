package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到结果 VO（FR-UC-031 成功跳使用中页 / FR-UC-032 失败显示原因+申诉入口）
 */
@Data
@ApiModel("签到结果")
public class CheckInResultVO {

    @ApiModelProperty("五重校验是否通过")
    private Boolean success;

    @ApiModelProperty("失败原因（成功为空）：二维码无效/签名校验失败/签到码过期/无有效订单/不在签到时间窗/距离门店过远")
    private String failReason;

    @ApiModelProperty("是否可申诉（失败时 true，成功 false）")
    private Boolean appealable;

    @ApiModelProperty("是否迟到签到（成功且签到时间晚于预约开始时间）")
    private Boolean late;

    @ApiModelProperty("迟到分钟数（非迟到为0）")
    private Integer lateMinutes;

    @ApiModelProperty("GPS 距离门店米数（未参与校验为空）")
    private Integer gpsDistance;

    @ApiModelProperty("订单ID（成功或失败但可定位订单时返回，供申诉使用）")
    private Long orderId;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("门店名称")
    private String storeName;

    @ApiModelProperty("座位编号（如 A-12）")
    private String seatNo;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("预约结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("签到时间")
    private LocalDateTime signTime;
}
