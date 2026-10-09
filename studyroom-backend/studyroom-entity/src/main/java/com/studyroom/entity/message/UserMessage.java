package com.studyroom.entity.message;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户消息实体（user_message 表）—— FR-UC-047 消息中心
 */
@Data
@TableName("user_message")
@ApiModel("用户消息")
public class UserMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 类型：1系统通知 2订单消息 3活动消息（FR-UC-047 分类） */
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_ORDER = 2;
    public static final int TYPE_ACTIVITY = 3;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

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

    @ApiModelProperty("0未读 1已读，FR-UC-042/047")
    private Integer isRead;

    @ApiModelProperty("已读时间")
    private LocalDateTime readTime;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
