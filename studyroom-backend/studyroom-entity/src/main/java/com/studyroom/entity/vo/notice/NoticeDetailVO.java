package com.studyroom.entity.vo.notice;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告详情 VO（FR-UC-043 富文本正文 + 附件下载）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("公告详情")
public class NoticeDetailVO extends NoticeListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("富文本正文（HTML，前端 rich-text 渲染）")
    private String content;

    @ApiModelProperty("附件列表（FR-UC-043 附件下载）")
    private List<AttachmentVO> attachments;

    @ApiModelProperty("阅读量")
    private Integer readCount;

    /**
     * 公告附件 VO
     */
    @Data
    @ApiModel("公告附件")
    public static class AttachmentVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("文件名")
        private String fileName;

        @ApiModelProperty("文件URL")
        private String fileUrl;

        @ApiModelProperty("文件大小（字节）")
        private Long fileSize;
    }
}
