package com.studyroom.entity.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 登录返回 VO —— FR-UC-001 微信静默登录
 *
 * <p>前端将 token 存入 Storage，后续请求自动注入 Authorization 头（FR-UC-005）；
 * 过期后 401 → 静默重新登录并重放失败请求（FR-UC-006）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("登录返回")
public class LoginVO {

    @ApiModelProperty("JWT Token（Bearer 前缀由前端拼接）")
    private String token;

    @ApiModelProperty("有效期（秒），用户端默认 7 天 —— FR-UC-005")
    private Long expireSeconds;

    @ApiModelProperty("是否首次登录（本次自动注册新用户）—— FR-UC-002")
    private Boolean newUser;

    @ApiModelProperty("用户信息")
    private UserVO user;

    public static LoginVO of(String token, Long expireSeconds, Boolean newUser, UserVO user) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpireSeconds(expireSeconds);
        vo.setNewUser(newUser);
        vo.setUser(user);
        return vo;
    }
}
