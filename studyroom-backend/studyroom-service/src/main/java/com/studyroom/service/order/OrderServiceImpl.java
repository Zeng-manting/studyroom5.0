package com.studyroom.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.common.utils.RedisUtils;
import com.studyroom.common.utils.RedissonUtils;
import com.studyroom.entity.dto.order.CreateOrderDTO;
import com.studyroom.entity.dto.order.PayOrderDTO;
import com.studyroom.entity.order.OrderPayment;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.pkg.Package;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.SeatType;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.vo.order.OrderCreateVO;
import com.studyroom.entity.vo.order.OrderDetailVO;
import com.studyroom.entity.vo.order.OrderPreviewVO;
import com.studyroom.entity.vo.order.TimelineVO;
import com.studyroom.entity.vo.order.UsableCouponVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.order.OrderPaymentMapper;
import com.studyroom.mapper.pkg.PackageMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.SeatTypeMapper;
import com.studyroom.mapper.store.StoreMapper;
import com.studyroom.service.coupon.CouponService;
import com.studyroom.service.message.MessageService;
import com.studyroom.service.pkg.PricingService;
import com.studyroom.service.review.ReviewService;
import com.studyroom.service.store.StoreService;
import com.studyroom.service.user.CreditService;
import com.studyroom.service.user.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现（FR-UC-018~028 核心链路）
 *
 * <h3>FR-UC-018 下单原子锁座</h3>
 * <p>Redisson 分布式锁（key = seat:lock:{seatId}，秒级持锁 5s）保护
 * "校验 + 创建订单"临界区；锁内完成座位占用/用户冲突双重校验后事务落库。
 * 座位占用由订单状态保证：待支付(0) 也占用座位 15 分钟（与 FR-UC-025 一致）。</p>
 *
 * <h3>事务边界说明</h3>
 * <p>分布式锁在事务外获取（锁 → 开事务 → 提交 → 释放锁），
 * 避免锁先于事务释放导致并发窗口；使用 TransactionTemplate 编程式事务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StoreMapper storeMapper;
    private final SeatMapper seatMapper;
    private final SeatTypeMapper seatTypeMapper;
    private final PackageMapper packageMapper;
    private final OrderMapper orderMapper;
    private final OrderPaymentMapper orderPaymentMapper;

    private final PricingService pricingService;
    private final CouponService couponService;
    private final WalletService walletService;
    private final StoreService storeService;
    private final OrderDelayQueue delayQueue;
    private final MessageService messageService;
    private final EmailService emailService;
    private final ReviewService reviewService;
    private final CreditService creditService;

    private final RedisUtils redisUtils;
    private final RedissonUtils redissonUtils;
    private final TransactionTemplate transactionTemplate;

    // ==================== FR-UC-022 订单确认页预览 ====================

    @Override
    public OrderPreviewVO preview(Long userId, Long storeId, Long seatId, Long packageId,
                                  LocalDateTime startTime, Long couponClaimId) {
        Seat seat = loadSeat(storeId, seatId);
        Package pkg = loadPackage(packageId);
        Store store = loadStore(storeId);
        SeatType seatType = seatTypeMapper.selectById(seat.getSeatTypeId());

        LocalDateTime endTime = pricingService.calcEndTime(pkg, startTime);
        // 计费：原价（含加价规则）
        BigDecimal originalPrice = pricingService.calcOriginalPrice(pkg, seat.getSeatTypeId(), startTime);
        BigDecimal discountAmount = packageDiscount(pkg);
        BigDecimal amountForCoupon = originalPrice.subtract(discountAmount);

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setStoreName(store.getName());
        vo.setStoreAddress(store.getAddress());
        vo.setSeatNo(seat.getSeatNo());
        vo.setSeatTypeName(seatType == null ? "" : seatType.getName());
        vo.setPackageName(pkg.getName());
        vo.setDurationDesc(pricingService.durationDesc(pkg));
        vo.setStartTime(startTime);
        vo.setEndTime(endTime);
        vo.setOriginalPrice(originalPrice);
        vo.setDiscountAmount(discountAmount);
        // FR-UC-024 券抵扣
        BigDecimal couponAmount = couponClaimId == null ? BigDecimal.ZERO
                : couponService.calcDeductible(couponClaimId, userId, amountForCoupon);
        vo.setCouponAmount(couponAmount);
        if (couponClaimId != null && couponAmount.compareTo(BigDecimal.ZERO) > 0) {
            vo.setCouponName("优惠券抵扣");
        }
        vo.setPayAmount(amountForCoupon.subtract(couponAmount).max(BigDecimal.ZERO));
        vo.setUserBalance(walletService.getBalance(userId));
        return vo;
    }

    @Override
    public List<UsableCouponVO> usableCoupons(Long userId, Long storeId, Long seatId, Long packageId,
                                              LocalDateTime startTime) {
        Seat seat = loadSeat(storeId, seatId);
        Package pkg = loadPackage(packageId);
        BigDecimal originalPrice = pricingService.calcOriginalPrice(pkg, seat.getSeatTypeId(), startTime);
        BigDecimal amountForCoupon = originalPrice.subtract(packageDiscount(pkg));
        return couponService.listUsableCoupons(userId, amountForCoupon);
    }

    // ==================== FR-UC-018 创建订单（原子锁座） ====================

    @Override
    public OrderCreateVO createOrder(Long userId, CreateOrderDTO dto, String idempotentKey) {
        // FR-SY-001 幂等预检：同 token 重复提交直接返回已有订单
        if (idempotentKey != null) {
            Orders exist = orderMapper.selectOne(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getIdempotentKey, idempotentKey).last("LIMIT 1"));
            if (exist != null) {
                log.info("[OrderService] 幂等命中，返回已有订单: {}", exist.getOrderNo());
                return toCreateVO(exist);
            }
        }

        // ---------- 锁外预校验（快速失败，减少锁竞争） ----------
        // FR-UC-054 信用限制拦截：限制期内禁止下单并提示原因（到期自动懒恢复）
        creditService.checkOrderAllowed(userId);
        LocalDateTime startTime = parseAndValidateTime(dto.getStartTime());
        Seat seat = loadSeat(dto.getStoreId(), dto.getSeatId());
        Package pkg = loadPackage(dto.getPackageId());
        LocalDateTime endTime = pricingService.calcEndTime(pkg, startTime);

        // FR-UC-020 黑名单校验（后端兜底，前端已置灰）
        if (storeService.isBlocked(dto.getStoreId(), startTime.toLocalDate(),
                startTime.toLocalTime(), endTime.toLocalTime())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "所选时段门店闭店休息，请更换时段");
        }

        // ---------- FR-UC-018 Redisson 分布式锁：秒级持锁保护"校验+创建" ----------
        String lockKey = CommonConstants.SEAT_LOCK_KEY + dto.getSeatId();
        try (RedissonUtils.RLockWrapper lock = redissonUtils.tryLock(lockKey, Duration.ofSeconds(5))) {
            if (lock == null) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "该座位正在被其他用户下单，请稍后重试");
            }

            // 锁内双重校验
            if (orderMapper.countSeatOverlap(dto.getSeatId(), startTime, endTime) > 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "手慢了，该座位在所选时段刚被预约");
            }
            // FR-UC-021 预约冲突：同一用户同一时段不可重复预约
            if (orderMapper.countUserOverlap(userId, startTime, endTime) > 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "您在该时段已有预约，请先取消原有预约");
            }

            // 计费
            BigDecimal originalPrice = pricingService.calcOriginalPrice(pkg, seat.getSeatTypeId(), startTime);
            BigDecimal discountAmount = packageDiscount(pkg);
            BigDecimal amountForCoupon = originalPrice.subtract(discountAmount);

            // 事务：绑券 + 落库（锁内提交后释放锁，无并发窗口）
            return transactionTemplate.execute(txStatus -> doCreateOrder(
                    userId, dto, seat, pkg, startTime, endTime,
                    originalPrice, discountAmount, amountForCoupon, idempotentKey));
        }
    }

    /**
     * 事务体：绑定优惠券（CAS）→ 生成订单号 → 落库 → 投递支付超时任务
     */
    private OrderCreateVO doCreateOrder(Long userId, CreateOrderDTO dto, Seat seat, Package pkg,
                                        LocalDateTime startTime, LocalDateTime endTime,
                                        BigDecimal originalPrice, BigDecimal discountAmount,
                                        BigDecimal amountForCoupon, String idempotentKey) {
        Long orderId = IdGenerator.nextId();

        // FR-UC-024 绑定优惠券（CAS 防并发重复用券；orders.coupon_id 存领取记录ID）
        BigDecimal couponAmount = BigDecimal.ZERO;
        if (dto.getCouponClaimId() != null) {
            couponAmount = couponService.bindToOrder(dto.getCouponClaimId(), userId, orderId, amountForCoupon);
        }
        BigDecimal payAmount = amountForCoupon.subtract(couponAmount).max(BigDecimal.ZERO);

        Orders order = new Orders();
        order.setId(orderId);
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStoreId(dto.getStoreId());
        order.setSeatId(seat.getId());
        order.setSeatTypeId(seat.getSeatTypeId());
        order.setPackageId(pkg.getId());
        order.setStartTime(startTime);
        order.setEndTime(endTime);
        order.setOriginalPrice(originalPrice);
        order.setDiscountAmount(discountAmount);
        order.setCouponId(dto.getCouponClaimId());
        order.setCouponAmount(couponAmount);
        order.setPayAmount(payAmount);
        order.setStatus(Orders.STATUS_PENDING_PAY);
        order.setIsNoShow(0);
        order.setAppealStatus(0);
        order.setExtendCount(0);
        order.setPauseCount(0);
        order.setRemark(dto.getRemark());
        order.setIdempotentKey(idempotentKey);
        orderMapper.insert(order);

        // FR-UC-025 投递 15 分钟支付超时任务（延迟队列，FR-SY-002）
        delayQueue.offerPayTimeout(orderId);

        OrderCreateVO vo = toCreateVO(order);
        log.info("[OrderService] FR-UC-018 创建订单成功: orderNo={}, seatId={}, {} ~ {}",
                order.getOrderNo(), seat.getId(), startTime, endTime);
        return vo;
    }

    // ==================== FR-UC-023 支付 ====================

    @Override
    public OrderDetailVO payOrder(Long userId, Long orderId, PayOrderDTO dto) {
        Orders order = loadOwnedOrder(userId, orderId);
        if (order.getStatus() != Orders.STATUS_PENDING_PAY) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态已变更，请刷新后重试");
        }
        // FR-UC-025 超时窗口校验（延迟任务可能存在秒级延迟，此处兜底）
        LocalDateTime deadline = order.getCreateTime().plusMinutes(CommonConstants.PAY_TIMEOUT_MINUTES);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "支付超时，订单已自动取消");
        }

        int payType = dto.getPayType();
        if (payType != Orders.PAY_TYPE_BALANCE && payType != Orders.PAY_TYPE_WECHAT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的支付方式");
        }

        // 事务：扣余额/生成交易号 → 支付记录 → CAS 0→1
        // 若 CAS 失败（超时任务恰好取消），事务回滚自动恢复余额
        transactionTemplate.executeWithoutResult(txStatus -> doPay(order, payType));

        // 事务提交后：补记券使用时间 + 投递未签到检查任务（FR-UC-026）
        couponService.markPaid(order.getCouponId(), orderId);
        delayQueue.offerNoShowCheck(orderId,
                order.getStartTime().plusMinutes(CommonConstants.NO_SHOW_CANCEL_MINUTES));

        // FR-UC-045 消息中心推送 + FR-UC-046 邮件兜底（预约成功关键节点）
        messageService.sendOrderMessage(userId, "预约成功",
                "订单 " + order.getOrderNo() + " 支付成功，预约时段 "
                        + order.getStartTime().format(TIME_FORMATTER) + " ~ "
                        + order.getEndTime().format(TIME_FORMATTER)
                        + "，请准时到店扫码签到。", orderId);
        emailService.sendOrderSuccessNotify(userId, order.getOrderNo(),
                order.getStartTime().format(TIME_FORMATTER) + " ~ "
                        + order.getEndTime().format(TIME_FORMATTER));

        Orders paid = orderMapper.selectById(orderId);
        return toDetailVO(paid);
    }

    /**
     * 支付事务体
     */
    private void doPay(Orders order, int payType) {
        // FR-UC-023 支付方式
        String transactionId = null;
        if (payType == Orders.PAY_TYPE_BALANCE) {
            // 余额支付（原子扣减，不足抛异常回滚）
            walletService.payFromBalance(order.getUserId(), order.getPayAmount(), order.getId(), order.getOrderNo());
        } else {
            // 模拟微信支付：生成本地交易号（真实环境替换为 wxpay 统一下单 + 支付回调）
            transactionId = "WXMOCK" + IdGenerator.nextIdStr();
        }

        // 支付记录
        OrderPayment payment = new OrderPayment();
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPayType(payType);
        payment.setPayAmount(order.getPayAmount());
        payment.setTransactionId(transactionId);
        payment.setPayStatus(1);
        payment.setRemark(payType == Orders.PAY_TYPE_BALANCE ? "余额支付" : "模拟微信支付");
        orderPaymentMapper.insert(payment);

        // CAS：0待支付 → 1已预约（失败抛异常 → 事务回滚 → 余额自动恢复）
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                .set(Orders::getStatus, Orders.STATUS_RESERVED)
                .set(Orders::getPayType, payType)
                .set(Orders::getPayTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "订单已被超时取消，支付金额将自动退回（余额支付）/ 交易自动关闭（微信支付）");
        }
    }

    // ==================== FR-UC-027 取消订单（阶梯退款） ====================

    @Override
    public OrderDetailVO cancelOrder(Long userId, Long orderId) {
        Orders order = loadOwnedOrder(userId, orderId);

        // 待支付：直接取消（无退款）
        if (order.getStatus() == Orders.STATUS_PENDING_PAY) {
            int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                    .eq(Orders::getId, orderId)
                    .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                    .set(Orders::getStatus, Orders.STATUS_CANCELED)
                    .set(Orders::getCancelTime, LocalDateTime.now())
                    .set(Orders::getCancelReason, "用户取消（未支付）"));
            if (rows > 0) {
                couponService.releaseFromOrder(order.getCouponId(), orderId);
            }
            return toDetailVO(orderMapper.selectById(orderId));
        }

        // 已预约：阶梯退款
        if (order.getStatus() == Orders.STATUS_RESERVED) {
            if (order.getSignTime() != null) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "已签到订单不可取消，如需退款请提交申诉");
            }
            BigDecimal ratio = calcRefundRatio(LocalDateTime.now(), order.getStartTime());
            BigDecimal refund = order.getPayAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            transactionTemplate.executeWithoutResult(txStatus -> {
                // CAS：1 → 6 已退款
                int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                        .eq(Orders::getId, orderId)
                        .eq(Orders::getStatus, Orders.STATUS_RESERVED)
                        .set(Orders::getStatus, Orders.STATUS_REFUNDED)
                        .set(Orders::getCancelTime, LocalDateTime.now())
                        .set(Orders::getCancelReason, "用户取消，阶梯退款"
                                + ratio.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%")
                        .set(Orders::getRefundAmount, refund)
                        .set(Orders::getRefundTime, LocalDateTime.now())
                        .set(Orders::getRefundReason, "FR-UC-027 阶梯退款"));
                if (rows == 0) {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态已变更，请刷新后重试");
                }
                // FR-UC-027 退款回余额（微信支付亦退回余额，真实环境走原路退款）
                walletService.refundToBalance(userId, refund, orderId,
                        "取消订单退款 " + order.getOrderNo());
                // 释放优惠券
                couponService.releaseFromOrder(order.getCouponId(), orderId);
            });
            // FR-UC-045 消息推送 + FR-UC-046 邮件兜底（退款到账关键节点）
            messageService.sendOrderMessage(userId, "退款到账",
                    "订单 " + order.getOrderNo() + " 已取消，退款 ¥" + refund
                            + " 已退回账户余额。", orderId);
            emailService.sendRefundNotify(userId, order.getOrderNo(), refund, "用户取消订单（阶梯退款）");
            return toDetailVO(orderMapper.selectById(orderId));
        }

        throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前订单状态不支持取消");
    }

    // ==================== FR-UC-022 / FR-UC-028 订单详情 ====================

    @Override
    public OrderDetailVO getOrderDetail(Long userId, Long orderId) {
        Orders order = loadOwnedOrder(userId, orderId);
        OrderDetailVO vo = toDetailVO(order);

        // FR-UC-038 模块7扩展：签到记录 + 时间轴 + 评价状态
        vo.setSignGpsDistance(order.getSignGpsDistance());
        vo.setActualDurationMinutes(order.getActualDurationMinutes());
        vo.setNoShow(order.getIsNoShow() != null && order.getIsNoShow() == 1);
        vo.setAppealStatus(order.getAppealStatus());
        vo.setTimeline(buildTimeline(order));
        if (order.getStatus() == Orders.STATUS_FINISHED) {
            vo.setReviewed(reviewService.hasReviewed(orderId));
        }
        return vo;
    }

    /**
     * 订单状态时间轴（FR-UC-038）：按发生时间正序组装节点
     */
    private List<TimelineVO> buildTimeline(Orders order) {
        List<TimelineVO> timeline = new ArrayList<>();
        timeline.add(node("create", "创建订单", "订单号 " + order.getOrderNo(),
                order.getCreateTime(), true));
        if (order.getPayTime() != null) {
            String payTypeDesc = order.getPayType() == null ? ""
                    : (order.getPayType() == Orders.PAY_TYPE_BALANCE ? "（余额支付）" : "（微信支付）");
            timeline.add(node("pay", "支付成功", "实付 ¥" + order.getPayAmount() + payTypeDesc,
                    order.getPayTime(), true));
        }
        if (order.getSignTime() != null) {
            String gpsDesc = order.getSignGpsDistance() != null
                    ? "GPS 距门店 " + order.getSignGpsDistance() + " 米" : "";
            timeline.add(node("sign", "扫码签到", gpsDesc, order.getSignTime(), true));
        }
        if (order.getPauseCount() != null && order.getPauseCount() > 0) {
            String pauseDesc = order.getPauseStartTime() != null
                    ? "当前暂停中（第 " + order.getPauseCount() + " 次）"
                    : "累计暂停 " + order.getPauseCount() + " 次";
            timeline.add(node("pause", "临时离座", pauseDesc,
                    order.getPauseStartTime() != null ? order.getPauseStartTime() : order.getPayTime(),
                    order.getPauseStartTime() == null));
        }
        if (order.getActualEndTime() != null) {
            timeline.add(node("end", "使用结束",
                    "实际使用 " + order.getActualDurationMinutes() + " 分钟"
                            + (order.getRefundAmount() != null && order.getRefundAmount()
                                    .compareTo(BigDecimal.ZERO) > 0
                            ? "，退差额 ¥" + order.getRefundAmount() : ""),
                    order.getActualEndTime(), true));
        }
        if (order.getCancelTime() != null) {
            timeline.add(node("cancel", "订单取消",
                    order.getCancelReason() == null ? "" : order.getCancelReason(),
                    order.getCancelTime(), true));
        }
        if (order.getRefundTime() != null) {
            timeline.add(node("refund", "退款到账",
                    "退款 ¥" + order.getRefundAmount() + " 已退回账户余额",
                    order.getRefundTime(), true));
        }
        return timeline;
    }

    private TimelineVO node(String type, String title, String desc, LocalDateTime time, boolean done) {
        TimelineVO node = new TimelineVO();
        node.setType(type);
        node.setTitle(title);
        node.setDesc(desc);
        node.setTime(time);
        node.setDone(done);
        return node;
    }

    // ==================== FR-UC-027 阶梯退款比例 ====================

    @Override
    public BigDecimal calcRefundRatio(LocalDateTime cancelTime, LocalDateTime startTime) {
        long minutes = Duration.between(startTime, cancelTime).toMinutes();
        long before = minutes < 0 ? -minutes : 0; // 提前取消的分钟数
        if (before > CommonConstants.REFUND_FULL_MINUTES) {
            return BigDecimal.ONE;                                  // >60 分钟：全退
        } else if (before >= 15) {
            return CommonConstants.REFUND_RATIO_90;                 // 15~60 分钟：退 90%
        } else {
            return CommonConstants.REFUND_RATIO_70;                 // <15 分钟（含已开始）：退 70%
        }
    }

    // ==================== 私有辅助 ====================

    /**
     * 解析并校验开始时间（FR-UC-016 30 分钟粒度）
     */
    private LocalDateTime parseAndValidateTime(String time) {
        LocalDateTime start;
        try {
            start = LocalDateTime.parse(time, TIME_FORMATTER);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "时间格式错误，应为 yyyy-MM-dd HH:mm");
        }
        if (start.getMinute() % 30 != 0 || start.getSecond() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始时间须为 30 分钟粒度（如 08:00 / 08:30）");
        }
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始时间必须晚于当前时间");
        }
        if (start.isAfter(LocalDateTime.now().plusDays(30))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持预约未来 30 天内的时段");
        }
        return start;
    }

    private Seat loadSeat(Long storeId, Long seatId) {
        Seat seat = seatMapper.selectById(seatId);
        // @TableLogic 已过滤已删除记录，deleted 字段 select=false 不返回，不可再判 getDeleted()
        if (seat == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "座位不存在");
        }
        if (!seat.getStoreId().equals(storeId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "座位不属于该门店");
        }
        // FR-UC-017：维护(2)/打扫(3)/锁定(4) 不可选
        if (seat.getStatus() != null && seat.getStatus() >= 2) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该座位暂不可预约（维护/打扫/锁定中）");
        }
        return seat;
    }

    private Package loadPackage(Long packageId) {
        Package pkg = packageMapper.selectById(packageId);
        if (pkg == null || pkg.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "套餐不存在或已下架");
        }
        return pkg;
    }

    private Store loadStore(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        // @TableLogic 已过滤已删除记录，deleted 字段 select=false 不返回，不可再判 getDeleted()
        if (store == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "门店不存在");
        }
        return store;
    }

    private Orders loadOwnedOrder(Long userId, Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        // @TableLogic 已过滤已删除记录，deleted 字段 select=false 不返回，不可再判 getDeleted()
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人订单");
        }
        return order;
    }

    /**
     * 订单号：yyyyMMdd + 6 位日自增序号（Redis INCR，FR-UC-018）
     */
    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long seq = redisUtils.incr(CommonConstants.ORDER_NO_SEQ_KEY + datePart);
        // 首次自增设置 2 天过期
        if (seq != null && seq == 1L) {
            redisUtils.expire(CommonConstants.ORDER_NO_SEQ_KEY + datePart, 2, TimeUnit.DAYS);
        }
        return datePart + String.format("%06d", seq % 1000000L);
    }

    /**
     * 套餐划线优惠 = max(0, 划线原价 - 售价)
     */
    private BigDecimal packageDiscount(Package pkg) {
        if (pkg.getOriginalPrice() == null) {
            return BigDecimal.ZERO;
        }
        return pkg.getOriginalPrice().subtract(pkg.getPrice()).max(BigDecimal.ZERO);
    }

    private OrderCreateVO toCreateVO(Orders order) {
        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setPayAmount(order.getPayAmount());
        vo.setOriginalPrice(order.getOriginalPrice());
        vo.setCouponAmount(order.getCouponAmount());
        // FR-UC-025 支付截止时间
        vo.setPayDeadline(order.getCreateTime().plusMinutes(CommonConstants.PAY_TIMEOUT_MINUTES));
        return vo;
    }

    private OrderDetailVO toDetailVO(Orders order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(statusDesc(order.getStatus()));
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        vo.setOriginalPrice(order.getOriginalPrice());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setRefundAmount(order.getRefundAmount());
        vo.setCreateTime(order.getCreateTime());
        if (order.getStatus() == Orders.STATUS_PENDING_PAY) {
            vo.setPayDeadline(order.getCreateTime().plusMinutes(CommonConstants.PAY_TIMEOUT_MINUTES));
        }

        // 关联信息
        Store store = storeMapper.selectById(order.getStoreId());
        if (store != null) {
            vo.setStoreName(store.getName());
            vo.setStoreAddress(store.getAddress());
        }
        Seat seat = seatMapper.selectById(order.getSeatId());
        if (seat != null) {
            vo.setSeatNo(seat.getSeatNo());
            SeatType type = seatTypeMapper.selectById(seat.getSeatTypeId());
            vo.setSeatTypeName(type == null ? "" : type.getName());
        }
        Package pkg = packageMapper.selectById(order.getPackageId());
        vo.setPackageName(pkg == null ? "" : pkg.getName());
        return vo;
    }

    private String statusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "待支付";
            case 1: return "已预约";
            case 2: return "进行中";
            case 3: return "已完成";
            case 4: return "已取消";
            case 5: return "退款中";
            case 6: return "已退款";
            case 7: return "申诉中";
            default: return "未知";
        }
    }
}
