package com.studyroom.service.user;

import com.studyroom.entity.order.Orders;
import com.studyroom.entity.vo.user.CreditVO;

/**
 * 信用服务（FR-UC-053 爽约记录 / FR-UC-054 信用限制）
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>爽约触发点：FR-UC-026 未签到自动取消（OrderTimeoutTask）成功后调用 {@link #recordNoShow}</li>
 *   <li>下单拦截：FR-UC-054 限制期内 createOrder 前调用 {@link #checkOrderAllowed}，拦截并提示原因</li>
 *   <li>到期自动恢复：懒恢复模式 —— 不依赖定时任务，任何信用读取/下单校验时发现
 *       credit_limit_end 已过即 CAS 恢复（信用状态流转低频，懒恢复足够精确且零调度成本）</li>
 *   <li>管理端手动解除（FR-UC-054）：{@link #adminRemoveLimit} 供模块9 管理端调用</li>
 * </ul>
 *
 * @author studyroom
 */
public interface CreditService {

    /**
     * 记录一次爽约（FR-UC-053）
     * <p>由未签到自动取消任务在 CAS 取消成功后调用（失败说明已被并发处理，天然幂等）：
     * 落爽约流水 + 信用分扣减 + 实时统计 30 天爽约次数，达上限（3 次）自动置 7 天预约限制。</p>
     *
     * @param order 被自动取消的爽约订单
     */
    void recordNoShow(Orders order);

    /**
     * 下单信用校验（FR-UC-054 限制期内下单拦截）
     * <p>限制中 → 抛业务异常并提示剩余天数；限制已到期 → 懒恢复（CAS 解除 + 恢复流水）后放行。</p>
     *
     * @param userId 用户ID
     */
    void checkOrderAllowed(Long userId);

    /**
     * 我的信用页（FR-UC-053/054 用户端展示）
     * <p>信用分、限制状态与剩余天数、30 天爽约次数、近期爽约订单、信用流水。</p>
     */
    CreditVO getMyCredit(Long userId);

    /**
     * 管理端手动解除限制（FR-UC-054，模块9 调用）
     *
     * @param userId     用户ID
     * @param operatorId 操作管理员ID（落流水 operator_id）
     * @return true = 本次解除成功；false = 用户非限制中（幂等）
     */
    boolean adminRemoveLimit(Long userId, Long operatorId);

    /**
     * 申诉通过恢复爽约信用（FR-AD-011 标记签到成功 → 撤销爽约）
     * <p>幂等：订单存在爽约流水（type=1）且尚无申诉恢复流水时才执行——
     * 回加单次扣减分值（上限 100）+ 落恢复流水（type=2，reason 以"申诉通过"开头便于判重）。</p>
     *
     * @param order      爽约订单
     * @param operatorId 操作管理员ID
     * @return true = 本次恢复成功；false = 无爽约记录或已恢复过（幂等跳过）
     */
    boolean restoreNoShowCredit(Orders order, Long operatorId);
}
