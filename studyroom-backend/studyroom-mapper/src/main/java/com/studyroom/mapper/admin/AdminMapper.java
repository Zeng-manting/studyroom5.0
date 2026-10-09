package com.studyroom.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.admin.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 管理员 Mapper
 * <p>FR-AD-001 管理员登录：按用户名查询 + 登录信息更新。</p>
 *
 * @author studyroom
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

    /**
     * 按用户名查询（uk_username 唯一，至多一条；逻辑删除自动过滤）
     */
    @Select("SELECT * FROM admin WHERE username = #{username} AND deleted = 0 LIMIT 1")
    Admin selectByUsername(@Param("username") String username);

    /**
     * 登录成功后更新最近登录时间 / IP（FR-AD-001，失败不影响主流程）
     */
    @Update("UPDATE admin SET last_login_time = #{time}, last_login_ip = #{ip} " +
            "WHERE id = #{adminId} AND deleted = 0")
    int updateLoginInfo(@Param("adminId") Long adminId,
                        @Param("time") LocalDateTime time,
                        @Param("ip") String ip);
}
