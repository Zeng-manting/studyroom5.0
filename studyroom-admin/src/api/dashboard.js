import request from '@/utils/request'

/**
 * 管理端仪表盘 API —— FR-AD-002 / FR-AD-003
 */

/** 核心指标卡片：今日预约量/营收/上座率/活跃用户（含昨日值供环比） */
export const getSummary = () => request.get('/admin/dashboard/summary')

/** 预约量与营收趋势（days=7|30，返回按日期升序双序列） */
export const getTrend = (days = 7) =>
  request.get('/admin/dashboard/trend', { params: { days } })

/** 座位状态总览（六色状态实时快照） */
export const getSeatStatus = () => request.get('/admin/dashboard/seat-status')

/** 座位利用率 Top10（days 统计窗口天数，limit 取前 N） */
export const getSeatTop = (days = 30, limit = 10) =>
  request.get('/admin/dashboard/seat-top', { params: { days, limit } })
