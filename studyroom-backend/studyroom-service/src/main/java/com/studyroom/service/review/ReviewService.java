package com.studyroom.service.review;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.dto.order.ReviewCreateDTO;
import com.studyroom.entity.vo.review.ReviewVO;

/**
 * 评价服务接口
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-039 评价提交：订单已完成 + 本人 + 一单一评 + 5星 + 标签 + 10-500字 + 图片≤6</li>
 *   <li>FR-UC-040 评价列表：头像/昵称（脱敏）/评分/标签/图片/管理员回复</li>
 * </ul>
 */
public interface ReviewService {

    /**
     * 提交评价（FR-UC-039）
     * <p>校验：订单存在且属于当前用户、状态=已完成(3)、未评价过（uk_order 兜底）。</p>
     */
    Long submitReview(Long userId, ReviewCreateDTO dto);

    /**
     * 门店评价分页列表（FR-UC-040，门店详情页展示）
     *
     * @param storeId 门店ID
     * @param page    页码（从1开始）
     * @param size    每页条数
     */
    PageResult<ReviewVO> listStoreReviews(Long userId, Long storeId, long page, long size);

    /**
     * 我的评价分页列表
     */
    PageResult<ReviewVO> listMyReviews(Long userId, long page, long size);

    /**
     * 指定订单是否已评价（订单列表/详情"评价"按钮依据，FR-UC-037）
     */
    boolean hasReviewed(Long orderId);
}
