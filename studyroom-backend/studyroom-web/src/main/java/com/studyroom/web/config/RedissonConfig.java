package com.studyroom.web.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置 —— FR-UC-018 / FR-SY-001 / FR-SY-002
 *
 * <p>Redisson 3.23.4，单机模式，复用 spring.redis 连接配置。承担：</p>
 * <ul>
 *   <li>RLock 分布式锁：下单原子锁座（FR-UC-018）</li>
 *   <li>RRateLimiter 限流：登录/下单/签到/评价提交等核心接口（FR-SY-001）</li>
 *   <li>RDelayedQueue 延迟队列：支付超时取消、未签到取消、暂停超时结算（FR-SY-002）</li>
 * </ul>
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private String redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setDatabase(redisDatabase)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                // 连接池与超时（单店场景：座位 ≤ 200，高峰在线 ≤ 300，下单峰值 QPS ≤ 50）
                .setConnectionPoolSize(32)
                .setConnectionMinimumIdleSize(8)
                .setConnectTimeout(10000)
                .setTimeout(3000);
        return Redisson.create(config);
    }
}
