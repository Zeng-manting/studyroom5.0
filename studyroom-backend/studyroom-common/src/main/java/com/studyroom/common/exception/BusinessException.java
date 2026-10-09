package com.studyroom.common.exception;

import com.studyroom.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>Service 层校验失败时抛出，由全局异常处理器
 * （FR-SY-003，见 GlobalExceptionHandler）统一捕获并转为标准响应。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 响应码 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /** 快速构建（常用） */
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException of(ResultCode resultCode) {
        return new BusinessException(resultCode);
    }
}
