package com.studyroom.service.statistics;

import com.studyroom.entity.vo.statistics.DashboardSummaryVO;
import com.studyroom.entity.vo.statistics.DashboardTrendVO;
import com.studyroom.entity.vo.statistics.SeatStatusStatsVO;
import com.studyroom.entity.vo.statistics.SeatTopVO;
import com.studyroom.mapper.statistics.DashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计 Service 实现 —— FR-AD-002 / FR-AD-003
 *
 * <h3>实现要点</h3>
 * <ul>
 *   <li>纯读聚合查询，无写操作，不加锁；单店规模下全部走索引聚合，无性能压力</li>
 *   <li>趋势图横轴由后端按日期序列补 0，保证 7/30 天连续（前端免补洞）</li>
 *   <li>上座率实时快照：使用中 ÷（总数 - 维护中），避免分母含不可用座位失真</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 趋势图窗口：7 天 */
    private static final int TREND_DAYS_SHORT = 7;

    /** 趋势图窗口：30 天 */
    private static final int TREND_DAYS_LONG = 30;

    /** 座位状态：使用中（FR-UC-017） */
    private static final int SEAT_STATUS_IN_USE = 1;

    /** 座位状态：维护中（不计入上座率分母） */
    private static final int SEAT_STATUS_MAINTENANCE = 2;

    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardSummaryVO getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        // ---- 座位状态快照（上座率）----
        Map<Integer, Long> seatStatusMap = new HashMap<>(8);
        long seatTotal = 0L;
        for (Map<String, Object> row : dashboardMapper.countSeatByStatus()) {
            int status = ((Number) row.get("status")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            seatStatusMap.put(status, cnt);
            seatTotal += cnt;
        }
        long seatInUse = seatStatusMap.getOrDefault(SEAT_STATUS_IN_USE, 0L);
        long seatUsable = seatTotal - seatStatusMap.getOrDefault(SEAT_STATUS_MAINTENANCE, 0L);
        BigDecimal occupancyRate = seatUsable > 0
                ? BigDecimal.valueOf(seatInUse).divide(BigDecimal.valueOf(seatUsable), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setTodayOrders(dashboardMapper.countOrdersCreatedBetween(todayStart, tomorrowStart));
        vo.setYesterdayOrders(dashboardMapper.countOrdersCreatedBetween(yesterdayStart, todayStart));
        vo.setTodayRevenue(dashboardMapper.sumRevenueBetween(todayStart, tomorrowStart));
        vo.setYesterdayRevenue(dashboardMapper.sumRevenueBetween(yesterdayStart, todayStart));
        vo.setActiveUsers(dashboardMapper.countActiveUsersBetween(todayStart, tomorrowStart));
        vo.setYesterdayActiveUsers(dashboardMapper.countActiveUsersBetween(yesterdayStart, todayStart));
        vo.setSeatTotal(seatTotal);
        vo.setSeatInUse(seatInUse);
        vo.setSeatUsable(seatUsable);
        vo.setOccupancyRate(occupancyRate);
        return vo;
    }

    @Override
    public List<DashboardTrendVO> getTrend(int days) {
        // 窗口校验：仅允许 7 / 30，其余回退 7（FR-AD-003）
        if (days != TREND_DAYS_LONG) {
            days = TREND_DAYS_SHORT;
        }
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(days - 1L);
        LocalDateTime since = firstDay.atStartOfDay();

        // 预约量按创建日、营收按支付日，两份聚合结果各自转 Map 后按日期序列合并补 0
        Map<String, Long> orderMap = new HashMap<>(days * 2);
        for (Map<String, Object> row : dashboardMapper.countOrdersDaily(since)) {
            orderMap.put(String.valueOf(row.get("date")), ((Number) row.get("cnt")).longValue());
        }
        Map<String, BigDecimal> revenueMap = new HashMap<>(days * 2);
        for (Map<String, Object> row : dashboardMapper.sumRevenueDaily(since)) {
            revenueMap.put(String.valueOf(row.get("date")), new BigDecimal(String.valueOf(row.get("amount"))));
        }

        List<DashboardTrendVO> list = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            String date = firstDay.plusDays(i).format(DATE_FMT);
            DashboardTrendVO vo = new DashboardTrendVO();
            vo.setDate(date);
            vo.setOrderCount(orderMap.getOrDefault(date, 0L));
            vo.setRevenue(revenueMap.getOrDefault(date, BigDecimal.ZERO));
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<SeatStatusStatsVO> getSeatStatus() {
        List<SeatStatusStatsVO> list = new ArrayList<>(5);
        for (Map<String, Object> row : dashboardMapper.countSeatByStatus()) {
            SeatStatusStatsVO vo = new SeatStatusStatsVO();
            vo.setStatus(((Number) row.get("status")).intValue());
            vo.setCount(((Number) row.get("cnt")).longValue());
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<SeatTopVO> getSeatTop(int days, int limit) {
        // 窗口与条数防御：days ∈ [1, 90]，limit ∈ [1, 20]
        if (days < 1 || days > 90) {
            days = 30;
        }
        if (limit < 1 || limit > 20) {
            limit = 10;
        }
        LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();

        List<SeatTopVO> list = new ArrayList<>(limit);
        for (Map<String, Object> row : dashboardMapper.selectSeatTop(since, limit)) {
            SeatTopVO vo = new SeatTopVO();
            vo.setSeatId(Long.valueOf(String.valueOf(row.get("seatId"))));
            vo.setSeatNo((String) row.get("seatNo"));
            vo.setTypeName(row.get("typeName") == null ? "-" : String.valueOf(row.get("typeName")));
            vo.setOrderCount(Long.valueOf(String.valueOf(row.get("orderCount"))));
            // 分钟 → 小时，保留 1 位小数
            long usedMinutes = Long.parseLong(String.valueOf(row.get("usedMinutes")));
            vo.setUsedHours(BigDecimal.valueOf(usedMinutes)
                    .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP));
            vo.setRevenue(new BigDecimal(String.valueOf(row.get("revenue"))));
            list.add(vo);
        }
        return list;
    }
}
