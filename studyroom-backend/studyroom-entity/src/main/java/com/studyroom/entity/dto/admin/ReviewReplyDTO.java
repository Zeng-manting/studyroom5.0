package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 评价回复 DTO —— FR-AD-018
 *
 * <p>回复内容 10-200 字；同一评价已有回复时为编辑语义（覆盖原内容）。</p>
 */
@Data
@ApiModel("评价回复请求")
public class ReviewReplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "回复内容不能为空")
    @Size(min = 10, max = 200, message = "回复内容需 10-200 字")
    @ApiModelProperty(value = "回复内容（10-200 字）", required = true)
    private String content;
}
