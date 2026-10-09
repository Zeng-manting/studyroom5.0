package com.studyroom.entity.review;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价回复实体（review_reply 表）—— FR-UC-040 管理员回复展示 / FR-AD-018 管理端回复
 */
@Data
@TableName("review_reply")
@ApiModel("评价回复")
public class ReviewReply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("评价ID")
    private Long reviewId;

    @ApiModelProperty("回复内容")
    private String content;

    @ApiModelProperty("回复人（管理员ID）")
    private Long adminId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
