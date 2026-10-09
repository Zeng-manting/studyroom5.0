import request from '@/utils/request'

/**
 * 管理端公告管理 API —— FR-AD-015 / FR-AD-016
 */

/** 公告列表（type 类型 / status 状态筛选） */
export const pageNotices = (params) => request.get('/admin/notice/page', { params })

/** 公告详情（编辑回显，含正文与附件） */
export const getNotice = (noticeId) => request.get(`/admin/notice/${noticeId}`)

/** 新建/编辑公告（publishNow=true 保存后直接发布） */
export const saveNotice = (data) => request.post('/admin/notice/save', data)

/** 发布公告 */
export const publishNotice = (noticeId) => request.post(`/admin/notice/${noticeId}/publish`)

/** 下架公告 */
export const offlineNotice = (noticeId) => request.post(`/admin/notice/${noticeId}/offline`)

/** 删除公告 */
export const deleteNotice = (noticeId) => request.delete(`/admin/notice/${noticeId}`)
