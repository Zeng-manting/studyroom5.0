package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端公告列表/详情 VO —— FR-AD-015/016
 *
 * <p>列表不含富文本正文（content 大字段），详情才携带。</p>
 */
@Data
@ApiModel("管理端-公告项")
public class AdminNoticeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("公告ID")
    private Long id;

    @ApiModelProperty("门店ID（NULL=全局公告）")
    private Long storeId;

    @ApiModelProperty("门店名（全局公告为「全部门店」）")
    private String storeName;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("类型：1门店规则 2停业通知 3活动公告 4其他")
    private Integer type;

    @ApiModelProperty("类型描述")
    private String typeDesc;

    @ApiModelProperty("是否重要：0否 1是")
    private Integer isImportant;

    @ApiModelProperty("状态：0草稿 1已发布 2已下架")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;

    @ApiModelProperty("富文本正文（仅详情接口返回）")
    private String content;

    @ApiModelProperty("附件列表（仅详情接口返回）")
    private List<AttachmentItem> attachments;

    @ApiModelProperty("阅读量（FR-AD-016 统计）")
    private Integer readCount;

    @ApiModelProperty("发布时间（草稿为空）")
    private LocalDateTime publishTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("最后更新时间")
    private LocalDateTime updateTime;

    /** 附件条目 */
    @Data
    @ApiModel("公告附件")
    public static class AttachmentItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("文件名")
        private String fileName;

        @ApiModelProperty("文件URL")
        private String fileUrl;

        @ApiModelProperty("文件大小（字节）")
        private Long fileSize;

        @ApiModelProperty("排序")
        private Integer sort;
    }
}
