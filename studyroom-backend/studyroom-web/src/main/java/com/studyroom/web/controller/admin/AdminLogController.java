package com.studyroom.web.controller.admin;

import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.admin.AdminLog;
import com.studyroom.service.admin.AdminLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-操作日志 Controller —— FR-AD-028
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/api/admin/log/page</td><td>操作日志分页查询（时间/人员/模块/状态）</td></tr>
 * </table>
 *
 * <p>日志由 AdminLogAspect 自动记录管理端全部写请求；每日 03:20 自动清理
 * 90 天前数据（AdminLogService @Scheduled）。</p>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/log")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "15.管理端-操作日志")
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping("/page")
    @ApiOperation("操作日志分页查询")
    public Result<PageResult<AdminLog>> page(
            @ApiParam("开始日期 yyyy-MM-dd") @RequestParam(required = false) String beginDate,
            @ApiParam("结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @ApiParam("管理员账号（模糊）") @RequestParam(required = false) String username,
            @ApiParam("模块（精确）") @RequestParam(required = false) String module,
            @ApiParam("0失败 1成功（空=全部）") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminLogService.pageLogs(beginDate, endDate, username, module, status, page, size));
    }
}
