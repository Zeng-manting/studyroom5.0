package com.studyroom.entity.vo.review;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价列表项 VO（FR-UC-040）
 * <p>头像/昵称（匿名或脱敏）/评分/标签/图片/管理员回复。</p>
 */
@Data
@ApiModel("评价列表项")
public class ReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("评价ID")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("评分1-5")
    private Integer score;

    @ApiModelProperty("预设标签")
    private List<String> tags;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("图片URL列表")
    private List<String> images;

    @ApiModelProperty("是否匿名")
    private Boolean anonymous;

    @ApiModelProperty("评价人昵称（匿名时为匿名用户，否则脱敏）")
    private String nickname;

    @ApiModelProperty("评价人头像（匿名不返回）")
    private String avatar;

    @ApiModelProperty("是否本人评价")
    private Boolean mine;

    @ApiModelProperty("评价时间")
    private LocalDateTime createTime;

    @ApiModelProperty("管理员回复（FR-AD-018）")
    private ReplyVO reply;

    /**
     * 管理员回复 VO
     */
    @Data
    @ApiModel("评价管理员回复")
    public static class ReplyVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("回复内容")
        private String content;

        @ApiModelProperty("回复时间")
        private LocalDateTime createTime;
    }
}
