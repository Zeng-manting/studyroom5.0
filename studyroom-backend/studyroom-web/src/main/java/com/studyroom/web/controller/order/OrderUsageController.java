package com.studyroom.web.controller.order;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.order.OrderUsageVO;
import com.studyroom.service.order.OrderUsageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 用户端-订单使用中 Controller（FR-UC-031 / FR-UC-033 / FR-UC-034）
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/order/{id}/usage</td><td>使用中/已完成状态（剩余时间、暂停态）</td><td>FR-UC-031</td></tr>
 *   <tr><td>POST</td><td>/api/order/{id}/pause</td><td>中途临时离座（暂停计时，保留30分钟）</td><td>FR-UC-033</td></tr>
 *   <tr><td>POST</td><td>/api/order/{id}/resume</td><td>回座恢复使用</td><td>FR-UC-033</td></tr>
 *   <tr><td>POST</td><td>/api/order/{id}/end</td><td>离场结束使用（实际时长结算+退差额）</td><td>FR-UC-034</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Api(tags = "用户端-订单使用中")
public class OrderUsageController {

    private final OrderUsageService orderUsageService;

    /**
     * 使用中状态 —— FR-UC-031 使用中页数据源
     */
    @GetMapping("/{id}/usage")
    @ApiOperation("订单使用状态（剩余时间/暂停状态/结算结果）")
    public Result<OrderUsageVO> getUsage(
            @ApiParam(value = "订单ID", required = true) @PathVariable("id") Long orderId,
            @CurrentUser LoginUser user) {
        return Result.ok(orderUsageService.getUsage(user.getId(), orderId));
    }

    /**
     * 中途临时离座 —— FR-UC-033
     * <p>暂停计时 + 座位保留 30 分钟，超时未回自动结算。</p>
     */
    @PostMapping("/{id}/pause")
    @ApiOperation("暂停计时（临时离座，保留30分钟）")
    @RateLimiter(value = "order:pause", permits = 5, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<OrderUsageVO> pause(
            @ApiParam(value = "订单ID", required = true) @PathVariable("id") Long orderId,
            @CurrentUser LoginUser user) {
        return Result.ok(orderUsageService.pause(user.getId(), orderId));
    }

    /**
     * 回座恢复 —— FR-UC-033
     */
    @PostMapping("/{id}/resume")
    @ApiOperation("恢复使用（回座）")
    @RateLimiter(value = "order:resume", permits = 5, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<OrderUsageVO> resume(
            @ApiParam(value = "订单ID", required = true) @PathVariable("id") Long orderId,
            @CurrentUser LoginUser user) {
        return Result.ok(orderUsageService.resume(user.getId(), orderId));
    }

    /**
     * 离场结束使用 —— FR-UC-034
     * <p>实际时长 = 签到→结束 − 暂停分钟；未使用部分按比例退差额至余额；座位转打扫中。</p>
     */
    @PostMapping("/{id}/end")
    @ApiOperation("结束使用（实际时长结算+按比例退差额）")
    @RateLimiter(value = "order:end", permits = 5, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<OrderUsageVO> end(
            @ApiParam(value = "订单ID", required = true) @PathVariable("id") Long orderId,
            @CurrentUser LoginUser user) {
        return Result.ok(orderUsageService.end(user.getId(), orderId));
    }
}
