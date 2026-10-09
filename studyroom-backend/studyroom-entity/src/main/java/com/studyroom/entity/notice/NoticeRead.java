package com.studyroom.entity.notice;

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
 * 公告已读记录实体（notice_read 表）—— FR-UC-042 已读/未读标识
 * <p>uk_user_notice 唯一键，重复标记已读由 insert 冲突忽略。</p>
 */
@Data
@TableName("notice_read")
@ApiModel("公告已读记录")
public class NoticeRead implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("公告ID")
    private Long noticeId;

    @ApiModelProperty("已读时间")
    private LocalDateTime readTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
