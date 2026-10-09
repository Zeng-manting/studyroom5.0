package com.studyroom.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 常用操作工具
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>封装 RLock / RRateLimiter 高频模板代码</li>
 *   <li>由业务模块直接注入，业务代码不需要关心 RedissonClient 的获取</li>
 * </ul>
 *
 * <h3>使用建议</h3>
 * <ul>
 *   <li>分布式锁：{@link #tryLock(String, Duration)} + try/finally</li>
 *   <li>接口限流：{@link #tryAcquire(String, long, Duration)} / {@link #tryAcquire(String, long, long, TimeUnit)}</li>
 *   <li>业务模板中如需延时任务，使用 {@link RedissonClient#getDelayedQueue}</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonUtils {

    private final RedissonClient redissonClient;

    // ==================== 分布式锁 ====================

    /**
     * 尝试加锁（最多等待 0 秒，失败立即返回）。try-with-resources 友好，业务代码需手动 unlock。
     *
     * @param key    锁 key
     * @param leaseTime 锁最大持有时间（防止宕机死锁）；建议设置为业务最长执行时间的 3 倍
     * @return 锁对象；返回 null 表示加锁失败（业务可走"已被占用"分支）
     */
    public RLockWrapper tryLock(String key, Duration leaseTime) {
        org.redisson.api.RLock lock = redissonClient.getLock(key);
        try {
            boolean ok = lock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            return ok ? new RLockWrapper(lock) : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[RedissonUtils] 加锁被中断: key={}", key);
            return null;
        }
    }

    /**
     * 包装 RLock 提供 close() 语义；业务可使用 try-with-resources 自动释放。
     */
    public static class RLockWrapper implements AutoCloseable {

        private final org.redisson.api.RLock lock;
        private boolean released = false;

        RLockWrapper(org.redisson.api.RLock lock) {
            this.lock = lock;
        }

        public org.redisson.api.RLock raw() {
            return lock;
        }

        public void unlock() {
            if (released) {
                return;
            }
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                log.error("[RedissonUtils] 释放锁异常", e);
            } finally {
                released = true;
            }
        }

        @Override
        public void close() {
            unlock();
        }
    }

    // ==================== 限流 ====================

    /**
     * 尝试获取 1 个令牌（窗口期内）。
     *
     * @param key      限流 key（业务侧按 IP / userId / 全局 拼接）
     * @param permits  令牌数
     * @param interval 时间窗口
     * @return true=获取成功，false=触发限流
     */
    public boolean tryAcquire(String key, long permits, Duration interval) {
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        // 幂等：仅在未设置时初始化（首次 1 次）
        limiter.isExists();
        // 重新设置策略（幂等）
        limiter.trySetRate(RateType.OVERALL, permits, interval.toMillis(), RateIntervalUnit.MILLISECONDS);
        return limiter.tryAcquire();
    }

    /**
     * 通用限流（每秒 N 个请求）。
     */
    public boolean tryAcquirePerSecond(String key, long permitsPerSecond) {
        return tryAcquire(key, permitsPerSecond, Duration.ofSeconds(1));
    }

    /**
     * 通用限流（每分钟 N 个请求）。
     */
    public boolean tryAcquirePerMinute(String key, long permitsPerMinute) {
        return tryAcquire(key, permitsPerMinute, Duration.ofMinutes(1));
    }
}