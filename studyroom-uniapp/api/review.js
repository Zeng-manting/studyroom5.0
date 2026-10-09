/**
 * 评价 API（FR-UC-039/040）
 */
import request from '@/utils/request'

/**
 * 提交评价（FR-UC-039：一单一评，5星+标签+文字10-500字+图片≤6张）
 * request 层自动预取 X-Idempotent-Token（FR-SY-001）
 */
export const submitReview = (data) =>
  request.post('/review', data, { idempotent: 'review:submit' })

/**
 * 门店评价列表（FR-UC-040：昵称脱敏/匿名，含管理员回复）
 */
export const getStoreReviews = (storeId, page = 1, size = 10) =>
  request.get('/review/list', { storeId, page, size })

/** 我的评价列表（FR-UC-040） */
export const getMyReviews = (page = 1, size = 10) =>
  request.get('/review/mine', { page, size })
