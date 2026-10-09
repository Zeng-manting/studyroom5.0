package com.studyroom.web.controller.order;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.Idempotent;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.order.CreateOrderDTO;
import com.studyroom.entity.dto.order.OrderExtendDTO;
import com.studyroom.entity.dto.order.PayOrderDTO;
import com.studyroom.entity.vo.order.OrderCreateVO;
import com.studyroom.entity.vo.order.OrderDetailVO;
import com.studyroom.entity.vo.order.OrderExtendPreviewVO;
import com.studyroom.entity.vo.order.OrderListVO;
import com.studyroom.entity.vo.order.OrderPreviewVO;
import com.studyroom.entity.vo.order.OrderUsageVO;
import com.studyroom.entity.vo.order.UsableCouponVO;
import com.studyroom.service.order.OrderExtendService;
import com.studyroom.service.order.OrderQueryService;
import com.studyroom.service.order.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户端-订单 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/order/preview</td><td>订单费用预览（原价-优惠-实付）</td><td>FR-UC-022</td></tr>
 *   <tr><td>GET</td><td>/api/order/coupons</td><td>可用优惠券列表</td><td>FR-UC-024</td></tr>
 *   <tr><td>POST</td><td>/api/order/create</td><td>创建订单（原子锁座）</td><td>FR-UC-018/021/025</td></tr>
 *   <tr><td>POST</td><td>/api/order/{id}/pay</td><td>订单支付（余额/模拟微信）</td><td>FR-UC-023</td></tr>
 *   <tr><td>POST</td><td>/api/order/{id}/cancel</td><td>取消订单（阶梯退款）</td><td>FR-UC-027</td></tr>
 *   <tr><td>GET</td><td>/api/order/{id}</td><td>订单详情</td><td>FR-UC-022/028</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Api(tags = "用户端-订单")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final OrderExtendService orderExtendService;

    /**
     * 订单分页列表 —— FR-UC-035 六 Tab / FR-UC-036 倒计时字段 / FR-UC-037 动态操作按钮
     */
    @GetMapping("/list")
    @ApiOperation("订单列表（6 Tab：0全部 1待支付 2已预约 3进行中 4已完成 5已取消）")
    public Result<PageResult<OrderListVO>> list(
            @CurrentUser LoginUser user,
            @ApiParam(value = "Tab：0全部 1待支付 2已预约 3进行中 4已完成 5已取消", defaultValue = "0")
            @RequestParam(defaultValue = "0") Integer tab,
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Long page,
            @ApiParam(value = "每页条数", defaultValue = "10") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(orderQueryService.pageOrders(user.getId(), tab, page, size));
    }

    /**
     * 订单费用预览 —— FR-UC-022 订单确认页
     */
    @GetMapping("/preview")
    @ApiOperation("订单费用预览（座位/时段/费用明细）")
    public Result<OrderPreviewVO> preview(
            @CurrentUser LoginUser user,
            @ApiParam(value = "门店ID", required = true) @RequestParam Long storeId,
            @ApiParam(value = "座位ID", required = true) @RequestParam Long seatId,
            @ApiParam(value = "套餐ID", required = true) @RequestParam Long packageId,
            @ApiParam(value = "开始时间 yyyy-MM-dd HH:mm", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime startTime,
            @ApiParam("优惠券领取记录ID（可空）") @RequestParam(required = false) Long couponClaimId) {
        return Result.ok(orderService.preview(user.getId(), storeId, seatId, packageId, startTime, couponClaimId));
    }

    /**
     * 可用优惠券列表 —— FR-UC-024
     */
    @GetMapping("/coupons")
    @ApiOperation("当前订单可用优惠券")
    public Result<List<UsableCouponVO>> coupons(
            @CurrentUser LoginUser user,
            @ApiParam(value = "门店ID", required = true) @RequestParam Long storeId,
            @ApiParam(value = "座位ID", required = true) @RequestParam Long seatId,
            @ApiParam(value = "套餐ID", required = true) @RequestParam Long packageId,
            @ApiParam(value = "开始时间 yyyy-MM-dd HH:mm", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime startTime) {
        return Result.ok(orderService.usableCoupons(user.getId(), storeId, seatId, packageId, startTime));
    }

    /**
     * 创建订单 —— FR-UC-018 下单原子锁座 / FR-UC-021 冲突校验 / FR-UC-025 支付超时任务投递
     * <p>幂等：Header X-Idempotent-Token（FR-SY-001）；限流：USER 3 次/10 秒。</p>
     */
    @PostMapping("/create")
    @ApiOperation("创建订单（Redisson 原子锁座）")
    @Idempotent(value = "order:create", timeout = 60, unit = TimeUnit.SECONDS)
    @RateLimiter(value = "order:create", permits = 3, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<OrderCreateVO> create(
            @CurrentUser LoginUser user,
            @RequestHeader(value = "X-Idempotent-Token", required = false) String idempotentToken,
            @Valid @RequestBody CreateOrderDTO dto) {
        return Result.ok(orderService.createOrder(user.getId(), dto, idempotentToken));
    }

    /**
     * 订单支付 —— FR-UC-023 支付方式（1余额优先 2模拟微信支付）
     * <p>支付成功投递"开始后 30 分钟未签到自动取消"任务（FR-UC-026）。</p>
     */
    @PostMapping("/{id}/pay")
    @ApiOperation("订单支付")
    @RateLimiter(value = "order:pay", permits = 3, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<OrderDetailVO> pay(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody PayOrderDTO dto) {
        return Result.ok(orderService.payOrder(user.getId(), orderId, dto));
    }

    /**
     * 取消订单 —— FR-UC-027 阶梯退款（>60min 全退 / 15~60min 退 90% / <15min 退 70%）
     */
    @PostMapping("/{id}/cancel")
    @ApiOperation("取消订单（阶梯退款）")
    @RateLimiter(value = "order:cancel", permits = 3, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<OrderDetailVO> cancel(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long orderId) {
        return Result.ok(orderService.cancelOrder(user.getId(), orderId));
    }

    /**
     * 订单详情 —— FR-UC-022 确认页 / FR-UC-028 支付成功页
     */
    @GetMapping("/{id}")
    @ApiOperation("订单详情")
    public Result<OrderDetailVO> detail(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long orderId) {
        return Result.ok(orderService.getOrderDetail(user.getId(), orderId));
    }

    /**
     * 续时费用预览 —— FR-UC-055 使用中订单按当前套餐单价折算
     */
    @GetMapping("/{id}/extend-preview")
    @ApiOperation("续时费用预览（折算单价 × 小时数 + 新结束时间）")
    public Result<OrderExtendPreviewVO> extendPreview(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long orderId,
            @ApiParam(value = "续时小时数（1~12）", required = true) @RequestParam Integer hours) {
        return Result.ok(orderExtendService.preview(user.getId(), orderId, hours));
    }

    /**
     * 进行中续时 —— FR-UC-055 支付后顺延结束时间并续占座位
     * <p>幂等：Header X-Idempotent-Token（FR-SY-001）；限流：USER 3 次/10 秒。</p>
     */
    @PostMapping("/{id}/extend")
    @ApiOperation("进行中续时（余额/模拟微信支付，顺延 end_time + extend_count+1）")
    @Idempotent(value = "order:extend", timeout = 60, unit = TimeUnit.SECONDS)
    @RateLimiter(value = "order:extend", permits = 3, interval = 10,
            unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)
    public Result<OrderUsageVO> extend(
            @CurrentUser LoginUser user,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody OrderExtendDTO dto) {
        return Result.ok(orderExtendService.extend(user.getId(), orderId, dto));
    }
}
