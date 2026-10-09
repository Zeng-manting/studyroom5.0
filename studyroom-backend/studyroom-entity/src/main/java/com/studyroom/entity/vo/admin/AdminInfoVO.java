package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员信息 VO —— FR-AD-001
 * <p>登录成功与 GET /admin/me 共用；不包含密码等敏感字段。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理员信息")
public class AdminInfoVO {

    @ApiModelProperty("管理员ID")
    private Long id;

    @ApiModelProperty("登录账号")
    private String username;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("头像 URL")
    private String avatar;

    @ApiModelProperty("角色编码（SUPER_ADMIN/MANAGER/OPERATOR）")
    private String roleCode;

    @ApiModelProperty("角色名称（超级管理员/店长/运营）")
    private String roleName;

    @ApiModelProperty("最近登录时间")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty("最近登录IP")
    private String lastLoginIp;
}
