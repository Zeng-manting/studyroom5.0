package com.studyroom.web.controller.admin;

import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.admin.NoticeSaveDTO;
import com.studyroom.entity.vo.admin.AdminNoticeVO;
import com.studyroom.service.admin.AdminNoticeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端-公告管理 Controller —— FR-AD-015/016
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/notice/page</td><td>公告列表（类型/状态筛选 + 阅读量）</td><td>FR-AD-016</td></tr>
 *   <tr><td>GET</td><td>/api/admin/notice/{id}</td><td>公告详情（编辑回显，含正文与附件）</td><td>FR-AD-015</td></tr>
 *   <tr><td>POST</td><td>/api/admin/notice/save</td><td>新建/编辑公告（附件全量覆盖，可存草稿或直接发布）</td><td>FR-AD-015</td></tr>
 *   <tr><td>POST</td><td>/api/admin/notice/{id}/publish</td><td>发布公告（草稿/已下架 → 已发布）</td><td>FR-AD-016</td></tr>
 *   <tr><td>POST</td><td>/api/admin/notice/{id}/offline</td><td>下架公告</td><td>FR-AD-016</td></tr>
 *   <tr><td>DELETE</td><td>/api/admin/notice/{id}</td><td>删除公告（逻辑删除）</td><td>FR-AD-016</td></tr>
 * </table>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/notice")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "8.管理端-公告管理")
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @GetMapping("/page")
    @ApiOperation("公告列表（类型/状态筛选，分页）")
    public Result<PageResult<AdminNoticeVO>> page(
            @ApiParam("类型：1门店规则 2停业通知 3活动公告 4其他（空=全部）")
            @RequestParam(required = false) Integer type,
            @ApiParam("状态：0草稿 1已发布 2已下架（空=全部）")
            @RequestParam(required = false) Integer status,
            @ApiParam("页码，默认 1") @RequestParam(defaultValue = "1") long page,
            @ApiParam("每页条数，默认 20，上限 100") @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminNoticeService.pageNotices(type, status, page, Math.min(size, 100)));
    }

    @GetMapping("/{noticeId}")
    @ApiOperation("公告详情（编辑回显，含富文本正文与附件）")
    public Result<AdminNoticeVO> detail(@PathVariable Long noticeId) {
        return Result.ok(adminNoticeService.getNotice(noticeId));
    }

    @PostMapping("/save")
    @ApiOperation("新建/编辑公告（publishNow=true 保存后直接发布）")
    public Result<Long> save(@RequestBody @Valid NoticeSaveDTO dto) {
        return Result.ok(adminNoticeService.saveNotice(dto, AuthContextHolder.require().getId()));
    }

    @PostMapping("/{noticeId}/publish")
    @ApiOperation("发布公告")
    public Result<Void> publish(@PathVariable Long noticeId) {
        adminNoticeService.publish(noticeId, AuthContextHolder.require().getId());
        return Result.ok();
    }

    @PostMapping("/{noticeId}/offline")
    @ApiOperation("下架公告（用户端即刻不可见）")
    public Result<Void> offline(@PathVariable Long noticeId) {
        adminNoticeService.offline(noticeId);
        return Result.ok();
    }

    @DeleteMapping("/{noticeId}")
    @ApiOperation("删除公告")
    public Result<Void> delete(@PathVariable Long noticeId) {
        adminNoticeService.deleteNotice(noticeId);
        return Result.ok();
    }
}
