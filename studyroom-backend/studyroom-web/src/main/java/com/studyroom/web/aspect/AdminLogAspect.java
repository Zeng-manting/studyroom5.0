package com.studyroom.web.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.annotation.OperationLog;
import com.studyroom.common.enums.UserTypeEnum;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.utils.JacksonUtils;
import com.studyroom.common.utils.WebUtils;
import com.studyroom.entity.admin.Admin;
import com.studyroom.entity.admin.AdminLog;
import com.studyroom.mapper.admin.AdminMapper;
import com.studyroom.mapper.admin.AdminLogMapper;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * 管理端操作日志切面 —— FR-AD-028
 *
 * <h3>拦截范围</h3>
 * <ul>
 *   <li>兜底：{@code com.studyroom.web.controller.admin} 包下所有 POST/PUT/DELETE 接口</li>
 *   <li>显式：标注 {@link OperationLog} 的方法可自定义模块与操作描述</li>
 * </ul>
 *
 * <h3>落库内容</h3>
 * <ul>
 *   <li>操作人：JWT 上下文 adminId + AdminMapper 反查 username（失败降级 roleKey）</li>
 *   <li>操作描述：@OperationLog &gt; @ApiOperation &gt; 方法名</li>
 *   <li>模块：@OperationLog &gt; Controller 类名映射表 &gt; 类名去 Controller 后缀</li>
 *   <li>参数：JSON 序列化（跳过 Servlet 对象；password 类字段打码）</li>
 *   <li>IP / User-Agent / 耗时 / 成败 / 错误信息（截断 500 字）</li>
 * </ul>
 *
 * <p>日志写入放在请求线程内 try-finally 保证异常路径也记录；
 * 落库失败仅 warn 不影响业务（日志组件自身必须无感）。</p>
 *
 * @author studyroom
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(50)  // 在幂等(0)/限流之后执行，避免重复计数
public class AdminLogAspect {

    /** password 类字段打码（JSON 字符串层级匹配） */
    private static final Pattern SENSITIVE = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword|pwd)\"\\s*:\\s*\")[^\"]*(\")");

    private static final ObjectMapper MAPPER = JacksonUtils.defaultMapper();

    private final AdminLogMapper adminLogMapper;
    private final AdminMapper adminMapper;

    /**
     * 兜底拦截管理端包下全部写请求 + 显式标注 @OperationLog 的方法
     * <p>注：@OperationLog 不在切点中绑定参数（避免 OR 分支导致 AspectJ inconsistent binding），
     * 改为在方法体内通过反射获取，可能为 null。</p>
     */
    @Around("(execution(* com.studyroom.web.controller.admin..*(..))"
            + " && (@annotation(org.springframework.web.bind.annotation.PostMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)))"
            + " || @annotation(com.studyroom.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        OperationLog operationLog = AnnotationUtils.findAnnotation(signature.getMethod(), OperationLog.class);
        long start = System.currentTimeMillis();
        AdminLog entry = buildEntry(pjp, operationLog);
        try {
            Object result = pjp.proceed();
            entry.setStatus(1);
            return result;
        } catch (Throwable ex) {
            entry.setStatus(0);
            entry.setErrorMsg(abbreviate(ex.getMessage()));
            throw ex;
        } finally {
            entry.setCostMs(System.currentTimeMillis() - start);
            saveQuietly(entry);
        }
    }

    /* ==================== 组装 ==================== */

    private AdminLog buildEntry(ProceedingJoinPoint pjp, OperationLog operationLog) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();

        AdminLog entry = new AdminLog();
        entry.setModule(resolveModule(operationLog, pjp));
        entry.setOperation(resolveOperation(operationLog, signature));
        entry.setMethod(signature.getDeclaringType().getSimpleName() + "#" + signature.getName());
        entry.setParams(serializeParams(pjp.getArgs()));
        entry.setStatus(1);

        // 操作人：JWT 上下文
        LoginUser user = AuthContextHolder.get();
        if (user != null) {
            entry.setAdminId(user.getId());
            entry.setUsername(resolveUsername(user));
        }

        // 请求元信息
        HttpServletRequest request = currentRequest();
        if (request != null) {
            entry.setRequestUrl(request.getRequestURI());
            entry.setIp(WebUtils.getClientIp());
            entry.setUserAgent(abbreviate(request.getHeader("User-Agent")));
        }
        return entry;
    }

    private String resolveUsername(LoginUser user) {
        if (user.getUserType() != UserTypeEnum.ADMIN || user.getId() == null) {
            return user.getRoleKey() == null ? "unknown" : user.getRoleKey();
        }
        try {
            Admin admin = adminMapper.selectById(user.getId());
            if (admin != null && admin.getUsername() != null) {
                return admin.getUsername();
            }
        } catch (Exception ignore) {
            // 反查失败降级 roleKey
        }
        return user.getRoleKey() == null ? ("admin#" + user.getId()) : user.getRoleKey();
    }

    private String resolveModule(OperationLog operationLog, ProceedingJoinPoint pjp) {
        if (operationLog != null && !operationLog.module().isEmpty()) {
            return operationLog.module();
        }
        String controller = pjp.getSignature().getDeclaringType().getSimpleName();
        switch (controller) {
            case "AdminController": return "登录与权限";
            case "DashboardController": return "仪表盘";
            case "AdminOrderController": return "订单管理";
            case "AdminStoreController": return "门店与座位";
            case "AdminUserController": return "用户管理";
            case "AdminNoticeController": return "公告管理";
            case "AdminReviewController": return "评价管理";
            case "AdminStatisticsController": return "数据统计";
            case "AdminFinanceController": return "财务总览";
            case "AdminPricingController": return "价格配置";
            case "AdminCouponController": return "优惠券管理";
            case "AdminConfigController": return "系统配置";
            case "AdminLogController": return "操作日志";
            default: return controller.replaceAll("Controller$", "");
        }
    }

    private String resolveOperation(OperationLog operationLog, MethodSignature signature) {
        if (operationLog != null && !operationLog.value().isEmpty()) {
            return operationLog.value();
        }
        ApiOperation apiOperation = AnnotationUtils.findAnnotation(signature.getMethod(), ApiOperation.class);
        if (apiOperation != null && !apiOperation.value().isEmpty()) {
            return apiOperation.value();
        }
        return signature.getName();
    }

    /** 参数 JSON 化：跳过 Servlet/Binding 对象，敏感字段打码，超长截断 */
    private String serializeParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg == null
                        || arg instanceof HttpServletRequest
                        || arg instanceof javax.servlet.http.HttpServletResponse
                        || arg instanceof org.springframework.validation.BindingResult) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(MAPPER.writeValueAsString(arg));
            }
            if (sb.length() == 0) {
                return null;
            }
            return abbreviate(SENSITIVE.matcher(sb).replaceAll("$1***$2"));
        } catch (Exception ex) {
            return abbreviate("参数序列化失败: " + ex.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private void saveQuietly(AdminLog entry) {
        try {
            adminLogMapper.insert(entry);
        } catch (Exception ex) {
            log.warn("[AdminLog] 操作日志落库失败（不影响业务）: {} - {}", entry.getOperation(), ex.getMessage());
        }
    }

    private String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}
