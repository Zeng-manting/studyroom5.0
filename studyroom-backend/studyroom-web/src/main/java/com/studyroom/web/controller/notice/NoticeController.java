package com.studyroom.web.controller.notice;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.notice.NoticeDetailVO;
import com.studyroom.entity.vo.notice.NoticeListVO;
import com.studyroom.service.notice.NoticeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端-公告 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/notice/list</td><td>公告列表（分类+已读标识）</td><td>FR-UC-041/042</td></tr>
 *   <tr><td>GET</td><td>/api/notice/popup</td><td>重要公告弹窗（最新未读）</td><td>FR-UC-042</td></tr>
 *   <tr><td>GET</td><td>/api/notice/{id}</td><td>公告详情（富文本+附件）</td><td>FR-UC-043</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
@Api(tags = "用户端-公告")
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 公告列表 —— FR-UC-041 分类 / FR-UC-042 已读标识
     */
    @GetMapping("/list")
    @ApiOperation("公告列表（0全部 1门店规则 2停业通知 3活动公告 4其他）")
    public Result<PageResult<NoticeListVO>> list(
            @CurrentUser LoginUser user,
            @ApiParam(value = "类型：0全部 1门店规则 2停业通知 3活动公告 4其他", defaultValue = "0")
            @RequestParam(defaultValue = "0") Integer type,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Long page,
            @ApiParam(value = "每页条数", defaultValue = "10") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(noticeService.listNotices(user.getId(), type, page, size));
    }

    /**
     * 重要公告弹窗 —— FR-UC-042（data 为 null 表示无弹窗）
     */
    @GetMapping("/popup")
    @ApiOperation("重要公告弹窗（最新一条重要未读；无则 data=null）")
    public Result<NoticeDetailVO> popup(@CurrentUser LoginUser user) {
        return Result.ok(noticeService.getImportantPopup(user.getId()));
    }

    /**
     * 公告详情 —— FR-UC-043 富文本正文 + 附件下载（首次访问记已读）
     */
    @GetMapping("/{id}")
    @ApiOperation("公告详情（自动标记已读）")
    public Result<NoticeDetailVO> detail(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long noticeId) {
        return Result.ok(noticeService.getNoticeDetail(user.getId(), noticeId));
    }
}
