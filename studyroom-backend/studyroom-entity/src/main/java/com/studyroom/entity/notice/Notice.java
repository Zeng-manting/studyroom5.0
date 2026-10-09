package com.studyroom.entity.notice;

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
 * 公告实体（notice 表）—— FR-UC-041/042/043 用户端公告域
 */
@Data
@TableName("notice")
@ApiModel("公告")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 类型：1门店规则 2停业通知 3活动公告 4其他（FR-UC-041 分类） */
    public static final int TYPE_RULE = 1;
    public static final int TYPE_CLOSURE = 2;
    public static final int TYPE_ACTIVITY = 3;
    public static final int TYPE_OTHER = 4;

    /** 状态：0草稿 1已发布 2已下架 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_OFFLINE = 2;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("门店ID（NULL=全局公告）")
    private Long storeId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("1门店规则 2停业通知 3活动公告 4其他")
    private Integer type;

    @ApiModelProperty("富文本正文，FR-UC-043")
    private String content;

    @ApiModelProperty("是否重要公告：0否 1是（FR-UC-042 重要公告弹窗）")
    private Integer isImportant;

    @ApiModelProperty("0草稿 1已发布 2已下架")
    private Integer status;

    @ApiModelProperty("发布时间")
    private LocalDateTime publishTime;

    @ApiModelProperty("发布人")
    private Long publisherId;

    @ApiModelProperty("阅读量统计")
    private Integer readCount;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
