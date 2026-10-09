package com.studyroom.web.controller.admin;

import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.statistics.DashboardSummaryVO;
import com.studyroom.entity.vo.statistics.DashboardTrendVO;
import com.studyroom.entity.vo.statistics.SeatStatusStatsVO;
import com.studyroom.entity.vo.statistics.SeatTopVO;
import com.studyroom.service.statistics.DashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-仪表盘 Controller —— FR-AD-002 / FR-AD-003
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/dashboard/summary</td><td>核心指标卡片（今日预约量/营收/上座率/活跃用户 + 昨日环比）</td><td>FR-AD-002</td></tr>
 *   <tr><td>GET</td><td>/api/admin/dashboard/trend?days=7|30</td><td>预约量与营收趋势折线图数据</td><td>FR-AD-003</td></tr>
 *   <tr><td>GET</td><td>/api/admin/dashboard/seat-status</td><td>座位状态总览（六色状态实时快照）</td><td>FR-AD-003</td></tr>
 *   <tr><td>GET</td><td>/api/admin/dashboard/seat-top?days=30&amp;limit=10</td><td>座位利用率 Top10 排行</td><td>FR-AD-003</td></tr>
 * </table>
 *
 * <p>全部只读接口，需 ADMIN 角色（{@code @PreAuthorize}，FR-AD-001 角色体系）。
 * 统计口径详见 {@link DashboardService} 各方法注释。</p>
 *
 * @author studyroom
 */
@Slf4j
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "4.管理端-仪表盘")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 核心指标卡片 —— FR-AD-002
     * <p>今日预约量 / 营收 / 上座率 / 活跃用户，各指标附昨日值供前端计算环比。</p>
     */
    @GetMapping("/summary")
    @ApiOperation("核心指标卡片")
    public Result<DashboardSummaryVO> summary() {
        return Result.ok(dashboardService.getSummary());
    }

    /**
     * 趋势折线图数据 —— FR-AD-003
     * <p>days 仅支持 7 / 30（默认 7），返回按日期升序的预约量 + 营收双序列。</p>
     */
    @GetMapping("/trend")
    @ApiOperation("预约量与营收趋势（7天/30天）")
    public Result<List<DashboardTrendVO>> trend(
            @ApiParam(value = "窗口天数：7 或 30，默认 7", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardService.getTrend(days));
    }

    /**
     * 座位状态总览 —— FR-AD-003
     * <p>座位表实时快照：0空闲 1使用中 2维护 3打扫中 4锁定。</p>
     */
    @GetMapping("/seat-status")
    @ApiOperation("座位状态总览")
    public Result<List<SeatStatusStatsVO>> seatStatus() {
        return Result.ok(dashboardService.getSeatStatus());
    }

    /**
     * 座位利用率 Top10 —— FR-AD-003
     * <p>按窗口内累计使用时长降序；days 默认 30，limit 默认 10（上限 20）。</p>
     */
    @GetMapping("/seat-top")
    @ApiOperation("座位利用率排行 Top10")
    public Result<List<SeatTopVO>> seatTop(
            @ApiParam(value = "统计窗口天数，默认 30", example = "30")
            @RequestParam(defaultValue = "30") int days,
            @ApiParam(value = "取前 N 名，默认 10，上限 20", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.getSeatTop(days, limit));
    }
}
