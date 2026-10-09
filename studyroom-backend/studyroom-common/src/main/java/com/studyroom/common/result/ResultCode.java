package com.studyroom.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应码枚举
 * <p>FR-SY-003 全局异常处理统一返回标准错误码与提示信息。</p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 参数 / 校验类错误 */
    PARAM_ERROR(400, "请求参数错误"),
    IDEMPOTENT_ERROR(409, "请勿重复提交"),

    /** 认证授权类错误（FR-UC-006 / FR-AD-001：401 由前端静默重登或跳转登录页） */
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    FORBIDDEN(403, "无访问权限"),

    /** 资源类错误 */
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持"),

    /** 系统类错误 */
    ERROR(500, "系统繁忙，请稍后重试"),

    /** 业务类错误（BusinessException 默认码） */
    BUSINESS_ERROR(600, "业务处理失败"),

    /** 限流（FR-SY-001 接口限流触发） */
    RATE_LIMITED(429, "操作过于频繁，请稍后再试");

    /** 响应码 */
    private final Integer code;

    /** 默认提示信息 */
    private final String message;
}
