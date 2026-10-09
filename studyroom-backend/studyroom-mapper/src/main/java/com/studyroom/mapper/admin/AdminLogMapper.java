package com.studyroom.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.admin.AdminLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员操作日志 Mapper —— FR-AD-028
 */
@Mapper
public interface AdminLogMapper extends BaseMapper<AdminLog> {

    /**
     * 清理 N 天前的日志（FR-AD-028 保留 90 天），定时任务调用
     *
     * @return 删除行数
     */
    @Delete("DELETE FROM admin_log WHERE create_time < #{before}")
    int deleteBefore(java.time.LocalDateTime before);
}
