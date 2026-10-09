package com.studyroom.web.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.admin.AdminRefundDTO;
import com.studyroom.entity.dto.admin.AppealHandleDTO;
import com.studyroom.entity.vo.admin.AdminAppealListVO;
import com.studyroom.entity.vo.admin.AdminOrderListVO;
import com.studyroom.entity.vo.admin.AdminRefundPreviewVO;
import com.studyroom.service.admin.AdminOrderService;
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

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 管理端-订单管理 Controller —— FR-AD-010 ~ FR-AD-013
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/order/page</td><td>订单列表：时间/状态/座位号/用户多条件筛选，分页默认 20</td><td>FR-AD-010</td></tr>
 *   <tr><td>GET</td><td>/api/admin/order/export</td><td>订单 Excel 导出（与列表同条件，上限 1 万行）</td><td>FR-AD-013</td></tr>
 *   <tr><td>GET</td><td>/api/admin/order/appeal/page</td><td>申诉列表（状态筛选 + 分页）</td><td>FR-AD-011</td></tr>
 *   <tr><td>GET</td><td>/api/admin/order/appeal/{id}</td><td>申诉详情（含凭证图片）</td><td>FR-AD-011</td></tr>
 *   <tr><td>POST</td><td>/api/admin/order/appeal/{id}/approve</td><td>申诉处理：标记签到成功</td><td>FR-AD-011</td></tr>
 *   <tr><td>POST</td><td>/api/admin/order/appeal/{id}/reject</td><td>申诉处理：驳回（处理意见必填）</td><td>FR-AD-011</td></tr>
 *   <tr><td>GET</td><td>/api/admin/order/{id}/refund-preview</td><td>退款预览：系统建议退款金额</td><td>FR-AD-012</td></tr>
 *   <tr><td>POST</td><td>/api/admin/order/{id}/refund</td><td>退款审核通过（金额可调整）</td><td>FR-AD-012</td></tr>
 * </table>
 *
 * <p>全部接口需 ADMIN 角色；操作人取自 JWT（{@link AuthContextHolder}）。</p>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "5.管理端-订单管理")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /* ==================== FR-AD-010：订单列表 ==================== */

    @GetMapping("/page")
    @ApiOperation("订单列表（多条件筛选，分页 20 条）")
    public Result<PageResult<AdminOrderListVO>> page(
            @ApiParam("开始日期 yyyy-MM-dd（按下单时间）") @RequestParam(required = false) String beginDate,
            @ApiParam("结束日期 yyyy-MM-dd（按下单时间）") @RequestParam(required = false) String endDate,
            @ApiParam("订单状态：0待支付 1已预约 2进行中 3已完成 4已取消 5退款中 6已退款 7申诉中")
            @RequestParam(required = false) Integer status,
            @ApiParam("座位号（模糊）") @RequestParam(required = false) String seatNo,
            @ApiParam("用户ID") @RequestParam(required = false) Long userId,
            @ApiParam("关键字：订单号/用户昵称（模糊）") @RequestParam(required = false) String keyword,
            @ApiParam("页码，默认 1") @RequestParam(defaultValue = "1") long page,
            @ApiParam("每页条数，默认 20，上限 100") @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminOrderService.pageOrders(beginDate, endDate, status, seatNo,
                userId, keyword, page, Math.min(size, 100)));
    }

    /* ==================== FR-AD-013：订单导出 ==================== */

    @GetMapping("/export")
    @ApiOperation("订单 Excel 导出（与列表同筛选条件）")
    public void export(
            @ApiParam("开始日期 yyyy-MM-dd") @RequestParam(required = false) String beginDate,
            @ApiParam("结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @ApiParam("订单状态") @RequestParam(required = false) Integer status,
            @ApiParam("座位号（模糊）") @RequestParam(required = false) String seatNo,
            @ApiParam("用户ID") @RequestParam(required = false) Long userId,
            @ApiParam("关键字：订单号/用户昵称（模糊）") @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws IOException {
        List<AdminOrderListVO> list = adminOrderService.listForExport(
                beginDate, endDate, status, seatNo, userId, keyword);

        // 动态表头导出（无需注解 DTO，EasyExcel 直接写 List<List<Object>>）
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(
                "订单导出_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<List<String>> head = new ArrayList<>();
        for (String h : Arrays.asList("订单号", "用户", "门店", "座位号", "套餐",
                "开始时间", "结束时间", "实付金额(元)", "退款金额(元)", "状态",
                "支付方式", "是否爽约", "下单时间")) {
            head.add(Collections.singletonList(h));
        }

        List<List<Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (AdminOrderListVO o : list) {
            rows.add(Arrays.asList(
                    o.getOrderNo(),
                    o.getNickname(),
                    o.getStoreName(),
                    o.getSeatNo(),
                    o.getPackageName(),
                    o.getStartTime() == null ? "" : o.getStartTime().format(fmt),
                    o.getEndTime() == null ? "" : o.getEndTime().format(fmt),
                    o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount(),
                    o.getRefundAmount() == null ? BigDecimal.ZERO : o.getRefundAmount(),
                    o.getStatusDesc(),
                    o.getPayTypeDesc(),
                    Boolean.TRUE.equals(o.getNoShow()) ? "是" : "否",
                    o.getCreateTime() == null ? "" : o.getCreateTime().format(fmt)));
        }
        EasyExcel.write(response.getOutputStream()).head(head).sheet("订单").doWrite(rows);
        log.info("[AdminOrder] FR-AD-013 订单导出: rows={}", rows.size());
    }

    /* ==================== FR-AD-011：申诉处理 ==================== */

    @GetMapping("/appeal/page")
    @ApiOperation("申诉列表（状态筛选 + 分页）")
    public Result<PageResult<AdminAppealListVO>> appealPage(
            @ApiParam("申诉状态：0待审 1通过 2驳回（空=全部）") @RequestParam(required = false) Integer status,
            @ApiParam("页码，默认 1") @RequestParam(defaultValue = "1") long page,
            @ApiParam("每页条数，默认 20") @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminOrderService.pageAppeals(status, page, Math.min(size, 100)));
    }

    @GetMapping("/appeal/{appealId}")
    @ApiOperation("申诉详情（含凭证图片）")
    public Result<AdminAppealListVO> appealDetail(@PathVariable Long appealId) {
        return Result.ok(adminOrderService.getAppealDetail(appealId));
    }

    @PostMapping("/appeal/{appealId}/approve")
    @ApiOperation("申诉处理：标记签到成功")
    public Result<Void> approveAppeal(@PathVariable Long appealId,
                                      @RequestBody(required = false) AppealHandleDTO dto) {
        adminOrderService.approveAppeal(appealId, dto, AuthContextHolder.require().getId());
        return Result.ok();
    }

    @PostMapping("/appeal/{appealId}/reject")
    @ApiOperation("申诉处理：驳回（处理意见必填）")
    public Result<Void> rejectAppeal(@PathVariable Long appealId,
                                     @RequestBody @Valid AppealHandleDTO dto) {
        adminOrderService.rejectAppeal(appealId, dto, AuthContextHolder.require().getId());
        return Result.ok();
    }

    /* ==================== FR-AD-012：退款审核 ==================== */

    @GetMapping("/{orderId}/refund-preview")
    @ApiOperation("退款预览：系统自动计算建议退款金额")
    public Result<AdminRefundPreviewVO> refundPreview(@PathVariable Long orderId) {
        return Result.ok(adminOrderService.previewRefund(orderId));
    }

    @PostMapping("/{orderId}/refund")
    @ApiOperation("退款审核通过（金额可在建议值上手动调整）")
    public Result<Void> refund(@PathVariable Long orderId,
                               @RequestBody @Valid AdminRefundDTO dto) {
        adminOrderService.manualRefund(orderId, dto, AuthContextHolder.require().getId());
        return Result.ok();
    }
}
