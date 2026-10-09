package com.studyroom.entity.message;

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
 * 订阅消息授权实体（user_subscribe_message 表）—— FR-UC-044
 * <p>uk_user_scene 唯一键；前端 uni.requestSubscribeMessage 回调后上报。</p>
 */
@Data
@TableName("user_subscribe_message")
@ApiModel("订阅消息授权")
public class UserSubscribeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 授权状态：0未授权 1已授权 2已拒绝 */
    public static final int ACCEPT_NONE = 0;
    public static final int ACCEPT_YES = 1;
    public static final int ACCEPT_NO = 2;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("微信订阅消息模板ID")
    private String templateId;

    @ApiModelProperty("业务场景：orderSuccess/startSoon/endSoon/cancel/refund/timeout")
    private String scene;

    @ApiModelProperty("0未授权 1已授权 2已拒绝")
    private Integer acceptFlag;

    @ApiModelProperty("最近授权时间")
    private LocalDateTime acceptTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
