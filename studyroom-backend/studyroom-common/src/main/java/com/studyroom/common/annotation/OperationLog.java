package com.studyroom.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端写操作日志注解 —— FR-AD-028
 *
 * <p>标注在管理端 Controller 的写方法上，由 {@code AdminLogAspect} 自动落
 * admin_log 流水（操作人 / 模块 / 参数 / IP / 耗时 / 成败）。</p>
 *
 * <p>说明：切面同时兜底拦截 {@code com.studyroom.web.controller.admin} 包下所有
 * POST/PUT/DELETE 请求（未标注的本注解的写接口也会被记录，模块名按 Controller
 * 类名推断）；显式标注本注解可自定义模块与操作描述。</p>
 *
 * @author studyroom
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作描述（空 = 取 @ApiOperation 值，再空 = 方法名） */
    String value() default "";

    /** 所属模块（空 = 按 Controller 类名推断） */
    String module() default "";
}
