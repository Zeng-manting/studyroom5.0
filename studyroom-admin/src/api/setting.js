import request from '@/utils/request'

/**
 * 管理端价格与系统配置 API —— FR-AD-025 / FR-AD-026
 */

/* ---------- FR-AD-025 价格配置 ---------- */

/** 套餐列表（含下架） */
export const listPackages = () => request.get('/admin/price/packages')

/** 新建/编辑套餐 */
export const savePackage = (data) => request.post('/admin/price/package/save', data)

/** 套餐上/下架 */
export const togglePackageStatus = (id, online) =>
  request.post(`/admin/price/package/${id}/status`, null, { params: { online } })

/** 时段加价规则列表 */
export const listPriceRules = () => request.get('/admin/price/rules')

/** 新建/编辑加价规则 */
export const savePriceRule = (data) => request.post('/admin/price/rule/save', data)

/** 删除加价规则 */
export const deletePriceRule = (id) => request.delete(`/admin/price/rule/${id}`)

/** 座位类型列表（含座位数） */
export const listSeatTypes = () => request.get('/admin/price/seat-types')

/** 新建/编辑座位类型 */
export const saveSeatType = (data) => request.post('/admin/price/seat-type/save', data)

/* ---------- FR-AD-026 系统配置 ---------- */

/** 按分组读取配置（group 空 = 全部） */
export const listConfigs = (group) => request.get('/admin/config/list', { params: { group } })

/** 批量保存配置值 */
export const saveConfigs = (values) => request.post('/admin/config/save', values)
