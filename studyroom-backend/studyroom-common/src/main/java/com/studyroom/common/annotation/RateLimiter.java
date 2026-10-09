package com.studyroom.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解（基于 Redisson RRateLimiter）
 *
 * <h3>使用示例</h3>
 * <pre>
 * {@code
 * // 整个接口 5 QPS（全局）
 * @RateLimiter(value = "global", permits = 5, interval = 1, unit = TimeUnit.SECONDS)
 *
 * // 按登录用户限流（建议用于登录后接口）
 * @RateLimiter(value = "order:create", permits = 3, interval = 10, unit = TimeUnit.SECONDS, key = KeyType.USER)
 *
 * // 按 IP 限流（建议用于公开接口：登录、签到提交）
 * @RateLimiter(value = "sign", permits = 10, interval = 1, unit = TimeUnit.MINUTES, key = KeyType.IP)
 * }
 * </pre>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-SY-001 接口限流、防滥用</li>
 *   <li>FR-UC-001 微信 code2Session 频控</li>
 *   <li>FR-UC-030 签到提交防爆破</li>
 * </ul>
 *
 * @author studyroom
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /** 限流 key 名（建议按业务域冒号分隔，例如 order:create、user:login） */
    String value();

    /** 时间窗口内允许的请求数 */
    int permits() default 1;

    /** 时间窗口大小 */
    int interval() default 1;

    /** 时间单位 */
    TimeUnit unit() default TimeUnit.SECONDS;

    /** 限流 key 拼接策略 */
    KeyType key() default KeyType.GLOBAL;

    /** 触发限流时的错误码，默认 429（ResultCode.RATE_LIMIT） */
    int code() default 429;

    /** 触发限流时的错误消息 */
    String message() default "操作过于频繁，请稍后再试";

    /** 限流 Key 拼接策略 */
    enum KeyType {
        /** 全局，所有用户共享一个限流桶 */
        GLOBAL,
        /** 按登录用户 ID 区分（未登录走 IP） */
        USER,
        /** 按客户端 IP 区分 */
        IP
    }
}