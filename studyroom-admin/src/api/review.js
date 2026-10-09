import request from '@/utils/request'

/**
 * 管理端评价管理 API —— FR-AD-017 / FR-AD-018
 */

/** 评价列表（score 评分 / beginDate,endDate 时间 / status 显示状态 / keyword 内容模糊） */
export const pageReviews = (params) => request.get('/admin/review/page', { params })

/** 回复评价（10-200 字，已有回复则编辑覆盖） */
export const replyReview = (reviewId, content) =>
  request.post(`/admin/review/${reviewId}/reply`, { content })

/** 隐藏评价 */
export const hideReview = (reviewId) => request.post(`/admin/review/${reviewId}/hide`)

/** 显示评价 */
export const showReview = (reviewId) => request.post(`/admin/review/${reviewId}/show`)
