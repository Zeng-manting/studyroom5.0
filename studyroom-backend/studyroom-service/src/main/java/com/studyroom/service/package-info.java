/**
 * 业务层模块：核心业务逻辑实现。
 * <p>关键业务（见《技术选型与功能汇总.md》2.5 节核心业务流程）：</p>
 * <ul>
 *   <li>下单原子锁座：Redisson 分布式锁保护"校验+创建订单"（FR-UC-018）</li>
 *   <li>签到五重校验：AES 解密 → HMAC 验签 → 有效订单 → 时间窗口 → GPS 距离（FR-UC-030）</li>
 *   <li>阶梯退款：距开始 &gt;60min 全退 / 15~60min 退 90% / &lt;15min 退 70% / 已签到不退（FR-UC-027）</li>
 *   <li>信用限制：30 天内爽约 3 次限制预约 7 天（FR-UC-054）</li>
 *   <li>接口幂等：Redis Token / 业务唯一键</li>
 * </ul>
 */
package com.studyroom.service;
