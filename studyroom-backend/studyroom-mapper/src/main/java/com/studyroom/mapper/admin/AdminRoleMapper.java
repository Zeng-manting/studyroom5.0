package com.studyroom.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.admin.AdminRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理端角色 Mapper（RBAC 预留，FR-AD-001 登录时取角色编码）
 *
 * @author studyroom
 */
@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {
}
