package com.studyroom.service.statistics;

import com.studyroom.entity.vo.statistics.DateTrendVO;
import com.studyroom.entity.vo.statistics.NameValueVO;

import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计服务 —— FR-AD-019 ~ FR-AD-023
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-AD-019 预约统计：时段分布（24h 柱/热力）+ 套餐占比（饼图）</li>
 *   <li>FR-AD-020 座位统计：上座率趋势 + 类型占用分布（利用率排行复用仪表盘 FR-AD-003 seat-top）</li>
 *   <li>FR-AD-021 营收统计：收入构成 + 退款趋势 + 客单价（趋势图复用仪表盘 trend）</li>
 *   <li>FR-AD-022 用户活跃度：DAU 趋势 + 新增 vs 活跃 + 时长分布直方图 + 次日留存率</li>
 *   <li>FR-AD-023 报表导出：指定时间范围导出统计 Excel</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminStatisticsService {

    /** FR-AD-019 预约时段分布：24 小时逐时预约量（无数据小时补 0） */
    List<NameValueVO> bookingHourDistribution(String beginDate, String endDate);

    /** FR-AD-019 套餐占比饼图数据 */
    List<NameValueVO> packageShare(String beginDate, String endDate);

    /** FR-AD-020 上座率趋势（每日使用时长 / 可用座位总时长） */
    DateTrendVO occupancyTrend(String beginDate, String endDate);

    /** FR-AD-020 座位类型占用分布 */
    List<NameValueVO> seatTypeDistribution(String beginDate, String endDate);

    /** FR-AD-021 收入构成（按套餐） */
    List<NameValueVO> revenueComposition(String beginDate, String endDate);

    /** FR-AD-021 退款趋势（每日退款金额 + 笔数） */
    DateTrendVO refundTrend(String beginDate, String endDate);

    /** FR-AD-021 客单价趋势（每日营收/订单数，含日订单量副序列） */
    DateTrendVO avgOrderValueTrend(String beginDate, String endDate);

    /** FR-AD-022 用户活跃趋势：DAU 主序列 + 每日新增副序列 */
    DateTrendVO userActiveTrend(String beginDate, String endDate);

    /** FR-AD-022 学习时长分布直方图（0-1h/1-2h/2-4h/4-8h/8h+ 分桶） */
    List<NameValueVO> durationHistogram(String beginDate, String endDate);

    /** FR-AD-022 次日~7日留存（按注册日队列，返回日期 + 新增数 + 留存数 + 留存率%） */
    List<Map<String, Object>> retentionCohorts(String beginDate, String endDate);

    /** FR-AD-023 报表导出数据：逐日预约量/营收/退款/客单价（Excel 用） */
    List<Map<String, Object>> dailyReportRows(String beginDate, String endDate);
}
