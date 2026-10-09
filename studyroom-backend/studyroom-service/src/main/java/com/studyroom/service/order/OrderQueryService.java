package com.studyroom.service.order;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.vo.order.OrderListVO;

/**
 * 订单查询服务（模块7：订单管理）
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-035 订单列表：6 个 Tab（全部/待支付/已预约/进行中/已完成/已取消）</li>
 *   <li>FR-UC-036 倒计时字段：待支付(支付截止)/已预约(开始时间)/进行中(结束时间)</li>
 *   <li>FR-UC-037 操作按钮：按状态计算 actions（pay/cancel/sign/pause/resume/end/review/appeal）</li>
 *   <li>FR-UC-038 订单详情：状态时间轴 + 签到记录</li>
 * </ul>
 */
public interface OrderQueryService {

    /**
     * 订单分页列表（FR-UC-035）
     *
     * @param userId 用户ID
     * @param tab    Tab：0全部 1待支付 2已预约 3进行中 4已完成 5已取消（含退款/申诉）
     * @param page   页码（从1开始）
     * @param size   每页条数
     */
    PageResult<OrderListVO> pageOrders(Long userId, int tab, long page, long size);
}
