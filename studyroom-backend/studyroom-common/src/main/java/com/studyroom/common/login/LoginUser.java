package com.studyroom.common.login;

import com.studyroom.common.enums.UserTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户快照
 * <p>仅在请求线程内传递，不持久化。包含足够做权限校验的最小信息：
 * id、openid/adminId、userType，以及可选的 roleKey（管理员侧）。</p>
 *
 * @author studyroom
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务主键：用户端为 user.id，管理员端为 admin.id */
    private Long id;

    /** 微信小程序 openid（管理员为空） */
    private String openid;

    /** 用户类型 */
    private UserTypeEnum userType;

    /** 角色 key（管理员：admin/super_admin；普通用户：user）。供 @PreAuthorize 校验 */
    private String roleKey;

    /** 登录 token jti（用于踢下线 / 黑名单校验） */
    private String jti;
}