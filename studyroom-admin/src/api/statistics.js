import request from '@/utils/request'

/**
 * 管理端数据统计 API —— FR-AD-019 ~ FR-AD-023
 */

/* ---------- FR-AD-019 预约统计 ---------- */

/** 预约时段分布（24h）：{ beginDate, endDate } */
export const bookingHours = (params) => request.get('/admin/stats/booking/hours', { params })

/** 套餐占比（饼图） */
export const packageShare = (params) => request.get('/admin/stats/booking/packages', { params })

/* ---------- FR-AD-020 座位统计 ---------- */

/** 上座率趋势（每日%） */
export const occupancyTrend = (params) => request.get('/admin/stats/seat/occupancy-trend', { params })

/** 座位类型占用分布 */
export const seatTypeDistribution = (params) => request.get('/admin/stats/seat/type-distribution', { params })

/* ---------- FR-AD-021 营收统计 ---------- */

/** 收入构成（按套餐） */
export const revenueComposition = (params) => request.get('/admin/stats/revenue/composition', { params })

/** 退款趋势（金额 + 笔数） */
export const refundTrend = (params) => request.get('/admin/stats/revenue/refund-trend', { params })

/** 客单价趋势（客单价 + 订单量） */
export const avgOrderValue = (params) => request.get('/admin/stats/revenue/avg-order-value', { params })

/* ---------- FR-AD-022 用户活跃度 ---------- */

/** 用户活跃趋势（DAU + 每日新增） */
export const userActiveTrend = (params) => request.get('/admin/stats/user/active-trend', { params })

/** 学习时长分布直方图 */
export const durationHistogram = (params) => request.get('/admin/stats/user/duration-histogram', { params })

/** 注册队列留存率 */
export const retention = (params) => request.get('/admin/stats/user/retention', { params })

/* ---------- FR-AD-023 报表导出 ---------- */

/** 统计报表 Excel 导出（逐日预约/营收/退款/客单价/DAU），返回 Blob */
export const exportStats = (params) =>
  request.get('/admin/stats/export', { params, responseType: 'blob' })
