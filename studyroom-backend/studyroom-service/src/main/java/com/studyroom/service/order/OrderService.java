package com.studyroom.service.order;

import com.studyroom.entity.dto.order.CreateOrderDTO;
import com.studyroom.entity.dto.order.PayOrderDTO;
import com.studyroom.entity.vo.order.OrderCreateVO;
import com.studyroom.entity.vo.order.OrderDetailVO;
import com.studyroom.entity.vo.order.OrderPreviewVO;
import com.studyroom.entity.vo.order.UsableCouponVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单服务（用户端核心链路）
 * <p>FR-UC-018 原子锁座、FR-UC-021 冲突校验、FR-UC-022 确认页、FR-UC-023 支付、
 * FR-UC-024 优惠券、FR-UC-027 阶梯退款。</p>
 */
public interface OrderService {

    /**
     * 订单费用预览（FR-UC-022 订单确认页）
     * <p>原价（含加价规则）- 套餐优惠 - 券抵扣 = 实付。</p>
     *
     * @param userId       用户ID
     * @param storeId      门店ID
     * @param seatId       座位ID
     * @param packageId    套餐ID
     * @param startTime    开始时间
     * @param couponClaimId 优惠券领取记录ID（可空）
     */
    OrderPreviewVO preview(Long userId, Long storeId, Long seatId, Long packageId,
                           LocalDateTime startTime, Long couponClaimId);

    /**
     * 用户可用优惠券列表（FR-UC-024，确认页选择）
     */
    List<UsableCouponVO> usableCoupons(Long userId, Long storeId, Long seatId, Long packageId,
                                       LocalDateTime startTime);

    /**
     * 创建订单（FR-UC-018 下单原子锁座）
     * <p>Redisson 分布式锁（秒级持锁）保护"校验 + 创建订单"：
     * 座位可用 → 黑名单 → 用户冲突(FR-UC-021) → 计费 → 绑券 → 落库 →
     * 投递 15 分钟支付超时延迟任务(FR-UC-025)。</p>
     */
    OrderCreateVO createOrder(Long userId, CreateOrderDTO dto, String idempotentKey);

    /**
     * 订单支付（FR-UC-023 支付方式：1余额优先 2模拟微信支付）
     * <p>支付成功后订单 → 已预约，投递"开始后 30 分钟未签到自动取消"任务（FR-UC-026）。</p>
     */
    OrderDetailVO payOrder(Long userId, Long orderId, PayOrderDTO dto);

    /**
     * 用户主动取消订单（FR-UC-027 阶梯退款）
     * <ul>
     *   <li>待支付：直接取消，释放券</li>
     *   <li>已预约且未开始：&gt;60min 全退；15~60min 退 90%；&lt;15min 退 70%</li>
     *   <li>已签到/进行中：不可取消（提示申诉，FR-UC-037 属模块6/7）</li>
     * </ul>
     */
    OrderDetailVO cancelOrder(Long userId, Long orderId);

    /**
     * 订单详情（FR-UC-022 / FR-UC-028）
     */
    OrderDetailVO getOrderDetail(Long userId, Long orderId);

    /**
     * 阶梯退款比例计算（FR-UC-027，供取消与超时任务共用）
     *
     * @param cancelTime 取消时间
     * @param startTime  预约开始时间
     * @return 退款比例（1.0 / 0.9 / 0.7）
     */
    BigDecimal calcRefundRatio(LocalDateTime cancelTime, LocalDateTime startTime);
}
