import request from '@/utils/request'

/**
 * 管理端用户管理 API —— FR-AD-014
 */

/** 用户列表（keyword 昵称模糊 / status 账号状态 / creditStatus 信用状态） */
export const pageUsers = (params) => request.get('/admin/user/page', { params })

/** 用户详情（基本信息 + 学习记录 + 评价记录） */
export const getUserDetail = (userId) => request.get(`/admin/user/${userId}`)

/** 禁用账号 */
export const disableUser = (userId) => request.post(`/admin/user/${userId}/disable`)

/** 启用账号 */
export const enableUser = (userId) => request.post(`/admin/user/${userId}/enable`)
