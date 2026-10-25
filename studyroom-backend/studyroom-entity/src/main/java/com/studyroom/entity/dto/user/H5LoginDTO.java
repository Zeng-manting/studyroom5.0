package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * H5 端账号登录请求 DTO
 *
 * <p>H5 环境没有微信登录能力，改用「账号」标识用户：
 * 同一账号首次登录自动注册，再次登录复用同一用户。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("H5 端账号登录请求")
public class H5LoginDTO {

    /** H5 登录账号（字母/数字/下划线/中文，2-32 位） */
    @NotBlank(message = "账号不能为空")
    @Size(min = 2, max = 32, message = "账号长度为 2-32 个字符")
    @Pattern(regexp = "^[A-Za-z0-9_\\u4e00-\\u9fa5]+$", message = "账号仅支持字母、数字、下划线或中文")
    @ApiModelProperty(value = "H5 登录账号（同一账号即同一用户）", required = true, example = "student01")
    private String account;
}
