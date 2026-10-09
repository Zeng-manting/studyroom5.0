package com.studyroom.entity.admin;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员实体（admin 表）
 * <p>FR-AD-001 管理员登录：账号 + BCrypt 密码 + 图形验证码 + JWT 2 小时。
 * 角色通过 role_id 关联 admin_role（RBAC 预留，见《技术选型与功能汇总》扩展项）。</p>
 *
 * @author studyroom
 */
@Data
@TableName("admin")
@ApiModel("管理员")
public class Admin implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 状态常量 ==================== */
    /** 正常 */
    public static final int STATUS_ENABLED = 1;
    /** 禁用 */
    public static final int STATUS_DISABLED = 0;

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("管理员ID（雪花ID）")
    private Long id;

    @ApiModelProperty("登录账号")
    private String username;

    /** FR-AD-001 BCrypt 加密存储 */
    @ApiModelProperty("BCrypt 密码（不出参）")
    private String password;

    @ApiModelProperty("真实姓名")
    private String realName;

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("角色ID（关联 admin_role，RBAC 预留）")
    private Long roleId;

    /** 1正常 0禁用 */
    @ApiModelProperty("状态：1正常 0禁用")
    private Integer status;

    @ApiModelProperty("最近登录时间")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty("最近登录IP")
    private String lastLoginIp;

    @Version
    @ApiModelProperty("乐观锁版本号")
    private Integer version;

    @TableLogic
    @ApiModelProperty("逻辑删除：0正常 1已删除")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
