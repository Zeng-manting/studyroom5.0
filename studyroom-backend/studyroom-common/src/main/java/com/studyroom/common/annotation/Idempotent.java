package com.studyroom.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等注解（基于 Redis Token）
 *
 * <h3>流程</h3>
 * <ol>
 *   <li>客户端先调用 {@code /api/idempotent/token} 获取 token（由前端框架在加载页面时预取并缓存）</li>
 *   <li>实际请求时通过 Header {@code X-Idempotent-Token} 传入</li>
 *   <li>AOP 在方法进入时从 Redis 取出并删除该 token（Lua 原子）；若不存在则拒绝（重复请求）</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>
 * {@code
 * @PostMapping("/order/create")
 * @Idempotent(value = "order:create", timeout = 30, unit = TimeUnit.SECONDS)
 * public Result<Long> createOrder(...) { ... }
 * }
 * </pre>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-SY-001 接口幂等</li>
 *   <li>FR-UC-018 锁座下单防重提交</li>
 *   <li>FR-UC-025 退款 / FR-UC-030 签到防重放</li>
 * </ul>
 *
 * @author studyroom
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等 key 前缀（与用户 ID / IP 拼成完整 key） */
    String value();

    /** token 在 Redis 中的有效期（防止 token 永久滞留）；通常大于业务最长执行时间 */
    int timeout() default 60;

    TimeUnit unit() default TimeUnit.SECONDS;

    /** Header 名（前端需在请求头携带） */
    String header() default "X-Idempotent-Token";

    /** key 拼接策略 */
    KeyType key() default KeyType.USER;

    /** 触发幂等失败时的提示 */
    String message() default "请勿重复提交";

    enum KeyType {
        /** 全局共享 token */
        GLOBAL,
        /** 按登录用户 ID */
        USER,
        /** 按 IP */
        IP
    }
}