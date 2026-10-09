package com.studyroom.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 订单延迟队列消费者（FR-SY-002）
 * <p>双保险消费：
 * <ul>
 *   <li>常驻单线程阻塞 take（秒级实时性）</li>
 *   <li>@Scheduled 每分钟兜底 poll（防止极端情况下线程异常退出后任务滞留）</li>
 * </ul>
 * 应用重启后，Redis 中未到期的延迟任务不丢失；已到期的会在启动后立即被消费。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayQueueConsumer {

    private final RBlockingQueue<String> orderDelayTargetQueue;
    private final OrderTimeoutTask orderTimeoutTask;

    private ExecutorService executor;
    private volatile boolean running = false;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        running = true;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "order-delay-consumer");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::loop);
        log.info("[OrderDelayQueueConsumer] 延迟队列消费线程已启动");
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 常驻阻塞消费
     */
    private void loop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String task = orderDelayTargetQueue.take();
                handle(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 单条任务异常不影响消费循环（FR-SY-003 日志留痕）
                log.error("[OrderDelayQueueConsumer] 任务处理异常", e);
            }
        }
    }

    /**
     * 兜底拉取（每分钟）：极端情况下常驻线程失效时的保险
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 90_000)
    public void pollFallback() {
        try {
            String task;
            while ((task = orderDelayTargetQueue.poll()) != null) {
                handle(task);
            }
        } catch (Exception e) {
            log.error("[OrderDelayQueueConsumer] 兜底拉取异常", e);
        }
    }

    /**
     * 任务分发：PAY:{orderId} → 支付超时（FR-UC-025）；NOSHOW:{orderId} → 未签到取消（FR-UC-026）；
     * PAUSE:{orderId}:{pauseId} → 暂停超时自动结算（FR-UC-033）
     */
    private void handle(String task) {
        log.info("[OrderDelayQueueConsumer] 收到延迟任务: {}", task);
        try {
            String[] parts = task.split(":");
            String type = parts[0];
            if (OrderDelayQueue.TASK_PAY_TIMEOUT.equals(type)) {
                orderTimeoutTask.handlePayTimeout(Long.valueOf(parts[1]));
            } else if (OrderDelayQueue.TASK_NO_SHOW.equals(type)) {
                orderTimeoutTask.handleNoShowCancel(Long.valueOf(parts[1]));
            } else if (OrderDelayQueue.TASK_PAUSE_TIMEOUT.equals(type)) {
                // PAUSE:{orderId}:{pauseId}，三元结构
                orderTimeoutTask.handlePauseTimeout(Long.valueOf(parts[1]), Long.valueOf(parts[2]));
            } else {
                log.warn("[OrderDelayQueueConsumer] 未知任务类型: {}", task);
            }
        } catch (Exception e) {
            log.error("[OrderDelayQueueConsumer] 任务解析/执行失败: {}", task, e);
        }
    }
}
