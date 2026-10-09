package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信静默登录请求 DTO —— FR-UC-001
 *
 * <p>前端 uni.login() 获取临时 code（有效期 5 分钟、一次性），
 * 后端调用微信 code2Session 换取 openid 后完成登录/注册。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("微信静默登录请求")
public class WxLoginDTO {

    /** uni.login 返回的临时登录凭证 code */
    @NotBlank(message = "登录 code 不能为空")
    @ApiModelProperty(value = "uni.login 获取的临时登录凭证", required = true)
    private String code;
}
