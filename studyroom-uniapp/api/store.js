/**
 * 门店/座位/套餐 API（FR-UC-015~020）
 */
import request from '@/utils/request'

/** 门店列表（FR-UC-007~010） */
export const getStoreList = (keyword) => request.get('/store/list', { keyword })

/** 门店详情（FR-UC-015） */
export const getStoreDetail = (id) => request.get(`/store/${id}`)

/** 某日时段黑名单（FR-UC-020） */
export const getBlacklist = (id, date) => request.get(`/store/${id}/blacklist`, { date })

/** 座位平面图（FR-UC-017，含时段占用计算） */
export const getSeatMap = (storeId, start, end) =>
  request.get('/seat/map', { storeId, start, end })

/** 套餐列表（FR-UC-019：1按时长 2日卡 3周卡 4月卡） */
export const getPackageList = (type) => request.get('/package/list', type ? { type } : {})
