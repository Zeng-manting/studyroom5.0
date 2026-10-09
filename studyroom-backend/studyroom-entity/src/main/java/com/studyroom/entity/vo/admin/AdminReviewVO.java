package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端评价列表 VO —— FR-AD-017/018
 */
@Data
@ApiModel("管理端-评价项")
public class AdminReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("评价ID")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户昵称（匿名评价显示「匿名用户」）")
    private String nickname;

    @ApiModelProperty("门店名")
    private String storeName;

    @ApiModelProperty("座位号（可空）")
    private String seatNo;

    @ApiModelProperty("评分 1-5")
    private Integer score;

    @ApiModelProperty("预设标签 JSON 数组（已解析为 List）")
    private List<String> tags;

    @ApiModelProperty("评价内容")
    private String content;

    @ApiModelProperty("评价图片 URL 列表")
    private List<String> images;

    @ApiModelProperty("0不匿名 1匿名")
    private Integer anonymous;

    @ApiModelProperty("0隐藏 1显示（FR-AD-018）")
    private Integer status;

    @ApiModelProperty("管理员回复内容（未回复为空）")
    private String replyContent;

    @ApiModelProperty("回复时间（未回复为空）")
    private LocalDateTime replyTime;

    @ApiModelProperty("评价时间")
    private LocalDateTime createTime;
}
