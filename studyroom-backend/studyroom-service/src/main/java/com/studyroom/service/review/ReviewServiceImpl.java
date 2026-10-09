package com.studyroom.service.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.common.utils.JacksonUtils;
import com.studyroom.entity.dto.order.ReviewCreateDTO;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.review.Review;
import com.studyroom.entity.review.ReviewImage;
import com.studyroom.entity.review.ReviewReply;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.review.ReviewVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.review.ReviewImageMapper;
import com.studyroom.mapper.review.ReviewMapper;
import com.studyroom.mapper.review.ReviewReplyMapper;
import com.studyroom.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评价服务实现（FR-UC-039/040）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitReview(Long userId, ReviewCreateDTO dto) {
        // FR-UC-039：订单归属 + 状态校验（仅已完成可评价）
        Orders order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单不存在");
        }
        if (order.getStatus() != Orders.STATUS_FINISHED) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "仅已完成订单可评价");
        }
        // 一单一评（DB uk_order 唯一键兜底）
        Long existed = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, dto.getOrderId()));
        if (existed != null && existed > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该订单已评价过");
        }

        // 落评价主记录：tags JSON 数组序列化
        Review review = new Review();
        review.setId(IdGenerator.nextId());
        review.setOrderId(order.getId());
        review.setUserId(userId);
        review.setStoreId(order.getStoreId());
        review.setSeatId(order.getSeatId());
        review.setScore(dto.getScore());
        review.setTags(toJson(dto.getTags()));
        review.setContent(dto.getContent());
        review.setAnonymous(dto.getAnonymous() != null && dto.getAnonymous() == 1 ? 1 : 0);
        review.setStatus(Review.STATUS_VISIBLE);
        reviewMapper.insert(review);

        // 落图片（≤6 张，校验由 DTO @Size 完成）
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            int sort = 0;
            for (String url : dto.getImages()) {
                ReviewImage img = new ReviewImage();
                img.setId(IdGenerator.nextId());
                img.setReviewId(review.getId());
                img.setImageUrl(url);
                img.setSort(sort++);
                reviewImageMapper.insert(img);
            }
        }
        log.info("[Review] FR-UC-039 评价提交成功: orderId={}, userId={}, score={}",
                order.getId(), userId, dto.getScore());
        return review.getId();
    }

    @Override
    public PageResult<ReviewVO> listStoreReviews(Long userId, Long storeId, long page, long size) {
        return queryReviews(userId,
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getStoreId, storeId)
                        .eq(Review::getStatus, Review.STATUS_VISIBLE)
                        .orderByDesc(Review::getCreateTime),
                page, size);
    }

    @Override
    public PageResult<ReviewVO> listMyReviews(Long userId, long page, long size) {
        return queryReviews(userId,
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .orderByDesc(Review::getCreateTime),
                page, size);
    }

    @Override
    public boolean hasReviewed(Long orderId) {
        Long cnt = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId));
        return cnt != null && cnt > 0;
    }

    /* ==================== 私有方法 ==================== */

    private PageResult<ReviewVO> queryReviews(Long currentUserId, LambdaQueryWrapper<Review> wrapper,
                                              long page, long size) {
        Page<Review> p = reviewMapper.selectPage(new Page<>(page, size), wrapper);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        List<Review> reviews = p.getRecords();

        // 批量查图片
        List<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toList());
        Map<Long, List<ReviewImage>> imageMap = reviewImageMapper.selectList(
                        new LambdaQueryWrapper<ReviewImage>()
                                .in(ReviewImage::getReviewId, reviewIds)
                                .orderByAsc(ReviewImage::getSort))
                .stream().collect(Collectors.groupingBy(ReviewImage::getReviewId));

        // 批量查回复
        Map<Long, ReviewReply> replyMap = reviewReplyMapper.selectList(
                        new LambdaQueryWrapper<ReviewReply>().in(ReviewReply::getReviewId, reviewIds))
                .stream().collect(Collectors.toMap(ReviewReply::getReviewId, Function.identity(),
                        (a, b) -> a));

        // 批量查评价人（匿名评价不展示昵称头像）
        Set<Long> userIds = reviews.stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ReviewVO> vos = new ArrayList<>();
        for (Review r : reviews) {
            ReviewVO vo = new ReviewVO();
            vo.setId(r.getId());
            vo.setOrderId(r.getOrderId());
            vo.setScore(r.getScore());
            vo.setTags(fromJson(r.getTags()));
            vo.setContent(r.getContent());
            List<ReviewImage> imgs = imageMap.getOrDefault(r.getId(), Collections.emptyList());
            vo.setImages(imgs.stream().map(ReviewImage::getImageUrl).collect(Collectors.toList()));
            vo.setAnonymous(r.getAnonymous() != null && r.getAnonymous() == 1);
            vo.setMine(currentUserId != null && currentUserId.equals(r.getUserId()));
            vo.setCreateTime(r.getCreateTime());

            // FR-UC-040：匿名 → "匿名用户"；否则昵称脱敏（首字符 + ***）
            if (Boolean.TRUE.equals(vo.getAnonymous())) {
                vo.setNickname("匿名用户");
                vo.setAvatar(null);
            } else {
                User u = userMap.get(r.getUserId());
                vo.setNickname(mask(u == null ? "自习用户" : u.getNickname()));
                vo.setAvatar(u != null ? u.getAvatar() : null);
            }

            ReviewReply reply = replyMap.get(r.getId());
            if (reply != null) {
                ReviewVO.ReplyVO replyVO = new ReviewVO.ReplyVO();
                replyVO.setContent(reply.getContent());
                replyVO.setCreateTime(reply.getCreateTime());
                vo.setReply(replyVO);
            }
            vos.add(vo);
        }
        return new PageResult<>(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 昵称脱敏：首字符 + ***（FR-UC-040） */
    private String mask(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return "自习用户";
        }
        return nickname.charAt(0) + "***";
    }

    private String toJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return JacksonUtils.defaultMapper().writeValueAsString(tags);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JacksonUtils.defaultMapper().readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
