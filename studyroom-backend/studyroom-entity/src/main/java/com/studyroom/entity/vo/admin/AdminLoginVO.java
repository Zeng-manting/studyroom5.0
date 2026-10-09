package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 管理员登录返回 VO —— FR-AD-001
 *
 * <p>JWT 有效期 2 小时（studyroom.jwt.admin-ttl-seconds）；
 * 过期后端返回 401，前端路由守卫跳转登录页重新认证。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理员登录返回")
public class AdminLoginVO {

    @ApiModelProperty("JWT Token（Bearer 前缀由前端拼接）")
    private String token;

    @ApiModelProperty("有效期（秒），管理端默认 2 小时 —— FR-AD-001")
    private Long expireSeconds;

    @ApiModelProperty("管理员信息")
    private AdminInfoVO admin;

    public static AdminLoginVO of(String token, Long expireSeconds, AdminInfoVO admin) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setExpireSeconds(expireSeconds);
        vo.setAdmin(admin);
        return vo;
    }
}
