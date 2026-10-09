package com.studyroom.web.controller.user;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.Idempotent;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.user.FeedbackCreateDTO;
import com.studyroom.entity.dto.user.RechargeDTO;
import com.studyroom.entity.vo.user.BalanceLogVO;
import com.studyroom.entity.vo.user.StudyStatVO;
import com.studyroom.service.user.ProfileService;
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
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 用户端-个人中心 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/profile/wallet</td><td>钱包余额</td><td>FR-UC-048</td></tr>
 *   <tr><td>POST</td><td>/api/profile/recharge</td><td>充值（模拟微信支付）</td><td>FR-UC-048</td></tr>
 *   <tr><td>GET</td><td>/api/profile/balance-logs</td><td>交易记录分页</td><td>FR-UC-048</td></tr>
 *   <tr><td>GET</td><td>/api/profile/study-stats</td><td>学习统计（柱状图+热力图）</td><td>FR-UC-049</td></tr>
 *   <tr><td>POST</td><td>/api/profile/privacy-agree</td><td>同意隐私协议</td><td>FR-UC-050</td></tr>
 *   <tr><td>POST</td><td>/api/profile/feedback</td><td>提交意见反馈</td><td>FR-UC-052</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Api(tags = "用户端-个人中心")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 钱包余额 —— FR-UC-048
     */
    @GetMapping("/wallet")
    @ApiOperation("钱包余额")
    public Result<BigDecimal> wallet(@CurrentUser LoginUser user) {
        return Result.ok(profileService.getBalance(user.getId()));
    }

    /**
     * 充值 —— FR-UC-048（模拟微信支付，直接到账）
     */
    @PostMapping("/recharge")
    @ApiOperation("余额充值（1~2000元，模拟支付直接到账）")
    @Idempotent(value = "profile:recharge", timeout = 30, unit = TimeUnit.SECONDS)
    @RateLimiter(value = "profile:recharge", permits = 5, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.USER)
    public Result<BigDecimal> recharge(
            @CurrentUser LoginUser user,
            @Valid @RequestBody RechargeDTO dto) {
        return Result.ok(profileService.recharge(user.getId(), dto));
    }

    /**
     * 交易记录 —— FR-UC-048（充值/消费/退款流水）
     */
    @GetMapping("/balance-logs")
    @ApiOperation("余额流水分页")
    public Result<PageResult<BalanceLogVO>> balanceLogs(
            @CurrentUser LoginUser user,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Long page,
            @ApiParam(value = "每页条数", defaultValue = "15") @RequestParam(defaultValue = "15") Long size) {
        return Result.ok(profileService.pageBalanceLogs(user.getId(), page, size));
    }

    /**
     * 学习统计 —— FR-UC-049
     */
    @GetMapping("/study-stats")
    @ApiOperation("学习统计（累计+时段分布柱状图+90天热力图）")
    public Result<StudyStatVO> studyStats(@CurrentUser LoginUser user) {
        return Result.ok(profileService.studyStats(user.getId()));
    }

    /**
     * 同意隐私协议 —— FR-UC-050（首次启动弹窗）
     */
    @PostMapping("/privacy-agree")
    @ApiOperation("同意隐私协议")
    public Result<Void> privacyAgree(@CurrentUser LoginUser user) {
        profileService.agreePrivacy(user.getId());
        return Result.ok();
    }

    /**
     * 意见反馈 —— FR-UC-052
     */
    @PostMapping("/feedback")
    @ApiOperation("提交意见反馈")
    @Idempotent(value = "profile:feedback", timeout = 30, unit = TimeUnit.SECONDS)
    @RateLimiter(value = "profile:feedback", permits = 3, interval = 10,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.USER)
    public Result<Long> feedback(
            @CurrentUser LoginUser user,
            @Valid @RequestBody FeedbackCreateDTO dto) {
        return Result.ok(profileService.submitFeedback(user.getId(), dto));
    }
}
