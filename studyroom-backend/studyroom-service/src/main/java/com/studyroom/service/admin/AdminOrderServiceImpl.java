package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.common.utils.JacksonUtils;
import com.studyroom.entity.dto.admin.AdminRefundDTO;
import com.studyroom.entity.dto.admin.AppealHandleDTO;
import com.studyroom.entity.order.OrderAppeal;
import com.studyroom.entity.order.OrderRefund;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.pkg.Package;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.admin.AdminAppealListVO;
import com.studyroom.entity.vo.admin.AdminOrderListVO;
import com.studyroom.entity.vo.admin.AdminRefundPreviewVO;
import com.studyroom.mapper.order.OrderAppealMapper;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.order.OrderRefundMapper;
import com.studyroom.mapper.pkg.PackageMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.StoreMapper;
import com.studyroom.mapper.user.UserMapper;
import com.studyroom.service.coupon.CouponService;
import com.studyroom.service.message.MessageService;
import com.studyroom.service.order.OrderService;
import com.studyroom.service.order.WalletService;
import com.studyroom.service.user.CreditService;
import com.studyroom.service.user.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端订单服务实现 —— FR-AD-010 ~ FR-AD-013
 *
 * <h3>统计与查询口径</h3>
 * <ul>
 *   <li>时间筛选按 orders.create_time（下单时间），日期字符串含起止当日全天</li>
 *   <li>座位号/关键字（订单号、昵称）为模糊匹配：先解析出 ID 集合再 IN 查询，
 *       与 MyBatis-Plus Lambda 条件组装一致，避免手写 JOIN SQL</li>
 *   <li>已退金额口径 = orders.refund_amount 累计值（阶梯退款 / 提前离场退差 / 历史手动退款均回写该字段）</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final ObjectMapper MAPPER = JacksonUtils.defaultMapper();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 导出上限（FR-AD-013）：超出截断，避免大结果集拖垮内存 */
    private static final int EXPORT_MAX_ROWS = 10000;

    private final OrderMapper orderMapper;
    private final OrderAppealMapper orderAppealMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final UserMapper userMapper;
    private final SeatMapper seatMapper;
    private final StoreMapper storeMapper;
    private final PackageMapper packageMapper;
    private final WalletService walletService;
    private final MessageService messageService;
    private final EmailService emailService;
    private final CreditService creditService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final TransactionTemplate transactionTemplate;

    /* ==================== FR-AD-010：订单列表 ==================== */

    @Override
    public PageResult<AdminOrderListVO> pageOrders(String beginDate, String endDate, Integer status,
                                                   String seatNo, Long userId, String keyword,
                                                   long page, long size) {
        LambdaQueryWrapper<Orders> wrapper = buildOrderWrapper(beginDate, endDate, status,
                seatNo, userId, keyword);
        Page<Orders> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(assemble(p.getRecords()), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /* ==================== FR-AD-013：订单导出 ==================== */

    @Override
    public List<AdminOrderListVO> listForExport(String beginDate, String endDate, Integer status,
                                                String seatNo, Long userId, String keyword) {
        LambdaQueryWrapper<Orders> wrapper = buildOrderWrapper(beginDate, endDate, status,
                seatNo, userId, keyword)
                .orderByDesc(Orders::getCreateTime)
                .last("LIMIT " + EXPORT_MAX_ROWS);
        List<Orders> orders = orderMapper.selectList(wrapper);
        if (orders.size() >= EXPORT_MAX_ROWS) {
            log.warn("[AdminOrder] FR-AD-013 导出达到上限 {} 行，已截断", EXPORT_MAX_ROWS);
        }
        return assemble(orders);
    }

    /**
     * 组装订单列表/导出共用查询条件（FR-AD-010）
     */
    private LambdaQueryWrapper<Orders> buildOrderWrapper(String beginDate, String endDate, Integer status,
                                                         String seatNo, Long userId, String keyword) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // 时间：按下单时间，含起止当日全天
        if (StringUtils.hasText(beginDate)) {
            wrapper.ge(Orders::getCreateTime, LocalDate.parse(beginDate, DATE).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(Orders::getCreateTime, LocalDate.parse(endDate, DATE).atTime(23, 59, 59));
        }
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(Orders::getUserId, userId);
        }
        // 座位号模糊 → 座位 ID 集合
        if (StringUtils.hasText(seatNo)) {
            List<Long> seatIds = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                            .like(Seat::getSeatNo, seatNo.trim()))
                    .stream().map(Seat::getId).collect(Collectors.toList());
            if (seatIds.isEmpty()) {
                // 无匹配座位：直接返回空结果（用一个不可能成立的 IN 条件短路）
                wrapper.in(Orders::getSeatId, Collections.singletonList(-1L));
            } else {
                wrapper.in(Orders::getSeatId, seatIds);
            }
        }
        // 关键字：订单号 / 用户昵称
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getNickname, kw))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                wrapper.like(Orders::getOrderNo, kw);
            } else {
                wrapper.and(w -> w.like(Orders::getOrderNo, kw).or().in(Orders::getUserId, userIds));
            }
        }
        wrapper.orderByDesc(Orders::getCreateTime);
        return wrapper;
    }

    /**
     * 批量组装 VO：关联用户昵称 / 门店 / 座位 / 套餐
     */
    private List<AdminOrderListVO> assemble(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> userMap = batch(orders.stream().map(Orders::getUserId).collect(Collectors.toSet()),
                ids -> userMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity())));
        Map<Long, Store> storeMap = batch(orders.stream().map(Orders::getStoreId).collect(Collectors.toSet()),
                ids -> storeMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Store::getId, Function.identity())));
        Map<Long, Seat> seatMap = batch(orders.stream().map(Orders::getSeatId).collect(Collectors.toSet()),
                ids -> seatMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Seat::getId, Function.identity())));
        Map<Long, Package> pkgMap = batch(orders.stream().map(Orders::getPackageId).collect(Collectors.toSet()),
                ids -> packageMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Package::getId, Function.identity())));

        return orders.stream().map(o -> {
            AdminOrderListVO vo = new AdminOrderListVO();
            vo.setOrderId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setUserId(o.getUserId());
            User u = userMap.get(o.getUserId());
            vo.setNickname(u == null ? "" : u.getNickname());
            Store s = storeMap.get(o.getStoreId());
            vo.setStoreName(s == null ? "" : s.getName());
            Seat seat = seatMap.get(o.getSeatId());
            vo.setSeatNo(seat == null ? "" : seat.getSeatNo());
            Package pkg = pkgMap.get(o.getPackageId());
            vo.setPackageName(pkg == null ? "" : pkg.getName());
            vo.setStartTime(o.getStartTime());
            vo.setEndTime(o.getEndTime());
            vo.setActualEndTime(o.getActualEndTime());
            vo.setActualDurationMinutes(o.getActualDurationMinutes());
            vo.setPayAmount(o.getPayAmount());
            vo.setRefundAmount(o.getRefundAmount() == null ? BigDecimal.ZERO : o.getRefundAmount());
            vo.setStatus(o.getStatus());
            vo.setStatusDesc(orderStatusDesc(o.getStatus()));
            vo.setPayType(o.getPayType());
            vo.setPayTypeDesc(payTypeDesc(o.getPayType()));
            vo.setNoShow(o.getIsNoShow() != null && o.getIsNoShow() == 1);
            vo.setAppealStatus(o.getAppealStatus());
            vo.setCreateTime(o.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /* ==================== FR-AD-011：申诉处理 ==================== */

    @Override
    public PageResult<AdminAppealListVO> pageAppeals(Integer status, long page, long size) {
        LambdaQueryWrapper<OrderAppeal> wrapper = new LambdaQueryWrapper<OrderAppeal>()
                .eq(status != null, OrderAppeal::getStatus, status)
                .orderByDesc(OrderAppeal::getCreateTime);
        Page<OrderAppeal> p = orderAppealMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(assembleAppeals(p.getRecords()), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AdminAppealListVO getAppealDetail(Long appealId) {
        OrderAppeal appeal = orderAppealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        List<AdminAppealListVO> vos = assembleAppeals(Collections.singletonList(appeal));
        return vos.get(0);
    }

    @Override
    public void approveAppeal(Long appealId, AppealHandleDTO dto, Long operatorId) {
        OrderAppeal appeal = loadPendingAppeal(appealId);
        Orders order = orderMapper.selectById(appeal.getOrderId());
        if (order == null) {
            throw new BusinessException("关联订单不存在");
        }
        String remark = dto == null || !StringUtils.hasText(dto.getRemark())
                ? "申诉通过，已标记签到成功" : dto.getRemark().trim();
        LocalDateTime now = LocalDateTime.now();

        transactionTemplate.executeWithoutResult(tx -> {
            // CAS：申诉 0待审 → 1通过（防并发重复处理）
            int rows = orderAppealMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderAppeal>()
                            .eq(OrderAppeal::getId, appealId)
                            .eq(OrderAppeal::getStatus, OrderAppeal.STATUS_PENDING)
                            .set(OrderAppeal::getStatus, OrderAppeal.STATUS_APPROVED)
                            .set(OrderAppeal::getHandlerId, operatorId)
                            .set(OrderAppeal::getHandleRemark, remark)
                            .set(OrderAppeal::getHandleTime, now));
            if (rows == 0) {
                throw new BusinessException("申诉已被处理，请刷新列表");
            }

            if (order.getStatus() == Orders.STATUS_RESERVED && order.getSignTime() == null) {
                // 语义一：订单仍有效 → 补记签到，转进行中（CAS 防与超时取消/用户取消并发）
                int orderRows = orderMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Orders>()
                                .eq(Orders::getId, order.getId())
                                .eq(Orders::getStatus, Orders.STATUS_RESERVED)
                                .isNull(Orders::getSignTime)
                                .set(Orders::getStatus, Orders.STATUS_IN_USE)
                                .set(Orders::getSignTime, now)
                                .set(Orders::getSignGpsDistance, 0));
                if (orderRows > 0) {
                    // 座位转使用中（best-effort，与 FR-UC-031 签到一致）
                    seatMapper.casUpdateStatus(order.getSeatId(), 0, 1);
                }
            } else if (order.getIsNoShow() != null && order.getIsNoShow() == 1) {
                // 语义二：订单已按爽约自动取消 → 撤销爽约标记 + 恢复信用（订单保持已退款）
                orderMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Orders>()
                                .eq(Orders::getId, order.getId())
                                .eq(Orders::getIsNoShow, 1)
                                .set(Orders::getIsNoShow, 0));
                creditService.restoreNoShowCredit(order, operatorId);
            }
            // 其余状态（已签到/已完成等）：仅申诉通过留痕
        });

        // 事务外通知（best-effort）
        String result;
        if (order.getStatus() == Orders.STATUS_RESERVED && order.getSignTime() == null) {
            result = "申诉已通过，系统已为您补记签到，可正常使用座位。";
        } else if (order.getIsNoShow() != null && order.getIsNoShow() == 1) {
            result = "申诉已通过，该订单的爽约记录已撤销，信用分已恢复。";
        } else {
            result = "您的申诉已通过。";
        }
        messageService.sendOrderMessage(order.getUserId(), "申诉处理结果", result, order.getId());
        log.info("[AdminOrder] FR-AD-011 申诉通过: appealId={}, orderNo={}, operator={}",
                appealId, order.getOrderNo(), operatorId);
    }

    @Override
    public void rejectAppeal(Long appealId, AppealHandleDTO dto, Long operatorId) {
        if (dto == null || !StringUtils.hasText(dto.getRemark())) {
            throw new BusinessException("驳回时必须填写处理意见");
        }
        OrderAppeal appeal = loadPendingAppeal(appealId);
        Orders order = orderMapper.selectById(appeal.getOrderId());
        String remark = dto.getRemark().trim();
        LocalDateTime now = LocalDateTime.now();

        transactionTemplate.executeWithoutResult(tx -> {
            int rows = orderAppealMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderAppeal>()
                            .eq(OrderAppeal::getId, appealId)
                            .eq(OrderAppeal::getStatus, OrderAppeal.STATUS_PENDING)
                            .set(OrderAppeal::getStatus, OrderAppeal.STATUS_REJECTED)
                            .set(OrderAppeal::getHandlerId, operatorId)
                            .set(OrderAppeal::getHandleRemark, remark)
                            .set(OrderAppeal::getHandleTime, now));
            if (rows == 0) {
                throw new BusinessException("申诉已被处理，请刷新列表");
            }
        });

        if (order != null) {
            messageService.sendOrderMessage(order.getUserId(), "申诉处理结果",
                    "您的申诉未通过。处理意见：" + remark, order.getId());
        }
        log.info("[AdminOrder] FR-AD-011 申诉驳回: appealId={}, operator={}", appealId, operatorId);
    }

    /**
     * 加载待审核申诉（不存在 / 已处理均抛业务异常）
     */
    private OrderAppeal loadPendingAppeal(Long appealId) {
        OrderAppeal appeal = orderAppealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        if (appeal.getStatus() != null && appeal.getStatus() != OrderAppeal.STATUS_PENDING) {
            throw new BusinessException("该申诉已处理，请刷新列表");
        }
        return appeal;
    }

    /**
     * 批量组装申诉 VO（FR-AD-011：含订单号/座位/门店/用户昵称 + 凭证图片解析）
     */
    private List<AdminAppealListVO> assembleAppeals(List<OrderAppeal> appeals) {
        if (appeals == null || appeals.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Orders> orderMap = batch(appeals.stream().map(OrderAppeal::getOrderId).collect(Collectors.toSet()),
                ids -> orderMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Orders::getId, Function.identity())));
        Map<Long, User> userMap = batch(appeals.stream().map(OrderAppeal::getUserId).collect(Collectors.toSet()),
                ids -> userMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity())));

        // 订单关联的门店/座位（二级批量）
        Map<Long, Store> storeMap = batch(orderMap.values().stream()
                        .filter(o -> o.getStoreId() != null)
                        .map(Orders::getStoreId).collect(Collectors.toSet()),
                ids -> storeMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Store::getId, Function.identity())));
        Map<Long, Seat> seatMap = batch(orderMap.values().stream()
                        .filter(o -> o.getSeatId() != null)
                        .map(Orders::getSeatId).collect(Collectors.toSet()),
                ids -> seatMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Seat::getId, Function.identity())));

        return appeals.stream().map(a -> {
            AdminAppealListVO vo = new AdminAppealListVO();
            vo.setAppealId(a.getId());
            Orders o = orderMap.get(a.getOrderId());
            vo.setOrderId(a.getOrderId());
            if (o != null) {
                vo.setOrderNo(o.getOrderNo());
                vo.setOrderStatus(o.getStatus());
                vo.setOrderStatusDesc(orderStatusDesc(o.getStatus()));
                vo.setStartTime(o.getStartTime());
                Store s = storeMap.get(o.getStoreId());
                vo.setStoreName(s == null ? "" : s.getName());
                Seat seat = seatMap.get(o.getSeatId());
                vo.setSeatNo(seat == null ? "" : seat.getSeatNo());
            }
            vo.setUserId(a.getUserId());
            User u = userMap.get(a.getUserId());
            vo.setNickname(u == null ? "" : u.getNickname());
            vo.setReasonType(a.getReasonType());
            vo.setReasonTypeDesc(reasonTypeDesc(a.getReasonType()));
            vo.setDescription(a.getDescription());
            vo.setImages(parseImages(a.getImages()));
            vo.setStatus(a.getStatus());
            vo.setStatusDesc(appealStatusDesc(a.getStatus()));
            vo.setHandlerId(a.getHandlerId());
            vo.setHandleRemark(a.getHandleRemark());
            vo.setHandleTime(a.getHandleTime());
            vo.setCreateTime(a.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 凭证图片 JSON 数组字符串 → URL 列表（FR-AD-011 查看凭证图片）
     */
    private List<String> parseImages(String images) {
        if (!StringUtils.hasText(images)) {
            return Collections.emptyList();
        }
        try {
            JsonNode arr = MAPPER.readTree(images);
            List<String> urls = new ArrayList<>();
            if (arr.isArray()) {
                arr.forEach(n -> urls.add(n.asText()));
            }
            return urls;
        } catch (Exception e) {
            log.warn("[AdminOrder] 凭证图片 JSON 解析失败: {}", images);
            return Collections.emptyList();
        }
    }

    /* ==================== FR-AD-012：退款审核 ==================== */

    @Override
    public AdminRefundPreviewVO previewRefund(Long orderId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        BigDecimal payAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        BigDecimal refunded = order.getRefundAmount() == null ? BigDecimal.ZERO : order.getRefundAmount();
        BigDecimal refundable = payAmount.subtract(refunded).max(BigDecimal.ZERO);

        AdminRefundPreviewVO vo = new AdminRefundPreviewVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        User u = userMapper.selectById(order.getUserId());
        vo.setNickname(u == null ? "" : u.getNickname());
        vo.setStatusDesc(orderStatusDesc(order.getStatus()));
        vo.setPayAmount(payAmount);
        vo.setRefundedAmount(refunded);
        vo.setRefundableAmount(refundable);

        // 建议金额规则（FR-AD-012 系统自动计算）
        switch (order.getStatus()) {
            case Orders.STATUS_RESERVED:
                BigDecimal ratio = orderService.calcRefundRatio(LocalDateTime.now(), order.getStartTime());
                BigDecimal suggested = payAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP)
                        .min(refundable);
                vo.setSuggestedAmount(suggested);
                vo.setSuggestionDesc("已预约订单：按 FR-UC-027 阶梯规则建议退 "
                        + ratio.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString()
                        + "%（距开始 " + minutesDesc(order.getStartTime()) + "），可手动调整。");
                vo.setRefundable(refundable.compareTo(BigDecimal.ZERO) > 0);
                break;
            case Orders.STATUS_IN_USE:
                vo.setSuggestedAmount(BigDecimal.ZERO);
                vo.setSuggestionDesc("进行中订单不可直接退款，请等用户结束使用或走提前离场结算（FR-UC-034）。");
                vo.setRefundable(false);
                break;
            case Orders.STATUS_FINISHED:
                vo.setSuggestedAmount(BigDecimal.ZERO);
                vo.setSuggestionDesc("已完成订单属补偿性退款，无自动建议金额，请根据实际情况填写（上限 ¥"
                        + refundable.toPlainString() + "）。");
                vo.setRefundable(refundable.compareTo(BigDecimal.ZERO) > 0);
                break;
            default:
                vo.setSuggestedAmount(BigDecimal.ZERO);
                vo.setSuggestionDesc("当前订单状态（" + orderStatusDesc(order.getStatus())
                        + "）无可退金额。");
                vo.setRefundable(false);
                break;
        }
        return vo;
    }

    @Override
    public void manualRefund(Long orderId, AdminRefundDTO dto, Long operatorId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Orders.STATUS_RESERVED && order.getStatus() != Orders.STATUS_FINISHED) {
            throw new BusinessException("仅「已预约 / 已完成」订单支持手动退款，进行中订单请先结束使用");
        }
        BigDecimal refunded = order.getRefundAmount() == null ? BigDecimal.ZERO : order.getRefundAmount();
        BigDecimal refundable = order.getPayAmount().subtract(refunded);
        if (dto.getAmount().compareTo(refundable) > 0) {
            throw new BusinessException("退款金额超出可退上限 ¥" + refundable.setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();

        transactionTemplate.executeWithoutResult(tx -> {
            // 1. 退款流水（type=5 管理端手动，FR-AD-012）
            OrderRefund refund = new OrderRefund();
            refund.setId(IdGenerator.nextId());
            refund.setOrderId(orderId);
            refund.setRefundAmount(amount);
            refund.setRefundType(OrderRefund.TYPE_MANUAL);
            refund.setRatio(amount.divide(order.getPayAmount(), 4, RoundingMode.HALF_UP));
            refund.setRefundReason(dto.getReason());
            refund.setOperatorId(operatorId);
            refund.setOperatorRemark(dto.getReason());
            refund.setStatus(1);
            refund.setRefundTime(now);
            orderRefundMapper.insert(refund);

            // 2. 订单状态流转（CAS 防并发）
            if (order.getStatus() == Orders.STATUS_RESERVED) {
                // 已预约 → 已退款：整单作废并释放优惠券
                int rows = orderMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Orders>()
                                .eq(Orders::getId, orderId)
                                .eq(Orders::getStatus, Orders.STATUS_RESERVED)
                                .set(Orders::getStatus, Orders.STATUS_REFUNDED)
                                .set(Orders::getCancelTime, now)
                                .set(Orders::getCancelReason, "管理端退款审核：" + dto.getReason())
                                .set(Orders::getRefundAmount, amount)
                                .set(Orders::getRefundTime, now)
                                .set(Orders::getRefundReason, "FR-AD-012 管理端手动退款"));
                if (rows == 0) {
                    throw new BusinessException("订单状态已变更，请刷新后重试");
                }
                couponService.releaseFromOrder(order.getCouponId(), orderId);
            } else {
                // 已完成：保持状态，累计退款额（补偿性部分退款）
                orderMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Orders>()
                                .eq(Orders::getId, orderId)
                                .eq(Orders::getStatus, Orders.STATUS_FINISHED)
                                .set(Orders::getRefundAmount, refunded.add(amount))
                                .set(Orders::getRefundTime, now)
                                .set(Orders::getRefundReason, "FR-AD-012 管理端手动退款"));
            }

            // 3. 退款回余额（与 FR-UC-027 一致：微信支付亦退回余额，真实环境走原路退款）
            walletService.refundToBalance(order.getUserId(), amount, orderId,
                    "管理端退款 " + order.getOrderNo());
        });

        // 事务外通知（FR-UC-045 站内消息 + FR-UC-046 邮件兜底）
        messageService.sendOrderMessage(order.getUserId(), "退款到账",
                "订单 " + order.getOrderNo() + " 的退款 ¥" + amount.toPlainString()
                        + " 已由管理员审核通过并退回您的账户余额。", orderId);
        emailService.sendRefundNotify(order.getUserId(), order.getOrderNo(), amount,
                "管理端退款审核通过：" + dto.getReason());
        log.info("[AdminOrder] FR-AD-012 手动退款: orderNo={}, amount={}, operator={}",
                order.getOrderNo(), amount, operatorId);
    }

    /* ==================== 私有辅助 ==================== */

    private <K, V> Map<K, V> batch(Set<K> ids, Function<Set<K>, Map<K, V>> loader) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return loader.apply(ids);
    }

    private String minutesDesc(LocalDateTime startTime) {
        if (startTime == null) {
            return "未知";
        }
        long minutes = java.time.Duration.between(LocalDateTime.now(), startTime).toMinutes();
        if (minutes > 0) {
            return minutes + " 分钟";
        }
        return "已开始 " + (-minutes) + " 分钟";
    }

    private String orderStatusDesc(Integer status) {
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

    private String payTypeDesc(Integer payType) {
        if (payType == null) {
            return "未支付";
        }
        switch (payType) {
            case 1: return "余额支付";
            case 2: return "微信支付";
            case 3: return "混合支付";
            default: return "未知";
        }
    }

    private String appealStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已驳回";
            default: return "未知";
        }
    }

    private String reasonTypeDesc(String reasonType) {
        if (reasonType == null) {
            return "其他";
        }
        switch (reasonType) {
            case "QR_EXPIRED": return "签到码过期/无效";
            case "GPS_FAIL": return "定位校验失败";
            case "TIME_WINDOW": return "不在签到时间窗口";
            case "ORDER_INVALID": return "订单状态异常";
            default: return "其他";
        }
    }
}
