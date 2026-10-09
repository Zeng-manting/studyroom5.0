package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.result.PageResult;
import com.studyroom.entity.admin.AdminLog;
import com.studyroom.mapper.admin.AdminLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理员操作日志服务 —— FR-AD-028
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>日志查询：时间/人员/模块/状态筛选分页</li>
 *   <li>日志清理：每日凌晨清理 90 天前日志（@Scheduled）</li>
 * </ul>
 *
 * <p>日志写入由 {@code AdminLogAspect}（web 模块切面）自动完成，本类只读 + 清理。</p>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogService {

    /** 保留天数（FR-AD-028） */
    private static final int RETENTION_DAYS = 90;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AdminLogMapper adminLogMapper;

    /**
     * 操作日志分页查询
     *
     * @param beginDate 开始日期（含当日）
     * @param endDate   结束日期（含当日全天）
     * @param username  管理员账号（模糊）
     * @param module    模块（精确）
     * @param status    0失败 1成功（空=全部）
     */
    public PageResult<AdminLog> pageLogs(String beginDate, String endDate, String username,
                                         String module, Integer status, long page, long size) {
        LambdaQueryWrapper<AdminLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(beginDate)) {
            wrapper.ge(AdminLog::getCreateTime, LocalDate.parse(beginDate, DATE).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(AdminLog::getCreateTime,
                    LocalDate.parse(endDate, DATE).atStartOfDay().plusDays(1).minusNanos(1));
        }
        if (StringUtils.hasText(username)) {
            wrapper.like(AdminLog::getUsername, username.trim());
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(AdminLog::getModule, module.trim());
        }
        if (status != null) {
            wrapper.eq(AdminLog::getStatus, status);
        }
        wrapper.orderByDesc(AdminLog::getCreateTime);

        Page<AdminLog> p = adminLogMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /**
     * 每日 03:20 清理 90 天前日志（FR-AD-028 保留策略）
     */
    @Scheduled(cron = "0 20 3 * * ?")
    public void cleanupExpiredLogs() {
        LocalDateTime before = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = adminLogMapper.deleteBefore(before);
        if (deleted > 0) {
            log.info("[AdminLog] FR-AD-028 清理 {} 天前操作日志 {} 条", RETENTION_DAYS, deleted);
        }
    }
}
