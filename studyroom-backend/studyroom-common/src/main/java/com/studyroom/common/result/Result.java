package com.studyroom.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构：{ code, message, data }
 * <p>接口规范见《技术选型与功能汇总.md》1.3 节：
 * 遵循 RESTful 风格，所有接口统一返回本结构。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 响应码：200 成功，401 未认证，500 系统异常等，见 {@link ResultCode} */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功（携带数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 失败（默认系统异常） */
    public static <T> Result<T> fail() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /** 失败（指定响应码） */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败（自定义 code + message） */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /** 失败（指定响应码 + 覆盖提示信息） */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /** 判断是否成功 */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
