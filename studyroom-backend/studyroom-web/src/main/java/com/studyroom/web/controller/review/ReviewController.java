package com.studyroom.web.controller.review;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.Idempotent;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.order.ReviewCreateDTO;
import com.studyroom.entity.vo.review.ReviewVO;
import com.studyroom.service.review.ReviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * 用户端-评价 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>POST</td><td>/api/review</td><td>提交评价（5星+标签+文字+图片）</td><td>FR-UC-039</td></tr>
 *   <tr><td>GET</td><td>/api/review/list</td><td>门店评价列表（脱敏）</td><td>FR-UC-040</td></tr>
 *   <tr><td>GET</td><td>/api/review/mine</td><td>我的评价列表</td><td>FR-UC-040</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
@Api(tags = "用户端-评价")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 提交评价 —— FR-UC-039
     * <p>幂等 + 限流防刷评。</p>
     */
    @PostMapping
    @ApiOperation("提交评价（一单一评）")
    @Idempotent(value = "review:submit", timeout = 60, unit = TimeUnit.SECONDS)
    @RateLimiter(value = "review:submit", permits = 3, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<Long> submit(
            @CurrentUser LoginUser user,
            @Valid @RequestBody ReviewCreateDTO dto) {
        return Result.ok(reviewService.submitReview(user.getId(), dto));
    }

    /**
     * 门店评价列表 —— FR-UC-040（门店详情页展示）
     */
    @GetMapping("/list")
    @ApiOperation("门店评价列表（昵称脱敏 + 管理员回复）")
    public Result<PageResult<ReviewVO>> list(
            @CurrentUser LoginUser user,
            @ApiParam(value = "门店ID", required = true) @RequestParam Long storeId,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Long page,
            @ApiParam(value = "每页条数", defaultValue = "10") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(reviewService.listStoreReviews(user.getId(), storeId, page, size));
    }

    /**
     * 我的评价列表 —— FR-UC-040
     */
    @GetMapping("/mine")
    @ApiOperation("我的评价列表")
    public Result<PageResult<ReviewVO>> mine(
            @CurrentUser LoginUser user,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Long page,
            @ApiParam(value = "每页条数", defaultValue = "10") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(reviewService.listMyReviews(user.getId(), page, size));
    }
}
