package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 发送邮箱验证码请求 DTO —— FR-UC-004 邮箱绑定（第一步：发送验证码）
 *
 * @author studyroom
 */
@Data
@ApiModel("发送邮箱验证码请求")
public class SendEmailCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "待绑定邮箱", required = true, example = "user@example.com")
    private String email;
}
