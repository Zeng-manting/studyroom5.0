package com.studyroom.web.controller.admin;

import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.admin.AdminUserDetailVO;
import com.studyroom.entity.vo.admin.AdminUserListVO;
import com.studyroom.service.admin.AdminUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-用户管理 Controller —— FR-AD-014
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/api/admin/user/page</td><td>用户列表：昵称/状态/信用状态筛选 + 订单数/学习时长聚合</td></tr>
 *   <tr><td>GET</td><td>/api/admin/user/{id}</td><td>用户详情：基本信息 + 学习记录 + 评价记录</td></tr>
 *   <tr><td>POST</td><td>/api/admin/user/{id}/disable</td><td>禁用账号（校验无进行中订单）</td></tr>
 *   <tr><td>POST</td><td>/api/admin/user/{id}/enable</td><td>启用账号</td></tr>
 * </table>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "7.管理端-用户管理")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/page")
    @ApiOperation("用户列表（昵称模糊/账号状态/信用状态筛选，分页 20 条）")
    public Result<PageResult<AdminUserListVO>> page(
            @ApiParam("昵称关键字（模糊）") @RequestParam(required = false) String keyword,
            @ApiParam("账号状态：0禁用 1正常（空=全部）") @RequestParam(required = false) Integer status,
            @ApiParam("信用状态：0正常 1受限（空=全部）") @RequestParam(required = false) Integer creditStatus,
            @ApiParam("页码，默认 1") @RequestParam(defaultValue = "1") long page,
            @ApiParam("每页条数，默认 20，上限 100") @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminUserService.pageUsers(keyword, status, creditStatus,
                page, Math.min(size, 100)));
    }

    @GetMapping("/{userId}")
    @ApiOperation("用户详情（基本信息 + 学习记录 + 评价记录）")
    public Result<AdminUserDetailVO> detail(@PathVariable Long userId) {
        return Result.ok(adminUserService.getUserDetail(userId));
    }

    @PostMapping("/{userId}/disable")
    @ApiOperation("禁用账号（该用户有已预约/进行中订单时拒绝）")
    public Result<Void> disable(@PathVariable Long userId) {
        adminUserService.toggleStatus(userId, true);
        return Result.ok();
    }

    @PostMapping("/{userId}/enable")
    @ApiOperation("启用账号")
    public Result<Void> enable(@PathVariable Long userId) {
        adminUserService.toggleStatus(userId, false);
        return Result.ok();
    }
}
