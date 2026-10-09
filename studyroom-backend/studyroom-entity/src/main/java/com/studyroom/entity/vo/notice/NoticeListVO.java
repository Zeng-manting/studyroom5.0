package com.studyroom.entity.vo.notice;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告列表项 VO（FR-UC-041/042）
 */
@Data
@ApiModel("公告列表项")
public class NoticeListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("公告ID")
    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("1门店规则 2停业通知 3活动公告 4其他")
    private Integer type;

    @ApiModelProperty("类型描述")
    private String typeDesc;

    @ApiModelProperty("是否重要公告（弹窗展示，FR-UC-042）")
    private Boolean important;

    @ApiModelProperty("是否已读：0未读 1已读（FR-UC-042 已读/未读标识）")
    private Boolean hasRead;

    @ApiModelProperty("发布时间")
    private LocalDateTime publishTime;
}
