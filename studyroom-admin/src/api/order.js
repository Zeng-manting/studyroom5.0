import request from '@/utils/request'

/**
 * 管理端订单管理 API —— FR-AD-010 ~ FR-AD-013
 */

/**
 * 订单列表（FR-AD-010）
 * @param {object} params - { beginDate, endDate, status, seatNo, userId, keyword, page, size }
 */
export const pageOrders = (params) => request.get('/admin/order/page', { params })

/**
 * 订单 Excel 导出（FR-AD-013）：与列表同筛选条件，返回 Blob
 */
export const exportOrders = (params) =>
  request.get('/admin/order/export', { params, responseType: 'blob' })

/** 申诉列表（FR-AD-011）：{ status, page, size } */
export const pageAppeals = (params) => request.get('/admin/order/appeal/page', { params })

/** 申诉详情（含凭证图片，FR-AD-011） */
export const getAppealDetail = (appealId) => request.get(`/admin/order/appeal/${appealId}`)

/** 申诉处理：标记签到成功（FR-AD-011） */
export const approveAppeal = (appealId, remark) =>
  request.post(`/admin/order/appeal/${appealId}/approve`, { remark })

/** 申诉处理：驳回（FR-AD-011，处理意见必填） */
export const rejectAppeal = (appealId, remark) =>
  request.post(`/admin/order/appeal/${appealId}/reject`, { remark })

/** 退款预览：系统建议退款金额（FR-AD-012） */
export const refundPreview = (orderId) => request.get(`/admin/order/${orderId}/refund-preview`)

/** 退款审核通过（FR-AD-012，金额可调整） */
export const submitRefund = (orderId, amount, reason) =>
  request.post(`/admin/order/${orderId}/refund`, { amount, reason })
