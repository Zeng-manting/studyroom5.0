package com.studyroom.web.controller.admin;

import com.alibaba.excel.EasyExcel;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.statistics.DateTrendVO;
import com.studyroom.entity.vo.statistics.NameValueVO;
import com.studyroom.service.statistics.AdminStatisticsService;
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
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 管理端-数据统计 Controller —— FR-AD-019 ~ FR-AD-023
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/booking/hours</td><td>预约时段分布（24h）</td><td>FR-AD-019</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/booking/packages</td><td>套餐占比饼图</td><td>FR-AD-019</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/seat/occupancy-trend</td><td>上座率趋势</td><td>FR-AD-020</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/seat/type-distribution</td><td>座位类型占用分布</td><td>FR-AD-020</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/revenue/composition</td><td>收入构成（按套餐）</td><td>FR-AD-021</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/revenue/refund-trend</td><td>退款趋势</td><td>FR-AD-021</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/revenue/avg-order-value</td><td>客单价趋势</td><td>FR-AD-021</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/user/active-trend</td><td>DAU + 每日新增</td><td>FR-AD-022</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/user/duration-histogram</td><td>学习时长分布直方图</td><td>FR-AD-022</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/user/retention</td><td>注册队列留存率</td><td>FR-AD-022</td></tr>
 *   <tr><td>GET</td><td>/api/admin/stats/export</td><td>统计报表 Excel 导出</td><td>FR-AD-023</td></tr>
 * </table>
 *
 * <p>统一参数 beginDate/endDate（yyyy-MM-dd 含当日全天）；跨度上限 92 天。</p>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "10.管理端-数据统计")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    /* ==================== FR-AD-019 预约统计 ==================== */

    @GetMapping("/booking/hours")
    @ApiOperation("预约时段分布（24 小时）")
    public Result<List<NameValueVO>> bookingHours(
            @ApiParam(value = "开始日期 yyyy-MM-dd", required = true) @RequestParam String beginDate,
            @ApiParam(value = "结束日期 yyyy-MM-dd", required = true) @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.bookingHourDistribution(beginDate, endDate));
    }

    @GetMapping("/booking/packages")
    @ApiOperation("套餐占比（饼图）")
    public Result<List<NameValueVO>> packageShare(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.packageShare(beginDate, endDate));
    }

    /* ==================== FR-AD-020 座位统计 ==================== */

    @GetMapping("/seat/occupancy-trend")
    @ApiOperation("上座率趋势（每日%）")
    public Result<DateTrendVO> occupancyTrend(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.occupancyTrend(beginDate, endDate));
    }

    @GetMapping("/seat/type-distribution")
    @ApiOperation("座位类型占用分布")
    public Result<List<NameValueVO>> seatTypeDistribution(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.seatTypeDistribution(beginDate, endDate));
    }

    /* ==================== FR-AD-021 营收统计 ==================== */

    @GetMapping("/revenue/composition")
    @ApiOperation("收入构成（按套餐）")
    public Result<List<NameValueVO>> revenueComposition(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.revenueComposition(beginDate, endDate));
    }

    @GetMapping("/revenue/refund-trend")
    @ApiOperation("退款趋势（金额 + 笔数）")
    public Result<DateTrendVO> refundTrend(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.refundTrend(beginDate, endDate));
    }

    @GetMapping("/revenue/avg-order-value")
    @ApiOperation("客单价趋势（客单价 + 订单量）")
    public Result<DateTrendVO> avgOrderValue(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.avgOrderValueTrend(beginDate, endDate));
    }

    /* ==================== FR-AD-022 用户活跃度 ==================== */

    @GetMapping("/user/active-trend")
    @ApiOperation("用户活跃趋势（DAU + 每日新增）")
    public Result<DateTrendVO> userActiveTrend(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.userActiveTrend(beginDate, endDate));
    }

    @GetMapping("/user/duration-histogram")
    @ApiOperation("学习时长分布直方图")
    public Result<List<NameValueVO>> durationHistogram(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.durationHistogram(beginDate, endDate));
    }

    @GetMapping("/user/retention")
    @ApiOperation("注册队列留存率（次日~7日）")
    public Result<List<Map<String, Object>>> retention(
            @RequestParam String beginDate, @RequestParam String endDate) {
        return Result.ok(adminStatisticsService.retentionCohorts(beginDate, endDate));
    }

    /* ==================== FR-AD-023 报表导出 ==================== */

    @GetMapping("/export")
    @ApiOperation("统计报表 Excel 导出（逐日预约/营收/退款/客单价/DAU）")
    public void export(
            @ApiParam(value = "开始日期 yyyy-MM-dd", required = true) @RequestParam String beginDate,
            @ApiParam(value = "结束日期 yyyy-MM-dd", required = true) @RequestParam String endDate,
            HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows = adminStatisticsService.dailyReportRows(beginDate, endDate);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(
                "经营统计_" + beginDate + "_" + endDate, "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<List<String>> head = new ArrayList<>();
        for (String h : Arrays.asList("日期", "订单量", "营收(元)", "退款(元)", "退款笔数",
                "净营收(元)", "客单价(元)", "活跃用户")) {
            head.add(Collections.singletonList(h));
        }
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            data.add(Arrays.asList(
                    r.get("date"),
                    r.get("orderCount"),
                    r.get("revenue"),
                    r.get("refund"),
                    r.get("refundCount"),
                    r.get("net"),
                    r.get("avgOrderAmount"),
                    r.get("dau")));
        }
        EasyExcel.write(response.getOutputStream()).head(head).sheet("经营统计").doWrite(data);
        log.info("[AdminStats] FR-AD-023 统计报表导出: {} ~ {}, rows={}", beginDate, endDate, data.size());
    }
}
