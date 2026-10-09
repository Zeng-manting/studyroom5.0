package com.studyroom.common.aspect;

import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.RedissonUtils;
import com.studyroom.common.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 限流注解 AOP（基于 Redisson RRateLimiter）
 *
 * <p>逻辑：根据 key 策略拼接限流 key，调用 RedissonUtils 申请令牌，失败抛 BusinessException，
 * 由 GlobalExceptionHandler 转译为统一 429 响应。</p>
 *
 * @author studyroom
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final RedissonUtils redissonUtils;

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint pjp, RateLimiter rateLimiter) throws Throwable {
        String key = buildKey(rateLimiter);
        boolean ok = redissonUtils.tryAcquire(
                key,
                rateLimiter.permits(),
                toDuration(rateLimiter.interval(), rateLimiter.unit()));
        if (!ok) {
            log.warn("[RateLimiterAspect] 触发限流: key={}, permits={}, interval={} {}",
                    key, rateLimiter.permits(), rateLimiter.interval(), rateLimiter.unit());
            throw new BusinessException(ResultCode.RATE_LIMITED, rateLimiter.message());
        }
        return pjp.proceed();
    }

    private String buildKey(RateLimiter limiter) {
        String prefix = CommonConstants.RATE_LIMITER_KEY + limiter.value();
        switch (limiter.key()) {
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

    private java.time.Duration toDuration(int interval, TimeUnit unit) {
        long ms = TimeUnit.MILLISECONDS.convert(interval, unit);
        return java.time.Duration.ofMillis(ms);
    }
}