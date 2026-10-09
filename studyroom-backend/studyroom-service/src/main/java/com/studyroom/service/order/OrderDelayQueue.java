package com.studyroom.service.order;

import com.studyroom.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RDelayedQueue;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 订单延迟队列操作（FR-SY-002 后台调度：Redisson RDelayedQueue）
 * <ul>
 *   <li>FR-UC-025：下单后投递 15 分钟支付超时检查</li>
 *   <li>FR-UC-026：支付成功后投递"开始 + 30 分钟未签到自动取消"检查</li>
 *   <li>FR-UC-033：暂停后投递"暂停 30 分钟未回座自动结算"检查</li>
 * </ul>
 * <p>队列元素格式：{@code PAY:{orderId}} / {@code NOSHOW:{orderId}} /
 * {@code PAUSE:{orderId}:{pauseId}}，到期后转入目标队列由
 * {@link OrderDelayQueueConsumer} 消费。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayQueue {

    /** 支付超时任务前缀（FR-UC-025） */
    public static final String TASK_PAY_TIMEOUT = "PAY";

    /** 未签到取消任务前缀（FR-UC-026） */
    public static final String TASK_NO_SHOW = "NOSHOW";

    /** 暂停超时自动结算任务前缀（FR-UC-033） */
    public static final String TASK_PAUSE_TIMEOUT = "PAUSE";

    private final RDelayedQueue<String> orderDelayedQueue;

    /**
     * 投递支付超时检查任务（FR-UC-025：15 分钟）
     */
    public void offerPayTimeout(Long orderId) {
        offer(TASK_PAY_TIMEOUT, orderId, Duration.ofMinutes(CommonConstants.PAY_TIMEOUT_MINUTES));
    }

    /**
     * 投递未签到自动取消任务（FR-UC-026：开始后 30 分钟）
     *
     * @param checkTime 检查时间点 = 预约开始时间 + 30 分钟
     */
    public void offerNoShowCheck(Long orderId, LocalDateTime checkTime) {
        Duration delay = Duration.between(LocalDateTime.now(), checkTime);
        if (delay.isNegative() || delay.isZero()) {
            delay = Duration.ofSeconds(1);
        }
        offer(TASK_NO_SHOW, orderId, delay);
    }

    private void offer(String type, Long orderId, Duration delay) {
        // RDelayedQueue API 为 (value, delay, TimeUnit)，Duration 需转换为毫秒
        orderDelayedQueue.offer(type + ":" + orderId, delay.toMillis(), TimeUnit.MILLISECONDS);
        log.info("[OrderDelayQueue] 投递延迟任务: type={}, orderId={}, delay={}s",
                type, orderId, delay.getSeconds());
    }

    /**
     * 投递暂停超时自动结算任务（FR-UC-033：暂停 30 分钟未回座）
     * <p>payload 格式 {@code PAUSE:{orderId}:{pauseId}}——携带 pauseId 使超时结算
     * 与暂停记录精确对齐（同一订单可能多次暂停，CAS 按 pauseId 幂等）。</p>
     *
     * @param deadline 暂停截止时间点 = 暂停开始 + 30 分钟
     */
    public void offerPauseTimeout(Long orderId, Long pauseId, LocalDateTime deadline) {
        Duration delay = Duration.between(LocalDateTime.now(), deadline);
        if (delay.isNegative() || delay.isZero()) {
            delay = Duration.ofSeconds(1);
        }
        // RDelayedQueue API 为 (value, delay, TimeUnit)
        orderDelayedQueue.offer(TASK_PAUSE_TIMEOUT + ":" + orderId + ":" + pauseId,
                delay.toMillis(), TimeUnit.MILLISECONDS);
        log.info("[OrderDelayQueue] 投递暂停超时任务: orderId={}, pauseId={}, delay={}s",
                orderId, pauseId, delay.getSeconds());
    }
}
