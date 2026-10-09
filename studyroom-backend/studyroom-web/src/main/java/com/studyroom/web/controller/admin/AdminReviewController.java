package com.studyroom.web.controller.admin;

import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.admin.ReviewReplyDTO;
import com.studyroom.entity.vo.admin.AdminReviewVO;
import com.studyroom.service.admin.AdminReviewService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端-评价管理 Controller —— FR-AD-017/018
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/review/page</td><td>评价列表：评分/时间/状态/关键字筛选</td><td>FR-AD-017</td></tr>
 *   <tr><td>POST</td><td>/api/admin/review/{id}/reply</td><td>回复评价（10-200 字，已有回复则编辑）</td><td>FR-AD-018</td></tr>
 *   <tr><td>POST</td><td>/api/admin/review/{id}/hide</td><td>隐藏评价（用户端不可见）</td><td>FR-AD-018</td></tr>
 *   <tr><td>POST</td><td>/api/admin/review/{id}/show</td><td>显示评价</td><td>FR-AD-018</td></tr>
 * </table>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/review")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "9.管理端-评价管理")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping("/page")
    @ApiOperation("评价列表（评分/时间/状态/关键字筛选，分页）")
    public Result<PageResult<AdminReviewVO>> page(
            @ApiParam("评分：1-5（空=全部）") @RequestParam(required = false) Integer score,
            @ApiParam("开始日期 yyyy-MM-dd（按评价时间）") @RequestParam(required = false) String beginDate,
            @ApiParam("结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @ApiParam("显示状态：0隐藏 1显示（空=全部）") @RequestParam(required = false) Integer status,
            @ApiParam("内容关键字（模糊）") @RequestParam(required = false) String keyword,
            @ApiParam("页码，默认 1") @RequestParam(defaultValue = "1") long page,
            @ApiParam("每页条数，默认 20，上限 100") @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminReviewService.pageReviews(score, beginDate, endDate, status,
                keyword, page, Math.min(size, 100)));
    }

    @PostMapping("/{reviewId}/reply")
    @ApiOperation("回复评价（10-200 字；已有回复则编辑覆盖）")
    public Result<AdminReviewVO> reply(@PathVariable Long reviewId,
                                       @RequestBody @Valid ReviewReplyDTO dto) {
        return Result.ok(adminReviewService.replyReview(reviewId, dto,
                AuthContextHolder.require().getId()));
    }

    @PostMapping("/{reviewId}/hide")
    @ApiOperation("隐藏评价")
    public Result<Void> hide(@PathVariable Long reviewId) {
        adminReviewService.toggleStatus(reviewId, true);
        return Result.ok();
    }

    @PostMapping("/{reviewId}/show")
    @ApiOperation("显示评价")
    public Result<Void> show(@PathVariable Long reviewId) {
        adminReviewService.toggleStatus(reviewId, false);
        return Result.ok();
    }
}
