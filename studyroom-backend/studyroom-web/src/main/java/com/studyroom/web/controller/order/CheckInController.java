package com.studyroom.web.controller.order;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.Idempotent;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.order.AppealCreateDTO;
import com.studyroom.entity.dto.order.CheckInDTO;
import com.studyroom.entity.vo.order.CheckInResultVO;
import com.studyroom.service.sign.CheckInService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * 用户端-自助签到 Controller（FR-UC-029 ~ FR-UC-032）
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>POST</td><td>/api/sign/checkin</td><td>扫码签到（五重校验）</td><td>FR-UC-029/030/031</td></tr>
 *   <tr><td>POST</td><td>/api/sign/appeal</td><td>提交签到申诉（原因+文字+图片）</td><td>FR-UC-032</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/sign")
@RequiredArgsConstructor
@Api(tags = "用户端-自助签到")
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 扫码签到 —— FR-UC-029/030/031
     * <p>五重校验：①AES解密 ②HMAC验签 ③查有效订单 ④时间窗口(±15min) ⑤GPS距离(≤100m)；
     * 成功订单转进行中，失败返回原因（appealable=true）。</p>
     * <p>限流 3 次/10 秒防爆破（FR-SY-001）；幂等 Token 防重复提交。</p>
     */
    @PostMapping("/checkin")
    @ApiOperation("扫码签到（五重校验）")
    @Idempotent(value = "sign:checkin", timeout = 30, unit = TimeUnit.SECONDS)
    @RateLimiter(value = "sign:checkin", permits = 3, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<CheckInResultVO> checkIn(
            @ApiParam(value = "签到码 + 当前GPS坐标", required = true) @Valid @RequestBody CheckInDTO dto,
            @ApiParam(hidden = true) @RequestHeader(value = "X-Idempotent-Token", required = false) String idempotentToken,
            @CurrentUser LoginUser user) {
        return Result.ok(checkInService.checkIn(user.getId(), dto));
    }

    /**
     * 提交签到申诉 —— FR-UC-032
     * <p>凭证图片先经 POST /api/file/upload（scene=feedback）上传取回 URL。</p>
     */
    @PostMapping("/appeal")
    @ApiOperation("提交签到申诉（原因+文字说明+凭证图片）")
    @RateLimiter(value = "sign:appeal", permits = 3, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<Long> submitAppeal(
            @ApiParam(value = "申诉内容", required = true) @Valid @RequestBody AppealCreateDTO dto,
            @CurrentUser LoginUser user) {
        return Result.ok(checkInService.submitAppeal(user.getId(), dto));
    }
}
