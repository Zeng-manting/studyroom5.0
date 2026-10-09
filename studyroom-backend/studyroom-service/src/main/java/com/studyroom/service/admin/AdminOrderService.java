package com.studyroom.service.admin;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.dto.admin.AdminRefundDTO;
import com.studyroom.entity.dto.admin.AppealHandleDTO;
import com.studyroom.entity.vo.admin.AdminAppealListVO;
import com.studyroom.entity.vo.admin.AdminOrderListVO;
import com.studyroom.entity.vo.admin.AdminRefundPreviewVO;

import java.util.List;

/**
 * 管理端订单服务 —— FR-AD-010 ~ FR-AD-013
 *
 * <h3>功能映射</h3>
 * <ul>
 *   <li>FR-AD-010 订单列表：多条件筛选（时间/状态/座位号/用户），分页默认 20 条</li>
 *   <li>FR-AD-011 申诉处理：查看详情+凭证图片，标记签到成功 / 驳回</li>
 *   <li>FR-AD-012 退款审核：系统自动计算建议退款金额，支持手动调整</li>
 *   <li>FR-AD-013 订单导出：与列表同条件导出 Excel（上限 1 万行，Web 层负责写出）</li>
 * </ul>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>列表与导出共用同一套筛选条件与 VO，保证口径一致</li>
 *   <li>申诉/退款均为低频管理操作，采用 CAS（状态条件更新）防并发重复处理</li>
 *   <li>申诉通过分订单状态处理：已预约→补记签到转进行中；已爽约取消→撤销爽约恢复信用</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminOrderService {

    /**
     * 订单列表（FR-AD-010）
     *
     * @param beginDate 开始日期（yyyy-MM-dd，按下单时间，含当日，可空）
     * @param endDate   结束日期（yyyy-MM-dd，按下单时间，含当日，可空）
     * @param status    订单状态（可空 = 全部）
     * @param seatNo    座位号模糊匹配（可空）
     * @param userId    用户ID精确匹配（可空）
     * @param keyword   关键字：订单号 / 用户昵称模糊匹配（可空）
     * @param page      页码（从 1 开始）
     * @param size      每页条数（默认 20，上限 100）
     */
    PageResult<AdminOrderListVO> pageOrders(String beginDate, String endDate, Integer status,
                                            String seatNo, Long userId, String keyword,
                                            long page, long size);

    /**
     * 订单导出数据（FR-AD-013）
     * <p>筛选条件与 {@link #pageOrders} 完全一致；上限 1 万行，超出截断。</p>
     */
    List<AdminOrderListVO> listForExport(String beginDate, String endDate, Integer status,
                                         String seatNo, Long userId, String keyword);

    /**
     * 申诉列表（FR-AD-011）
     *
     * @param status 申诉状态：0待审 1通过 2驳回（可空 = 全部）
     */
    PageResult<AdminAppealListVO> pageAppeals(Integer status, long page, long size);

    /**
     * 申诉详情（FR-AD-011：含凭证图片 URL 列表与关联订单信息）
     */
    AdminAppealListVO getAppealDetail(Long appealId);

    /**
     * 申诉处理-标记签到成功（FR-AD-011）
     * <p>按订单当前状态分三种语义：
     * 已预约(1)未签到 → 补记签到时间并转进行中(2)，座位转使用中；
     * 已爽约取消(6) → 撤销爽约标记 + 恢复信用分（订单保持已退款，用户可重新预约）；
     * 其余状态 → 仅申诉通过留痕。</p>
     *
     * @param appealId   申诉ID
     * @param dto        处理意见（可空）
     * @param operatorId 操作管理员ID
     */
    void approveAppeal(Long appealId, AppealHandleDTO dto, Long operatorId);

    /**
     * 申诉处理-驳回（FR-AD-011）
     *
     * @param appealId   申诉ID
     * @param dto        处理意见（必填，服务端校验）
     * @param operatorId 操作管理员ID
     */
    void rejectAppeal(Long appealId, AppealHandleDTO dto, Long operatorId);

    /**
     * 退款审核预览（FR-AD-012：系统自动计算建议退款金额）
     * <p>建议规则：已预约订单复用 FR-UC-027 阶梯比例；已完成订单属补偿性退款，建议 0 由管理员判断；
     * 进行中订单不可直接退款（请先结束使用）；待支付/已取消/已退款订单无可退金额。</p>
     */
    AdminRefundPreviewVO previewRefund(Long orderId);

    /**
     * 退款审核通过（FR-AD-012：管理员可调整金额）
     * <p>校验 0 &lt; amount ≤ 实付 - 已退；事务：退款流水（type=5 手动）+ 订单状态流转
     * （已预约 → 已退款并释放优惠券；已完成 → 保持状态累计退款额）+ 余额回充；
     * 事务后站内消息 + 邮件通知。</p>
     *
     * @param orderId    订单ID
     * @param dto        退款金额与原因
     * @param operatorId 操作管理员ID
     */
    void manualRefund(Long orderId, AdminRefundDTO dto, Long operatorId);
}
