package com.studyroom.service.admin;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.dto.admin.ReviewReplyDTO;
import com.studyroom.entity.vo.admin.AdminReviewVO;

/**
 * 管理端评价管理服务 —— FR-AD-017/018
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-AD-017 评价列表筛选：评分/时间范围/显示状态多条件筛选分页</li>
 *   <li>FR-AD-018 回复评价：10-200 字回复，已有回复则编辑覆盖；隐藏/显示评价</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminReviewService {

    /**
     * 评价分页列表（FR-AD-017）
     *
     * @param score     评分：1-5（空=全部）
     * @param beginDate 开始日期 yyyy-MM-dd（按评价时间，含当日）
     * @param endDate   结束日期 yyyy-MM-dd（含当日全天）
     * @param status    显示状态：0隐藏 1显示（空=全部）
     * @param keyword   评价内容模糊
     */
    PageResult<AdminReviewVO> pageReviews(Integer score, String beginDate, String endDate,
                                          Integer status, String keyword,
                                          long page, long size);

    /**
     * 回复评价（FR-AD-018）
     *
     * <p>一评一回复：无回复则新增（记 adminId），已有回复则编辑覆盖内容。
     * 回复后评价恢复显示语义由调用方决定，本方法不改评价 status。</p>
     *
     * @return 回复后的评价（含最新回复内容）
     */
    AdminReviewVO replyReview(Long reviewId, ReviewReplyDTO dto, Long adminId);

    /**
     * 隐藏/显示评价（FR-AD-018）
     *
     * @param hide true=隐藏（用户端 FR-UC-040 列表不可见）false=显示
     */
    void toggleStatus(Long reviewId, boolean hide);
}
