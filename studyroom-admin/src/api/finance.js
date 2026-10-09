import request from '@/utils/request'

/**
 * 管理端财务管理 API —— FR-AD-024
 */

/** 财务总览（汇总 + 按日趋势）：{ beginDate, endDate } */
export const financeSummary = (params) => request.get('/admin/finance/summary', { params })

/** 退款记录分页：{ beginDate, endDate, type, keyword, page, size } */
export const refundPage = (params) => request.get('/admin/finance/refund/page', { params })

/** 退款记录 Excel 导出（与列表同条件），返回 Blob */
export const refundExport = (params) =>
  request.get('/admin/finance/refund/export', { params, responseType: 'blob' })
