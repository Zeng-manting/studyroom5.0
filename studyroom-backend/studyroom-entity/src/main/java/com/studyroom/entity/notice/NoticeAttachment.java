package com.studyroom.entity.notice;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 公告附件实体（notice_attachment 表）—— FR-UC-043 附件下载
 */
@Data
@TableName("notice_attachment")
@ApiModel("公告附件")
public class NoticeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("公告ID")
    private Long noticeId;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件URL")
    private String fileUrl;

    @ApiModelProperty("文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty("排序")
    private Integer sort;
}
