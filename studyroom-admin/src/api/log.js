import request from '@/utils/request'

/**
 * 管理端操作日志 API —— FR-AD-028
 */

/** 分页查询：{ beginDate, endDate, username, module, status, page, size } */
export const pageLogs = (params) => request.get('/admin/log/page', { params })
