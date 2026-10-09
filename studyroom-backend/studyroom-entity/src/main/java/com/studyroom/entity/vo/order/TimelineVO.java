package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单时间轴节点 VO（FR-UC-038 订单详情时间轴）
 */
@Data
@ApiModel("订单时间轴节点")
public class TimelineVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("节点类型：create/pay/sign/pause/resume/end/cancel/refund/appeal")
    private String type;

    @ApiModelProperty("节点标题")
    private String title;

    @ApiModelProperty("节点描述（如 GPS 距门店 35 米）")
    private String desc;

    @ApiModelProperty("发生时间")
    private LocalDateTime time;

    @ApiModelProperty("是否已完成节点（时间轴高亮）")
    private Boolean done;
}
