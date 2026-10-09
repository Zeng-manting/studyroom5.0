package com.studyroom.entity.user;

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
 * 意见反馈实体（feedback 表）—— FR-UC-052
 */
@Data
@TableName("feedback")
@ApiModel("意见反馈")
public class Feedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 类型：1功能建议 2问题反馈 3投诉 4其他 */
    public static final int TYPE_SUGGESTION = 1;
    public static final int TYPE_BUG = 2;
    public static final int TYPE_COMPLAINT = 3;
    public static final int TYPE_OTHER = 4;

    /** 状态：0待处理 1已处理 2已关闭 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_HANDLED = 1;
    public static final int STATUS_CLOSED = 2;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("用户ID（匿名反馈时为NULL）")
    private Long userId;

    @ApiModelProperty("1功能建议 2问题反馈 3投诉 4其他")
    private Integer type;

    @ApiModelProperty("反馈内容")
    private String content;

    @ApiModelProperty("图片URL数组 JSON，最多6张")
    private String images;

    @ApiModelProperty("联系方式（手机/邮箱/微信号）")
    private String contact;

    @ApiModelProperty("0待处理 1已处理 2已关闭")
    private Integer status;

    @ApiModelProperty("管理员回复")
    private String reply;

    @ApiModelProperty("处理人")
    private Long handlerId;

    @ApiModelProperty("处理时间")
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
