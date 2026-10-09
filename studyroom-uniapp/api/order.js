/**
 * 订单 API（FR-UC-018~028）
 */
import request from '@/utils/request'

/** 订单费用预览（FR-UC-022） */
export const previewOrder = (params) => request.get('/order/preview', params)

/** 当前订单可用优惠券（FR-UC-024） */
export const getUsableCoupons = (params) => request.get('/order/coupons', params)

/**
 * 创建订单（FR-UC-018 原子锁座）
 * request 层自动预取 X-Idempotent-Token（FR-SY-001）
 */
export const createOrder = (data) =>
  request.post('/order/create', data, { idempotent: 'order:create' })

/** 订单支付（FR-UC-023：1余额 2模拟微信） */
export const payOrder = (orderId, payType) => request.post(`/order/${orderId}/pay`, { payType })

/** 取消订单（FR-UC-027 阶梯退款） */
export const cancelOrder = (orderId) => request.post(`/order/${orderId}/cancel`)

/** 订单详情（FR-UC-022/028） */
export const getOrderDetail = (orderId) => request.get(`/order/${orderId}`)

/* ==================== 模块6：使用中/暂停/结束（FR-UC-031/033/034） ==================== */

/** 使用中状态（FR-UC-031：剩余时间/暂停状态/结算结果） */
export const getUsage = (orderId) => request.get(`/order/${orderId}/usage`)

/** 中途临时离座（FR-UC-033：暂停计时，保留30分钟） */
export const pauseOrder = (orderId) => request.post(`/order/${orderId}/pause`)

/** 回座恢复使用（FR-UC-033） */
export const resumeOrder = (orderId) => request.post(`/order/${orderId}/resume`)

/** 离场结束使用（FR-UC-034：实际时长结算 + 按比例退差额） */
export const endOrder = (orderId) => request.post(`/order/${orderId}/end`)

/* ==================== 模块7：订单管理（FR-UC-035~038） ==================== */

/**
 * 订单列表（FR-UC-035 六 Tab）
 * @param {number} tab 0全部/1待支付/2已预约/3进行中/4已完成/5已取消
 */
export const getOrderList = (tab, page = 1, size = 10) =>
  request.get('/order/list', { tab, page, size })

/* ==================== 模块8：进行中续时（FR-UC-055） ==================== */

/**
 * 续时费用预览（FR-UC-055：按当前套餐单价折算）
 * @param {number} orderId 订单ID
 * @param {number} hours 续时小时数（1~12）
 */
export const extendPreview = (orderId, hours) =>
  request.get(`/order/${orderId}/extend-preview`, { hours })

/**
 * 执行续时（FR-UC-055：支付后顺延结束时间并续占座位）
 * @param {number} orderId 订单ID
 * @param {object} data { hours, payType: 1余额/2微信 }
 */
export const extendOrder = (orderId, data) =>
  request.post(`/order/${orderId}/extend`, data, { idempotent: 'order:extend' })
