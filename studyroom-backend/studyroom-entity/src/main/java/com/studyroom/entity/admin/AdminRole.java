package com.studyroom.entity.admin;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端角色实体（admin_role 表，RBAC 预留）
 * <p>FR-AD-001 登录时按 role_id 取角色编码写入 JWT roleKey；
 * 完整 RBAC（多角色权限分配）为《技术选型与功能汇总》扩展项，当前仅做角色标识。</p>
 *
 * @author studyroom
 */
@Data
@TableName("admin_role")
@ApiModel("管理端角色")
public class AdminRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty("角色ID")
    private Long id;

    @ApiModelProperty("角色编码：SUPER_ADMIN/MANAGER/OPERATOR")
    private String code;

    @ApiModelProperty("角色名称")
    private String name;

    @ApiModelProperty("角色描述")
    private String description;

    /** 1启用 0禁用 */
    @ApiModelProperty("状态：1启用 0禁用")
    private Integer status;

    /** 本表仅 create_time（种子数据，运行期不变） */
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
