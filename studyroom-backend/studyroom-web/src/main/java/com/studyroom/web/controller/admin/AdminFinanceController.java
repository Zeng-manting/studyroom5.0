package com.studyroom.web.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.admin.AdminRefundRecordVO;
import com.studyroom.entity.vo.statistics.FinanceSummaryVO;
import com.studyroom.service.statistics.AdminFinanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 管理端-财务管理 Controller —— FR-AD-024
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/finance/summary</td><td>财务总览（总营收/退款/净营收/客单价 + 按日趋势）</td><td>FR-AD-024</td></tr>
 *   <tr><td>GET</td><td>/api/admin/finance/refund/page</td><td>退款记录分页</td><td>FR-AD-024</td></tr>
 *   <tr><td>GET</td><td>/api/admin/finance/refund/export</td><td>退款记录 Excel 导出</td><td>FR-AD-024</td></tr>
 * </table>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "11.管理端-财务管理")
public class AdminFinanceController {

    private final AdminFinanceService adminFinanceService;

    @GetMapping("/summary")
    @ApiOperation("财务总览（时间范围内汇总 + 按日趋势）")
    public Result<FinanceSummaryVO> summary(
            @ApiParam(value = "开始日期 yyyy-MM-dd", required = true) @RequestParam String beginDate,
            @ApiParam(value = "结束日期 yyyy-MM-dd", required = true) @RequestParam String endDate) {
        return Result.ok(adminFinanceService.summary(beginDate, endDate));
    }

    @GetMapping("/refund/page")
    @ApiOperation("退款记录分页（类型/时间/订单号筛选）")
    public Result<PageResult<AdminRefundRecordVO>> refundPage(
            @RequestParam String beginDate,
            @RequestParam String endDate,
            @ApiParam("退款类型：1阶梯取消 2爽约 3申诉 4提前离场 5手动（空=全部）")
            @RequestParam(required = false) Integer type,
            @ApiParam("订单号（模糊）") @RequestParam(required = false) String keyword,
            @ApiParam("页码，默认 1") @RequestParam(defaultValue = "1") long page,
            @ApiParam("每页条数，默认 20") @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminFinanceService.pageRefunds(beginDate, endDate, type,
                keyword, page, Math.min(size, 100)));
    }

    @GetMapping("/refund/export")
    @ApiOperation("退款记录 Excel 导出（与列表同条件）")
    public void refundExport(
            @RequestParam String beginDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) throws IOException {
        List<AdminRefundRecordVO> list = adminFinanceService.listRefundsForExport(
                beginDate, endDate, type, keyword);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(
                "退款记录_" + beginDate + "_" + endDate, "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<List<String>> head = new ArrayList<>();
        for (String h : Arrays.asList("退款时间", "订单号", "用户", "退款类型",
                "退款金额(元)", "退款原因")) {
            head.add(Collections.singletonList(h));
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<List<Object>> data = new ArrayList<>();
        for (AdminRefundRecordVO r : list) {
            data.add(Arrays.asList(
                    r.getCreateTime() == null ? "" : r.getCreateTime().format(fmt),
                    r.getOrderNo(),
                    r.getNickname(),
                    r.getTypeDesc(),
                    r.getAmount(),
                    r.getReason() == null ? "" : r.getReason()));
        }
        EasyExcel.write(response.getOutputStream()).head(head).sheet("退款记录").doWrite(data);
        log.info("[AdminFinance] FR-AD-024 退款导出: {} ~ {}, rows={}", beginDate, endDate, data.size());
    }
}
