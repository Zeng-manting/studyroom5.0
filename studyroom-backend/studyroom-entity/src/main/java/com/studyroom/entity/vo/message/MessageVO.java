package com.studyroom.entity.vo.message;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息中心列表项 VO（FR-UC-047）
 */
@Data
@ApiModel("消息列表项")
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("消息ID")
    private Long id;

    @ApiModelProperty("1系统通知 2订单消息 3活动消息")
    private Integer type;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("内容")
    private String content;

    @ApiModelProperty("业务类型：order/notice/coupon 等")
    private String bizType;

    @ApiModelProperty("业务ID")
    private Long bizId;

    @ApiModelProperty("是否已读")
    private Boolean isRead;

    @ApiModelProperty("发送时间")
    private LocalDateTime createTime;
}
