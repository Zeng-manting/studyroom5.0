package com.studyroom.service.config;

import com.studyroom.common.constants.CommonConstants;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单延迟队列配置（FR-SY-002 / FR-UC-025 / FR-UC-026）
 * <p>RDelayedQueue 基于 Redis 有序结构实现，到期元素自动转入目标阻塞队列；
 * Bean 销毁时 destroy() 只清理本地资源，不丢队列数据。</p>
 */
@Configuration
public class OrderDelayQueueConfig {

    /**
     * 目标队列：到期任务的消费入口（FR-UC-025/026 消费者阻塞 take）
     */
    @Bean
    public RBlockingQueue<String> orderDelayTargetQueue(RedissonClient redissonClient) {
        return redissonClient.getBlockingQueue(CommonConstants.ORDER_DELAY_QUEUE);
    }

    /**
     * 延迟队列：业务侧 offer 延迟任务
     */
    @Bean(destroyMethod = "destroy")
    public RDelayedQueue<String> orderDelayedQueue(RedissonClient redissonClient,
                                                   RBlockingQueue<String> orderDelayTargetQueue) {
        return redissonClient.getDelayedQueue(orderDelayTargetQueue);
    }
}
