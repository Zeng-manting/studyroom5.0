package com.studyroom.service.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.result.PageResult;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.pkg.Package;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.vo.order.OrderListVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.pkg.PackageMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.StoreMapper;
import com.studyroom.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单查询服务实现（FR-UC-035/036/037）
 * <p>与写路径（OrderService）分离，专注列表/详情组装；
 * actions 由后端统一计算，前端零逻辑渲染动态按钮。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderMapper orderMapper;
    private final StoreMapper storeMapper;
    private final SeatMapper seatMapper;
    private final PackageMapper packageMapper;
    private final ReviewService reviewService;

    @Override
    public PageResult<OrderListVO> pageOrders(Long userId, int tab, long page, long size) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreateTime);

        // FR-UC-035 六 Tab 状态映射
        switch (tab) {
            case 1: // 待支付
                wrapper.eq(Orders::getStatus, Orders.STATUS_PENDING_PAY);
                break;
            case 2: // 已预约
                wrapper.eq(Orders::getStatus, Orders.STATUS_RESERVED);
                break;
            case 3: // 进行中
                wrapper.eq(Orders::getStatus, Orders.STATUS_IN_USE);
                break;
            case 4: // 已完成
                wrapper.eq(Orders::getStatus, Orders.STATUS_FINISHED);
                break;
            case 5: // 已取消（含退款中/已退款/申诉中）
                wrapper.in(Orders::getStatus, Arrays.asList(
                        Orders.STATUS_CANCELED, Orders.STATUS_REFUNDING,
                        Orders.STATUS_REFUNDED, Orders.STATUS_APPEALING));
                break;
            case 0: // 全部
            default:
                break;
        }

        Page<Orders> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        List<Orders> orders = p.getRecords();

        // 批量关联：门店/座位/套餐
        Map<Long, Store> storeMap = batchQuery(orders.stream().map(Orders::getStoreId).collect(Collectors.toSet()),
                ids -> storeMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Store::getId, Function.identity())));
        Map<Long, Seat> seatMap = batchQuery(orders.stream().map(Orders::getSeatId).collect(Collectors.toSet()),
                ids -> seatMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Seat::getId, Function.identity())));
        Map<Long, Package> pkgMap = batchQuery(orders.stream().map(Orders::getPackageId).collect(Collectors.toSet()),
                ids -> packageMapper.selectBatchIds(ids).stream()
                        .collect(Collectors.toMap(Package::getId, Function.identity())));

        // FR-UC-037/039：已完成订单批量查"是否已评价"
        Set<Long> reviewedOrderIds = new HashSet<>();
        List<Long> finishedIds = orders.stream()
                .filter(o -> o.getStatus() == Orders.STATUS_FINISHED)
                .map(Orders::getId).collect(Collectors.toList());
        for (Long orderId : finishedIds) {
            if (reviewService.hasReviewed(orderId)) {
                reviewedOrderIds.add(orderId);
            }
        }

        List<OrderListVO> vos = new ArrayList<>();
        for (Orders o : orders) {
            vos.add(toListVO(o, storeMap.get(o.getStoreId()), seatMap.get(o.getSeatId()),
                    pkgMap.get(o.getPackageId()), reviewedOrderIds.contains(o.getId())));
        }
        return new PageResult<>(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /* ==================== 私有辅助 ==================== */

    private <K, V> Map<K, V> batchQuery(Set<K> ids, Function<Set<K>, Map<K, V>> loader) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return loader.apply(ids);
    }

    /**
     * 列表 VO 组装：倒计时字段（FR-UC-036）+ 动态按钮（FR-UC-037）
     */
    private OrderListVO toListVO(Orders o, Store store, Seat seat, Package pkg, boolean reviewed) {
        OrderListVO vo = new OrderListVO();
        vo.setOrderId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setStatus(o.getStatus());
        vo.setStatusDesc(statusDesc(o.getStatus()));
        vo.setStoreName(store == null ? "" : store.getName());
        vo.setSeatNo(seat == null ? "" : seat.getSeatNo());
        vo.setPackageName(pkg == null ? "" : pkg.getName());
        vo.setStartTime(o.getStartTime());
        vo.setEndTime(o.getEndTime());
        vo.setPayAmount(o.getPayAmount());
        vo.setRefundAmount(o.getRefundAmount());
        vo.setSignTime(o.getSignTime());
        vo.setActualEndTime(o.getActualEndTime());
        vo.setNoShow(o.getIsNoShow() != null && o.getIsNoShow() == 1);
        vo.setReviewed(reviewed);
        vo.setCreateTime(o.getCreateTime());

        // FR-UC-036 倒计时三态：待支付(红)=支付截止；已预约(蓝)=开始时间；进行中(绿)=结束时间
        if (o.getStatus() == Orders.STATUS_PENDING_PAY && o.getCreateTime() != null) {
            vo.setPayDeadline(o.getCreateTime().plusMinutes(CommonConstants.PAY_TIMEOUT_MINUTES));
        }

        vo.setActions(buildActions(o, reviewed));
        return vo;
    }

    /**
     * FR-UC-037 按状态动态生成操作按钮：
     * 待支付→支付/取消；已预约→签到/取消；进行中→暂停/恢复/结束；已完成→评价(未评)/再次预约；爽约→申诉
     */
    private List<String> buildActions(Orders o, boolean reviewed) {
        List<String> actions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        switch (o.getStatus()) {
            case Orders.STATUS_PENDING_PAY:
                actions.add("pay");
                actions.add("cancel");
                break;
            case Orders.STATUS_RESERVED:
                // 签到按钮：时间窗内（开始前后 15 分钟）
                if (o.getSignTime() == null && o.getStartTime() != null
                        && !now.isBefore(o.getStartTime().minusMinutes(15))
                        && !now.isAfter(o.getStartTime().plusMinutes(15))) {
                    actions.add("sign");
                }
                actions.add("cancel");
                break;
            case Orders.STATUS_IN_USE:
                actions.add(o.getPauseStartTime() != null ? "resume" : "pause");
                actions.add("end");
                break;
            case Orders.STATUS_FINISHED:
                if (!reviewed) {
                    actions.add("review");
                }
                break;
            case Orders.STATUS_CANCELED:
            case Orders.STATUS_REFUNDED:
                // 爽约订单提供申诉入口（FR-UC-032）
                if (o.getIsNoShow() != null && o.getIsNoShow() == 1
                        && (o.getAppealStatus() == null || o.getAppealStatus() == 0)) {
                    actions.add("appeal");
                }
                break;
            default:
                break;
        }
        return actions;
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
