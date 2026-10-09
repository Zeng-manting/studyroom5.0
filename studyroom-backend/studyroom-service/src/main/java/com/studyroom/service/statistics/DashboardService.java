package com.studyroom.service.statistics;

import com.studyroom.entity.vo.statistics.DashboardSummaryVO;
import com.studyroom.entity.vo.statistics.DashboardTrendVO;
import com.studyroom.entity.vo.statistics.SeatStatusStatsVO;
import com.studyroom.entity.vo.statistics.SeatTopVO;

import java.util.List;

/**
 * 仪表盘统计 Service —— FR-AD-002（核心指标卡片）/ FR-AD-003（趋势图表）
 *
 * @author studyroom
 */
public interface DashboardService {

    /**
     * 核心指标卡片 —— FR-AD-002
     * <p>今日预约量 / 营收 / 上座率 / 活跃用户（含昨日值供环比）。</p>
     */
    DashboardSummaryVO getSummary();

    /**
     * 预约量与营收趋势 —— FR-AD-003 折线图（7 天 / 30 天）
     *
     * @param days 7 或 30，其他值回退 7
     * @return 按日期升序，无数据日期补 0
     */
    List<DashboardTrendVO> getTrend(int days);

    /**
     * 座位状态总览 —— FR-AD-003 座位状态总览网格
     * <p>六色状态实时快照：0空闲 1使用中 2维护 3打扫中 4锁定。</p>
     */
    List<SeatStatusStatsVO> getSeatStatus();

    /**
     * 座位利用率排行 —— FR-AD-003 Top10
     *
     * @param days  统计窗口天数，默认 30
     * @param limit 取前 N 名，上限 20
     */
    List<SeatTopVO> getSeatTop(int days, int limit);
}
