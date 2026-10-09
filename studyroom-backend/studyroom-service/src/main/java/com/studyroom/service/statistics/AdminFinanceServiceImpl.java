package com.studyroom.service.statistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.order.OrderRefund;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.admin.AdminRefundRecordVO;
import com.studyroom.entity.vo.statistics.FinanceSummaryVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.order.OrderRefundMapper;
import com.studyroom.mapper.statistics.AdminStatsMapper;
import com.studyroom.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端财务总览服务实现 —— FR-AD-024
 *
 * <h3>口径说明</h3>
 * <ul>
 *   <li>总营收 = pay_time 落在区间的实付合计（与仪表盘/统计口径一致）</li>
 *   <li>总退款 = order_refund.create_time 落在区间的退款金额合计</li>
 *   <li>退款记录金额 = refund_amount（单笔），类型映射 OrderRefund 常量</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFinanceServiceImpl implements AdminFinanceService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int EXPORT_MAX_ROWS = 10000;

    private final AdminStatsMapper statsMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    /* ==================== 财务总览 ==================== */

    @Override
    public FinanceSummaryVO summary(String beginDate, String endDate) {
        LocalDateTime[] range = parseRange(beginDate, endDate);
        FinanceSummaryVO vo = new FinanceSummaryVO();

        vo.setTotalRevenue(safeDecimal(statsMapper.sumRevenue(range[0], range[1])));
        vo.setTotalRefund(safeDecimal(statsMapper.sumRefund(range[0], range[1])));
        vo.setNetRevenue(vo.getTotalRevenue().subtract(vo.getTotalRefund()));
        Long orderCount = statsMapper.countPaidOrders(range[0], range[1]);
        vo.setOrderCount(orderCount == null ? 0L : orderCount);
        if (vo.getOrderCount() > 0) {
            vo.setAvgOrderAmount(vo.getTotalRevenue()
                    .divide(BigDecimal.valueOf(vo.getOrderCount()), 2, RoundingMode.HALF_UP));
        }

        // 按日趋势（营收/退款/净额）
        Map<LocalDate, BigDecimal> revenueMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyRevenue(range[0], range[1])) {
            revenueMap.put(parseDate(row.get("date")), toDecimal(row.get("amount")));
        }
        Map<LocalDate, BigDecimal> refundMap = new HashMap<>();
        for (Map<String, Object> row : statsMapper.dailyRefund(range[0], range[1])) {
            refundMap.put(parseDate(row.get("date")), toDecimal(row.get("amount")));
        }

        LocalDate begin = range[0].toLocalDate();
        LocalDate end = range[1].toLocalDate().minusDays(1);
        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
            FinanceSummaryVO.DailyItem item = new FinanceSummaryVO.DailyItem();
            item.setDate(d.format(DATE));
            BigDecimal revenue = revenueMap.getOrDefault(d, BigDecimal.ZERO);
            BigDecimal refund = refundMap.getOrDefault(d, BigDecimal.ZERO);
            item.setRevenue(revenue);
            item.setRefund(refund);
            item.setNet(revenue.subtract(refund));
            vo.getDailyTrend().add(item);
        }
        return vo;
    }

    /* ==================== 退款记录 ==================== */

    @Override
    public PageResult<AdminRefundRecordVO> pageRefunds(String beginDate, String endDate, Integer type,
                                                       String keyword, long page, long size) {
        Page<OrderRefund> p = orderRefundMapper.selectPage(new Page<>(page, size),
                buildWrapper(beginDate, endDate, type, keyword));
        return new PageResult<>(assemble(p.getRecords()), p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public List<AdminRefundRecordVO> listRefundsForExport(String beginDate, String endDate,
                                                          Integer type, String keyword) {
        List<OrderRefund> refunds = orderRefundMapper.selectList(
                buildWrapper(beginDate, endDate, type, keyword)
                        .orderByDesc(OrderRefund::getCreateTime)
                        .last("LIMIT " + EXPORT_MAX_ROWS));
        if (refunds.size() >= EXPORT_MAX_ROWS) {
            log.warn("[AdminFinance] FR-AD-024 退款导出达到上限 {} 行，已截断", EXPORT_MAX_ROWS);
        }
        return assemble(refunds);
    }

    /* ==================== 内部方法 ==================== */

    private LambdaQueryWrapper<OrderRefund> buildWrapper(String beginDate, String endDate,
                                                         Integer type, String keyword) {
        LambdaQueryWrapper<OrderRefund> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(beginDate)) {
            wrapper.ge(OrderRefund::getCreateTime, LocalDate.parse(beginDate, DATE).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(OrderRefund::getCreateTime,
                    LocalDate.parse(endDate, DATE).atStartOfDay().plusDays(1).minusNanos(1));
        }
        if (type != null) {
            wrapper.eq(OrderRefund::getRefundType, type);
        }
        // 订单号模糊：先解析订单 ID 集合再 IN
        if (StringUtils.hasText(keyword)) {
            List<Long> orderIds = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                            .like(Orders::getOrderNo, keyword.trim())
                            .select(Orders::getId))
                    .stream().map(Orders::getId).collect(Collectors.toList());
            wrapper.in(orderIds.isEmpty() ? OrderRefund::getId : OrderRefund::getOrderId,
                    orderIds.isEmpty() ? Collections.singletonList(-1L) : orderIds);
        }
        wrapper.orderByDesc(OrderRefund::getCreateTime);
        return wrapper;
    }

    private List<AdminRefundRecordVO> assemble(List<OrderRefund> refunds) {
        if (refunds.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量补订单号 + 用户昵称
        Set<Long> orderIds = refunds.stream().map(OrderRefund::getOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Orders> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Orders::getId, Function.identity(), (a, b) -> a));
        Set<Long> userIds = orderMap.values().stream().map(Orders::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<AdminRefundRecordVO> list = new ArrayList<>(refunds.size());
        for (OrderRefund r : refunds) {
            AdminRefundRecordVO vo = new AdminRefundRecordVO();
            vo.setRefundId(r.getId());
            vo.setOrderId(r.getOrderId());
            Orders order = orderMap.get(r.getOrderId());
            vo.setOrderNo(order != null ? order.getOrderNo() : null);
            User user = order != null ? userMap.get(order.getUserId()) : null;
            vo.setNickname(user != null && StringUtils.hasText(user.getNickname())
                    ? user.getNickname() : (order != null ? "用户#" + order.getUserId() : null));
            vo.setType(r.getRefundType());
            vo.setTypeDesc(typeDesc(r.getRefundType()));
            vo.setAmount(r.getRefundAmount());
            vo.setOperatorId(r.getOperatorId());
            vo.setReason(r.getRefundReason());
            vo.setCreateTime(r.getCreateTime() != null ? r.getCreateTime() : r.getRefundTime());
            list.add(vo);
        }
        return list;
    }

    private String typeDesc(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "阶梯取消退款";
            case 2: return "爽约退款";
            case 3: return "申诉退款";
            case 4: return "提前离场退差";
            case 5: return "手动退款";
            default: return "未知";
        }
    }

    private LocalDateTime[] parseRange(String beginDate, String endDate) {
        LocalDate begin = LocalDate.parse(beginDate, DATE);
        LocalDate end = LocalDate.parse(endDate, DATE);
        if (end.isBefore(begin)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        return new LocalDateTime[]{begin.atStartOfDay(), end.plusDays(1).atStartOfDay()};
    }

    private LocalDate parseDate(Object obj) {
        if (obj instanceof java.sql.Date) {
            return ((java.sql.Date) obj).toLocalDate();
        }
        if (obj instanceof LocalDate) {
            return (LocalDate) obj;
        }
        return LocalDate.parse(String.valueOf(obj), DATE);
    }

    private BigDecimal safeDecimal(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal toDecimal(Object obj) {
        return obj == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(obj));
    }
}
