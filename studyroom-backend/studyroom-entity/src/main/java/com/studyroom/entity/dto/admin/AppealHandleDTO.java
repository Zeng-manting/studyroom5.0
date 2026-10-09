package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理端申诉处理 DTO —— FR-AD-011
 *
 * <p>标记签到成功（approve）时处理意见可选；驳回（reject）时处理意见必填（服务端校验）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-申诉处理")
public class AppealHandleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "处理意见（驳回时必填，≤200字）", example = "经核实签到码确已过期，已补记签到")
    @Size(max = 200, message = "处理意见不能超过 200 字")
    private String remark;
}
