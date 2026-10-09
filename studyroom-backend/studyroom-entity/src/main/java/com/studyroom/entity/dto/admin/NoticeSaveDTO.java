package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 公告新建/编辑 DTO —— FR-AD-015
 *
 * <p>id 为空 = 新建；非空 = 编辑（草稿与已下架可任意编辑，
 * 已发布公告编辑后保持已发布状态并刷新更新时间）。</p>
 */
@Data
@ApiModel("公告新建/编辑请求")
public class NoticeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("公告ID（编辑时必传，新建不传）")
    private Long id;

    @ApiModelProperty("门店ID（不传=全局公告）")
    private Long storeId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 60, message = "标题不超过 60 字")
    @ApiModelProperty(value = "标题", required = true)
    private String title;

    @NotNull(message = "公告类型不能为空")
    @Min(value = 1, message = "公告类型非法")
    @Max(value = 4, message = "公告类型非法")
    @ApiModelProperty(value = "类型：1门店规则 2停业通知 3活动公告 4其他", required = true)
    private Integer type;

    @NotBlank(message = "正文不能为空")
    @Size(max = 20000, message = "正文过长")
    @ApiModelProperty(value = "富文本正文（HTML）", required = true)
    private String content;

    @NotNull(message = "重要性标识不能为空")
    @Min(0)
    @Max(1)
    @ApiModelProperty(value = "是否重要：0否 1是（重要公告触发用户端弹窗）", required = true)
    private Integer isImportant;

    @ApiModelProperty("保存后立即发布：true=直接发布（状态置 1 并记发布时间），false=存草稿")
    private Boolean publishNow = false;

    @Valid
    @ApiModelProperty("附件列表（全量覆盖式保存）")
    private List<AttachmentItem> attachments;

    /** 附件条目 */
    @Data
    @ApiModel("公告附件项")
    public static class AttachmentItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotBlank(message = "附件名不能为空")
        @Size(max = 100)
        @ApiModelProperty(value = "文件名", required = true)
        private String fileName;

        @NotBlank(message = "附件地址不能为空")
        @Size(max = 255)
        @ApiModelProperty(value = "文件URL", required = true)
        private String fileUrl;

        @Min(value = 0, message = "文件大小非法")
        @ApiModelProperty("文件大小（字节）")
        private Long fileSize;
    }
}
