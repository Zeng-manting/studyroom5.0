import request from '@/utils/request'

/**
 * 管理端优惠券配置 API —— FR-AD-027
 */

/** 分页列表：{ type, status, page, size } */
export const pageCoupons = (params) => request.get('/admin/coupon/page', { params })

/** 新建/编辑优惠券（id 空 = 新建） */
export const saveCoupon = (data) => request.post('/admin/coupon/save', data)

/** 上架/下架 */
export const toggleCouponStatus = (id, online) =>
  request.post(`/admin/coupon/${id}/status`, null, { params: { online } })

/** 定向发放（按用户ID列表） */
export const grantCoupon = (data) => request.post('/admin/coupon/grant', data)

/** 领取明细（最近 100 条） */
export const listClaims = (couponId) => request.get(`/admin/coupon/${couponId}/claims`)
