package com.studyroom.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 方法参数：自动注入当前登录用户
 *
 * <h3>使用示例</h3>
 * <pre>
 * {@code
 * @GetMapping("/me")
 * public Result<UserVO> me(@CurrentUser LoginUser user) { ... }
 * }
 * </pre>
 *
 * <p>未登录访问会被 SecurityFilter 拦截，方法参数不会为 null。</p>
 *
 * @author studyroom
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    /** 是否允许匿名（默认 false，匿名调用会抛 401） */
    boolean anonymous() default false;
}