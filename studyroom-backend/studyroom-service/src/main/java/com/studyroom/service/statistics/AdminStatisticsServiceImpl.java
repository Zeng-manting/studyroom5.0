package com.studyroom.service.statistics;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.vo.statistics.DateTrendVO;
import com.studyroom.entity.vo.statistics.NameValueVO;
import com.studyroom.mapper.statistics.AdminStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 管理端数据统计服务实现 —— FR-AD-019 ~ FR-AD-023
 *
 * <h3>实现说明</h3>
 * <ul>
 *   <li>全部 SQL 落在 {@link AdminStatsMapper}，本类做日期补零、分桶与比率计算</li>
 *   <li>时间参数为日期字符串 yyyy-MM-dd，含起止当日全天；跨度上限 92 天防拖库</li>
 *   <li>上座率口径：当日订单使用分钟数合计 ÷（可用座位数 × 1440），与仪表盘一致</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 单次统计最大天数（防全表扫描拖库） */
    private static final int MAX_RANGE_DAYS = 92;
    /** 可用座位数（单店部署：全部未删除座位）——上座率分母 */
    private static final int AVAILABLE_SEATS_FALLBACK = 50;

    private final AdminStatsMapper statsMapper;
    private final com.studyroom.mapper.store.SeatMapper seatMapper;

    /* ==================== FR-AD-019 预约统计 ==================== */

    @Override
    public List<NameValueVO> bookingHourDistribution(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<Map<String, Object>> rows = statsMapper.bookingHourDistribution(range[0], range[1]);
        Map<Integer, Long> byHour = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byHour.put(((Number) row.get("hour")).intValue(), ((Number) row.get("cnt")).longValue());
        }
        // 24 小时补零
        List<NameValueVO> result = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            result.add(new NameValueVO(String.format("%02d:00", h), byHour.getOrDefault(h, 0L)));
        }
        return result;
    }

    @Override
    public List<NameValueVO> packageShare(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        return toNameValues(statsMapper.packageShare(range[0], range[1]), "未指定套餐");
    }

    /* ==================== FR-AD-020 座位统计 ==================== */

    @Override
    public DateTrendVO occupancyTrend(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<LocalDate> dates = dateSequence(range);

        // 分母：可用座位数 × 当日营业分钟（营业时间按 8:00-23:00 共 900 分钟，与门店营业时间一致口径简化）
        long seats = availableSeatCount();
        long capacityPerDay = seats * 900L;

        Map<LocalDate, Long> minutesMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyOccupiedMinutes(range[0], range[1])) {
            minutesMap.put(toDate(row.get("date")),
                    row.get("minutes") == null ? 0L : ((Number) row.get("minutes")).longValue());
        }

        DateTrendVO vo = new DateTrendVO();
        for (LocalDate d : dates) {
            vo.getDates().add(d.format(DATE));
            long minutes = minutesMap.getOrDefault(d, 0L);
            BigDecimal rate = capacityPerDay <= 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(minutes * 100.0 / capacityPerDay)
                    .setScale(2, RoundingMode.HALF_UP);
            vo.getPrimary().add(rate);
            vo.getSecondary().add(BigDecimal.valueOf(minutes));
        }
        return vo;
    }

    @Override
    public List<NameValueVO> seatTypeDistribution(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        return toNameValues(statsMapper.seatTypeDistribution(range[0], range[1]), "未知类型");
    }

    /* ==================== FR-AD-021 营收统计 ==================== */

    @Override
    public List<NameValueVO> revenueComposition(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        return toNameValues(statsMapper.revenueComposition(range[0], range[1]), "未关联套餐");
    }

    @Override
    public DateTrendVO refundTrend(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<LocalDate> dates = dateSequence(range);

        Map<LocalDate, BigDecimal> amountMap = new HashMap<>();
        Map<LocalDate, Long> cntMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyRefund(range[0], range[1])) {
            LocalDate d = toDate(row.get("date"));
            amountMap.put(d, toDecimal(row.get("amount")));
            cntMap.put(d, row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue());
        }

        DateTrendVO vo = new DateTrendVO();
        for (LocalDate d : dates) {
            vo.getDates().add(d.format(DATE));
            vo.getPrimary().add(amountMap.getOrDefault(d, BigDecimal.ZERO));
            vo.getSecondary().add(BigDecimal.valueOf(cntMap.getOrDefault(d, 0L)));
        }
        return vo;
    }

    @Override
    public DateTrendVO avgOrderValueTrend(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<LocalDate> dates = dateSequence(range);

        Map<LocalDate, BigDecimal> amountMap = new HashMap<>();
        Map<LocalDate, Long> cntMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyRevenue(range[0], range[1])) {
            LocalDate d = toDate(row.get("date"));
            amountMap.put(d, toDecimal(row.get("amount")));
            cntMap.put(d, row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue());
        }

        DateTrendVO vo = new DateTrendVO();
        for (LocalDate d : dates) {
            vo.getDates().add(d.format(DATE));
            BigDecimal amount = amountMap.getOrDefault(d, BigDecimal.ZERO);
            long cnt = cntMap.getOrDefault(d, 0L);
            BigDecimal avg = cnt == 0 ? BigDecimal.ZERO
                    : amount.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP);
            vo.getPrimary().add(avg);
            vo.getSecondary().add(BigDecimal.valueOf(cnt));
        }
        return vo;
    }

    /* ==================== FR-AD-022 用户活跃度 ==================== */

    @Override
    public DateTrendVO userActiveTrend(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<LocalDate> dates = dateSequence(range);

        Map<LocalDate, Long> dauMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyActiveUsers(range[0], range[1])) {
            dauMap.put(toDate(row.get("date")), ((Number) row.get("cnt")).longValue());
        }
        Map<LocalDate, Long> newMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyNewUsers(range[0], range[1])) {
            newMap.put(toDate(row.get("date")), ((Number) row.get("cnt")).longValue());
        }

        DateTrendVO vo = new DateTrendVO();
        for (LocalDate d : dates) {
            vo.getDates().add(d.format(DATE));
            vo.getPrimary().add(BigDecimal.valueOf(dauMap.getOrDefault(d, 0L)));
            vo.getSecondary().add(BigDecimal.valueOf(newMap.getOrDefault(d, 0L)));
        }
        return vo;
    }

    @Override
    public List<NameValueVO> durationHistogram(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<Map<String, Object>> rows = statsMapper.orderDurations(range[0], range[1]);

        // 分桶：0-1h / 1-2h / 2-4h / 4-8h / 8h+
        String[] buckets = {"0-1小时", "1-2小时", "2-4小时", "4-8小时", "8小时+"};
        long[] counts = new long[buckets.length];
        for (Map<String, Object> row : rows) {
            Object minutesObj = row.get("minutes");
            if (minutesObj == null) {
                continue;
            }
            long minutes = ((Number) minutesObj).longValue();
            if (minutes <= 60) counts[0]++;
            else if (minutes <= 120) counts[1]++;
            else if (minutes <= 240) counts[2]++;
            else if (minutes <= 480) counts[3]++;
            else counts[4]++;
        }
        List<NameValueVO> result = new ArrayList<>(buckets.length);
        for (int i = 0; i < buckets.length; i++) {
            result.add(new NameValueVO(buckets[i], counts[i]));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> retentionCohorts(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<Map<String, Object>> rows = statsMapper.retentionCohorts(range[0], range[1]);
        // 计算留存率百分比
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", String.valueOf(row.get("date")));
            long newCnt = row.get("new_cnt") == null ? 0L : ((Number) row.get("new_cnt")).longValue();
            long retained = row.get("retained_cnt") == null ? 0L
                    : ((Number) row.get("retained_cnt")).longValue();
            item.put("newCount", newCnt);
            item.put("retainedCount", retained);
            item.put("retentionRate", newCnt == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(retained * 100.0 / newCnt).setScale(2, RoundingMode.HALF_UP));
            result.add(item);
        }
        return result;
    }

    /* ==================== FR-AD-023 报表导出 ==================== */

    @Override
    public List<Map<String, Object>> dailyReportRows(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        List<LocalDate> dates = dateSequence(range);

        Map<LocalDate, BigDecimal> revenueMap = new HashMap<>();
        Map<LocalDate, Long> orderCntMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyRevenue(range[0], range[1])) {
            revenueMap.put(toDate(row.get("date")), toDecimal(row.get("amount")));
            orderCntMap.put(toDate(row.get("date")),
                    row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue());
        }
        Map<LocalDate, BigDecimal> refundMap = new HashMap<>();
        Map<LocalDate, Long> refundCntMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyRefund(range[0], range[1])) {
            refundMap.put(toDate(row.get("date")), toDecimal(row.get("amount")));
            refundCntMap.put(toDate(row.get("date")),
                    row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue());
        }
        Map<LocalDate, Long> dauMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyActiveUsers(range[0], range[1])) {
            dauMap.put(toDate(row.get("date")), ((Number) row.get("cnt")).longValue());
        }

        List<Map<String, Object>> rows = new ArrayList<>(dates.size());
        for (LocalDate d : dates) {
            Map<String, Object> item = new LinkedHashMap<>();
            BigDecimal revenue = revenueMap.getOrDefault(d, BigDecimal.ZERO);
            long cnt = orderCntMap.getOrDefault(d, 0L);
            item.put("date", d.format(DATE));
            item.put("orderCount", cnt);
            item.put("revenue", revenue);
            item.put("refund", refundMap.getOrDefault(d, BigDecimal.ZERO));
            item.put("refundCount", refundCntMap.getOrDefault(d, 0L));
            item.put("net", revenue.subtract(refundMap.getOrDefault(d, BigDecimal.ZERO)));
            item.put("avgOrderAmount", cnt == 0 ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP));
            item.put("dau", dauMap.getOrDefault(d, 0L));
            rows.add(item);
        }
        return rows;
    }

    /* ==================== 内部方法 ==================== */

    /** 日期字符串 → [begin, end) 时间对；校验跨度 */
    private LocalDateTime[] parseRange(String beginDate, String endDate) {
        LocalDate begin = LocalDate.parse(beginDate, DATE);
        LocalDate end = LocalDate.parse(endDate, DATE);
        if (end.isBefore(begin)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        if (begin.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "统计跨度最多 " + MAX_RANGE_DAYS + " 天");
        }
        return new LocalDateTime[]{begin.atStartOfDay(), end.plusDays(1).atStartOfDay()};
    }

    /** [begin, end] 连续日期序列（含端点） */
    private List<LocalDate> dateSequence(LocalDateTime[] range) {
        LocalDate begin = range[0].toLocalDate();
        LocalDate end = range[1].toLocalDate().minusDays(1);
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
            dates.add(d);
        }
        return dates;
    }

    /** 可用座位数（单店全量，排除逻辑删除） */
    private long availableSeatCount() {
        Long cnt = seatMapper.selectCount(null);
        return cnt == null || cnt == 0 ? AVAILABLE_SEATS_FALLBACK : cnt;
    }

    private LocalDate toDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toLocalDate();
        }
        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        }
        return LocalDate.parse(String.valueOf(dateObj), DATE);
    }

    private BigDecimal toDecimal(Object obj) {
        return obj == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(obj));
    }

    /** Map 行 → NameValueVO 列表（name 兜底） */
    private List<NameValueVO> toNameValues(List<Map<String, Object>> rows, String defaultName) {
        List<NameValueVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            String name = row.get("name") == null ? defaultName : String.valueOf(row.get("name"));
            Long cnt = row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue();
            BigDecimal amount = row.get("amount") == null ? null
                    : new BigDecimal(String.valueOf(row.get("amount")));
            result.add(new NameValueVO(name, cnt, amount));
        }
        return result;
    }
}
