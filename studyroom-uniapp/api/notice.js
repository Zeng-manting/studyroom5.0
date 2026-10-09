/**
 * 公告 API（FR-UC-041~043）
 */
import request from '@/utils/request'

/**
 * 公告列表（FR-UC-041：仅已发布且 publish_time <= now，分页）
 * @param {number} type 1制度/2停业公告/3活动/4其他，不传查全部
 */
export const getNotices = (type, page = 1, size = 10) =>
  request.get('/notice/list', { ...(type ? { type } : {}), page, size })

/**
 * 重要公告弹窗（FR-UC-042：最新一条重要且未读，无则 data 为 null）
 */
export const getNoticePopup = () => request.get('/notice/popup')

/**
 * 公告详情（FR-UC-043：首次访问自动落已读 + read_count 自增）
 */
export const getNoticeDetail = (id) => request.get(`/notice/${id}`)
