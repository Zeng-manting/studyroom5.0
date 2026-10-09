package com.studyroom.common.login;

/**
 * 当前请求线程内的登录用户信息（基于 ThreadLocal）
 * <p>由 JwtAuthenticationFilter 在解析 Token 后写入，由 Controller / Service 任意位置读取，
 * 由 Spring MVC 的 @CurrentUser 参数解析器或 GlobalExceptionHandler finally 中清理。</p>
 *
 * <p>框架级基础组件，所有业务模块共享。</p>
 *
 * @author studyroom
 */
public final class AuthContextHolder {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    /** 设置当前登录用户（由 JwtAuthenticationFilter 调用） */
    public static void set(LoginUser user) {
        CONTEXT.set(user);
    }

    /** 获取当前登录用户；未登录返回 null */
    public static LoginUser get() {
        return CONTEXT.get();
    }

    /** 获取当前登录用户；未登录时抛业务异常（供业务接口使用） */
    public static LoginUser require() {
        LoginUser u = CONTEXT.get();
        if (u == null) {
            throw new com.studyroom.common.exception.BusinessException(
                    com.studyroom.common.result.ResultCode.UNAUTHORIZED);
        }
        return u;
    }

    /** 清理 ThreadLocal，防止线程复用导致信息泄露 */
    public static void clear() {
        CONTEXT.remove();
    }
}