package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 邮箱绑定请求 DTO —— FR-UC-004 邮箱绑定（第二步：验证码校验并绑定）
 *
 * @author studyroom
 */
@Data
@ApiModel("邮箱绑定请求")
public class BindEmailDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "待绑定邮箱", required = true, example = "user@example.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    @ApiModelProperty(value = "邮箱验证码（6 位数字）", required = true)
    private String code;
}
