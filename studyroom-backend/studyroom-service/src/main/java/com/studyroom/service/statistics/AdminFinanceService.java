package com.studyroom.service.statistics;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.vo.admin.AdminRefundRecordVO;
import com.studyroom.entity.vo.statistics.FinanceSummaryVO;

/**
 * 管理端财务总览服务 —— FR-AD-024
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>财务总览：时间范围内总营收/总退款/净营收/订单数/客单价 + 按日趋势</li>
 *   <li>退款记录表格：类型/时间/关键字筛选分页</li>
 *   <li>Excel 导出：与退款记录同条件</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminFinanceService {

    /**
     * 财务总览（FR-AD-024）
     *
     * @param beginDate yyyy-MM-dd（含当日）
     * @param endDate   yyyy-MM-dd（含当日全天）
     */
    FinanceSummaryVO summary(String beginDate, String endDate);

    /**
     * 退款记录分页（FR-AD-024）
     *
     * @param type    退款类型：1阶梯取消 2提前离场 3申诉 4管理补偿 5手动（空=全部）
     * @param keyword 订单号模糊
     */
    PageResult<AdminRefundRecordVO> pageRefunds(String beginDate, String endDate, Integer type,
                                                String keyword, long page, long size);

    /**
     * 退款记录导出数据（FR-AD-024，与列表同条件，上限 1 万行）
     */
    java.util.List<AdminRefundRecordVO> listRefundsForExport(String beginDate, String endDate,
                                                             Integer type, String keyword);
}
