package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 意见反馈提交 DTO —— FR-UC-052
 */
@Data
@ApiModel("意见反馈参数")
public class FeedbackCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "反馈类型不能为空")
    @Min(1)
    @Max(4)
    @ApiModelProperty("1功能建议 2问题反馈 3投诉 4其他")
    private Integer type;

    @NotNull(message = "反馈内容不能为空")
    @Size(min = 5, max = 1000, message = "反馈内容需5-1000字")
    @ApiModelProperty("反馈内容")
    private String content;

    @Size(max = 6, message = "图片最多6张")
    @ApiModelProperty("图片URL列表，最多6张")
    private List<String> images;

    @Size(max = 128, message = "联系方式过长")
    @ApiModelProperty("联系方式（手机/邮箱/微信号，可空）")
    private String contact;
}
