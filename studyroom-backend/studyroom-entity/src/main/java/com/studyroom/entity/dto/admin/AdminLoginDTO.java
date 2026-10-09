package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 管理员登录请求 DTO —— FR-AD-001
 * <p>账号 + 密码 + 图形验证码（captchaId 与验证码内容）。
 * 验证码答案存 Redis（studyroom:captcha:{uuid}），一次性消费。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理员登录请求")
public class AdminLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账号不能为空")
    @Size(max = 32, message = "账号长度不能超过 32 字符")
    @ApiModelProperty("登录账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64 字符")
    @ApiModelProperty("密码（明文传输依赖 HTTPS，服务端 BCrypt 校验）")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty("验证码 ID（GET /admin/captcha 返回的 captchaId）")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    @Size(max = 8, message = "验证码长度非法")
    @ApiModelProperty("验证码内容（不区分大小写）")
    private String captchaCode;
}
