package com.studyroom.common.exception;

import com.studyroom.common.result.Result;
import com.studyroom.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 —— FR-SY-003
 * <p>@RestControllerAdvice 统一捕获业务异常与系统异常，
 * 返回标准错误码 { code, message, data } 结构。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：Service 层主动抛出，直接透出提示信息
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] {} {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常：@RequestBody + @Valid/@Validated（Hibernate Validator）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("；"));
        log.warn("[参数校验失败] {}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 参数校验异常：表单绑定（@ModelAttribute）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("[参数绑定失败] {}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 参数校验异常：@RequestParam / @PathVariable 上的约束注解
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("；"));
        log.warn("[约束校验失败] {}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "缺少必填参数：" + e.getParameterName();
        log.warn("[缺少参数] {}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 上传文件超限（如评价图片最多 6 张，FR-UC-039）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("[上传超限] {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, "上传文件大小超出限制");
    }

    /**
     * 404：请求路径不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("[404] {} {}", request.getMethod(), request.getRequestURI());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 兜底：未预期系统异常，隐藏堆栈细节，仅记录日志
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.ERROR);
    }
}
