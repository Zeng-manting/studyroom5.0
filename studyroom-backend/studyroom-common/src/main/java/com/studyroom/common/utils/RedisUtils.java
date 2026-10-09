package com.studyroom.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类（基于 StringRedisTemplate）
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>二次封装高频原子操作（incr/expire/setNx/lua 限流等），避免业务层直接拼接 Key</li>
 *   <li>所有方法统一返回业务类型而非 Redis 内部类型</li>
 *   <li>不持有 Redisson 客户端 —— 分布式锁/RateLimiter 由业务层直接使用 RedissonClient</li>
 * </ul>
 *
 * <h3>Key 规范</h3>
 * <p>所有 Redis Key 必须遵循 {@link com.studyroom.common.constants.CommonConstants} 中定义的前缀；
 * 工具类本身不再追加业务前缀，由调用方按规范传入完整 key。</p>
 *
 * @author studyroom
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final StringRedisTemplate redisTemplate;

    // ==================== String ====================

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, String value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Long getLong(String key) {
        String v = redisTemplate.opsForValue().get(key);
        return v == null ? null : Long.parseLong(v);
    }

    /**
     * 仅在 key 不存在时设置（SETNX），常用于幂等控制。
     *
     * @return true 表示本次设置成功
     */
    public boolean setNx(String key, String value, Duration timeout) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 仅在 key 不存在时设置（SETNX），由调用方自行 expire。
     */
    public boolean setNx(String key, String value) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 仅在 key 存在时设置（GETSET 替代），用于 CAS。
     */
    public boolean setEx(String key, String value, Duration timeout) {
        Boolean ok = redisTemplate.opsForValue().setIfPresent(key, value, timeout);
        return Boolean.TRUE.equals(ok);
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long incr(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long decr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long decr(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    public boolean expire(String key, Duration timeout) {
        Boolean ok = redisTemplate.expire(key, timeout);
        return Boolean.TRUE.equals(ok);
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean ok = redisTemplate.expire(key, timeout, unit);
        return Boolean.TRUE.equals(ok);
    }

    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    // ==================== Key 操作 ====================

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean delete(String key) {
        Boolean ok = redisTemplate.delete(key);
        return Boolean.TRUE.equals(ok);
    }

    public Long delete(Collection<String> keys) {
        Long n = redisTemplate.delete(keys);
        return n == null ? 0L : n;
    }

    /** 按前缀扫描并删除（仅运维使用，业务路径严禁调用） */
    public long deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long n = redisTemplate.delete(keys);
        return n == null ? 0L : n;
    }

    // ==================== Hash ====================

    public void hSet(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public String hGet(String key, String field) {
        Object v = redisTemplate.opsForHash().get(key, field);
        return v == null ? null : v.toString();
    }

    public Map<String, String> hGetAll(String key) {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        Map<Object, Object> raw = ops.entries(key);
        if (raw == null || raw.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<String, String> result = new java.util.HashMap<>(raw.size());
        raw.forEach((k, v) -> result.put(Objects.toString(k), Objects.toString(v)));
        return result;
    }

    public Long hDel(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    public Long hIncrBy(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    public Boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    // ==================== Set / ZSet 摘要 ====================

    public Long sAdd(String key, String... members) {
        return redisTemplate.opsForSet().add(key, members);
    }

    public Long sRemove(String key, String... members) {
        return redisTemplate.opsForSet().remove(key, members);
    }

    public Set<String> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public Boolean sIsMember(String key, String member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /** 短暂 token 校验：尝试"读+删除"原子操作（用 Lua 脚本） */
    private static final String LUA_DEL_IF_EQUAL = ""
            + "if redis.call('get', KEYS[1]) == ARGV[1] then\n"
            + "  return redis.call('del', KEYS[1])\n"
            + "else\n"
            + "  return 0\n"
            + "end";

    private static final DefaultRedisScript<Long> DEL_IF_EQUAL_SCRIPT =
            new DefaultRedisScript<>(LUA_DEL_IF_EQUAL, Long.class);

    /**
     * 读取并删除（用于一次性 token 消费：幂等、防重放）。
     *
     * @param key   完整 Redis Key
     * @param token 期望值（null 则只读取不删除）
     * @return 实际值；值不匹配或不存在返回 null
     */
    public String getAndDelete(String key, String token) {
        if (token == null) {
            String v = redisTemplate.opsForValue().get(key);
            redisTemplate.delete(key);
            return v;
        }
        Long n = redisTemplate.execute(DEL_IF_EQUAL_SCRIPT,
                java.util.Collections.singletonList(key), token);
        return n != null && n > 0 ? token : null;
    }
}