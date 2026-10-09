package com.studyroom.common.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web 上下文工具
 * <p>封装 HttpServletRequest / HttpServletResponse / 客户端 IP 获取，
 * 配合 AOP 在非 Controller 层也能拿到当前请求上下文。</p>
 *
 * @author studyroom
 */
public final class WebUtils {

    private WebUtils() {
    }

    public static HttpServletRequest currentRequest() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) ra).getRequest();
        }
        return null;
    }

    public static HttpServletResponse currentResponse() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) ra).getResponse();
        }
        return null;
    }

    /**
     * 获取客户端真实 IP（按顺序尝试：X-Forwarded-For → X-Real-IP → RemoteAddr）
     */
    public static String getClientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return "unknown";
        }
        String ip = req.getHeader("X-Forwarded-For");
        if (isInvalidIp(ip)) {
            ip = req.getHeader("X-Real-IP");
        }
        if (isInvalidIp(ip)) {
            ip = req.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = req.getRemoteAddr();
        }
        // X-Forwarded-For 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "unknown" : ip;
    }

    private static boolean isInvalidIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}