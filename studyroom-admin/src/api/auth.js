import request from '@/utils/request'

/**
 * 管理端认证 API —— FR-AD-001
 */

/** 获取图形验证码（Base64，5 分钟一次性） */
export const getCaptcha = () => request.get('/admin/captcha')

/** 管理员登录：账号 + 密码 + 验证码，返回 JWT（2 小时）+ 管理员信息 */
export const login = (data) => request.post('/admin/login', data)

/** 退出登录（当前 token 拉黑） */
export const logout = () => request.post('/admin/logout')

/** 当前管理员信息 */
export const getMe = () => request.get('/admin/me')
