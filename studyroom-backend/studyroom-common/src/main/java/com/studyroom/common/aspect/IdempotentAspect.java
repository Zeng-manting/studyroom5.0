package com.studyroom.common.aspect;

import com.studyroom.common.annotation.Idempotent;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.RedissonUtils;
import com.studyroom.common.utils.RedisUtils;
import com.studyroom.common.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 幂等注解 AOP
 *
 * <h3>流程</h3>
 * <ol>
 *   <li>从请求头读取 {@link Idempotent#header()} 中的 token</li>
 *   <li>按 key 策略 + token 拼接 Redis Key，调用 {@link RedisUtils#getAndDelete} 原子消费</li>
 *   <li>消费失败（重复请求）抛 BusinessException(IDEMPOTENT)</li>
 *   <li>消费成功才执行方法体</li>
 * </ol>
 *
 * <p>token 由前端在页面打开时调用 {@code /api/idempotent/token?biz=xxx} 预取，
 * 同一个表单的所有提交共用一个 token。</p>
 *
 * @author studyroom
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(0)   // 最先执行：避免先被 RateLimiter 等消耗
public class IdempotentAspect {

    private final RedisUtils redisUtils;
    private final RedissonUtils redissonUtils;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        String token = resolveToken(idempotent);
        if (token == null || token.isEmpty()) {
            log.warn("[IdempotentAspect] 缺少幂等 token header={}", idempotent.header());
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "缺少幂等性校验 token，请在请求头添加 " + idempotent.header());
        }

        String key = buildKey(idempotent) + ":" + token;
        // 一次性消费：存在则删除并通过；不存在则拒绝
        String value = redisUtils.getAndDelete(key, token);
        if (value == null) {
            log.warn("[IdempotentAspect] 幂等校验失败: key={}, token={}", key, token);
            throw new BusinessException(ResultCode.IDEMPOTENT_ERROR, idempotent.message());
        }
        return pjp.proceed();
    }

    /**
     * 由 Controller 调用，签发幂等 token 并写入 Redis。
     * <p>token 默认有效期由 timeout 决定（业务侧传入）；</p>
     * 前端应在页面加载时调用一次，获取后整个页面生命周期复用。</p>
     *
     * @param biz     业务域 key（对应 @Idempotent.value）
     * @param timeout 有效期数值
     * @param unit    时间单位
     * @param keyType 拼接策略
     * @return token 字符串
     */
    public String issue(String biz, int timeout, TimeUnit unit, Idempotent.KeyType keyType) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = buildKey(biz, keyType) + ":" + token;
        redisUtils.set(key, token, timeout, unit);
        return token;
    }

    private String resolveToken(Idempotent idempotent) {
        return WebUtils.currentRequest() == null
                ? null
                : WebUtils.currentRequest().getHeader(idempotent.header());
    }

    private String buildKey(Idempotent idempotent) {
        return buildKey(idempotent.value(), idempotent.key());
    }

    private String buildKey(String biz, Idempotent.KeyType keyType) {
        String prefix = CommonConstants.IDEMPOTENT_KEY + biz;
        switch (keyType) {
            case USER: {
                Long uid = AuthContextHolder.get() == null ? null : AuthContextHolder.get().getId();
                return prefix + ":user:" + (uid == null ? "anon" : uid);
            }
            case IP:
                return prefix + ":ip:" + WebUtils.getClientIp();
            case GLOBAL:
            default:
                return prefix + ":global";
        }
    }
}