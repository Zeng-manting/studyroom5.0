package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.JacksonUtils;
import com.studyroom.entity.dto.admin.ReviewReplyDTO;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.review.Review;
import com.studyroom.entity.review.ReviewImage;
import com.studyroom.entity.review.ReviewReply;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.admin.AdminReviewVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.review.ReviewImageMapper;
import com.studyroom.mapper.review.ReviewMapper;
import com.studyroom.mapper.review.ReviewReplyMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.StoreMapper;
import com.studyroom.mapper.user.UserMapper;
import com.studyroom.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端评价管理服务实现 —— FR-AD-017/018
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>匿名评价在管理端仍显示真实昵称（便于跟进处理），仅用户端脱敏（FR-UC-040）</li>
 *   <li>回复为覆盖式编辑（一评一回复，review_reply 无唯一键约束，业务上先查后写）</li>
 *   <li>首次回复后站内信通知评价用户（编辑回复不重复通知）</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private static final ObjectMapper MAPPER = JacksonUtils.defaultMapper();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReviewMapper reviewMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final ReviewImageMapper reviewImageMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final StoreMapper storeMapper;
    private final SeatMapper seatMapper;
    private final MessageService messageService;

    /* ==================== FR-AD-017：评价列表 ==================== */

    @Override
    public PageResult<AdminReviewVO> pageReviews(Integer score, String beginDate, String endDate,
                                                 Integer status, String keyword,
                                                 long page, long size) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        if (score != null) {
            wrapper.eq(Review::getScore, score);
        }
        if (StringUtils.hasText(beginDate)) {
            wrapper.ge(Review::getCreateTime, LocalDate.parse(beginDate, DATE).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(Review::getCreateTime,
                    LocalDate.parse(endDate, DATE).atStartOfDay().plusDays(1).minusNanos(1));
        }
        if (status != null) {
            wrapper.eq(Review::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Review::getContent, keyword.trim());
        }
        wrapper.orderByDesc(Review::getCreateTime);

        Page<Review> p = reviewMapper.selectPage(new Page<>(page, size), wrapper);
        List<Review> records = p.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), p.getTotal(), p.getCurrent(), p.getSize());
        }
        return new PageResult<>(assemble(records), p.getTotal(), p.getCurrent(), p.getSize());
    }

    /* ==================== FR-AD-018：回复评价 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewVO replyReview(Long reviewId, ReviewReplyDTO dto, Long adminId) {
        Review review = requireReview(reviewId);

        ReviewReply existing = reviewReplyMapper.selectOne(
                new LambdaQueryWrapper<ReviewReply>().eq(ReviewReply::getReviewId, reviewId));
        if (existing == null) {
            ReviewReply reply = new ReviewReply();
            reply.setReviewId(reviewId);
            reply.setContent(dto.getContent().trim());
            reply.setAdminId(adminId);
            reviewReplyMapper.insert(reply);
            // 首次回复站内信通知（事务提交语义由调用方消息服务自身的异步/独立事务承担）
            messageService.sendSystemMessage(review.getUserId(), "您的评价已收到回复",
                    "您的评价已收到门店回复，快去看看吧。");
            log.info("[AdminReview] FR-AD-018 新增回复: reviewId={}, admin={}", reviewId, adminId);
        } else {
            existing.setContent(dto.getContent().trim());
            existing.setAdminId(adminId);
            reviewReplyMapper.updateById(existing);
            log.info("[AdminReview] FR-AD-018 编辑回复: reviewId={}, admin={}", reviewId, adminId);
        }

        return assemble(Collections.singletonList(
                requireReview(reviewId))).get(0);
    }

    /* ==================== FR-AD-018：隐藏/显示 ==================== */

    @Override
    public void toggleStatus(Long reviewId, boolean hide) {
        Review review = requireReview(reviewId);
        int target = hide ? Review.STATUS_HIDDEN : Review.STATUS_VISIBLE;
        if (review.getStatus() != null && review.getStatus() == target) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    hide ? "该评价已是隐藏状态" : "该评价已是显示状态");
        }
        Review patch = new Review();
        patch.setId(reviewId);
        patch.setStatus(target);
        reviewMapper.updateById(patch);
        log.info("[AdminReview] FR-AD-018 评价{}: reviewId={}", hide ? "隐藏" : "显示", reviewId);
    }

    /* ==================== 内部方法 ==================== */

    private Review requireReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        return review;
    }

    /** VO 组装：订单号/用户昵称/门店名/座位号/标签/图片/回复 批量补齐 */
    private List<AdminReviewVO> assemble(List<Review> reviews) {
        Set<Long> orderIds = reviews.stream().map(Review::getOrderId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Orders> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Orders::getId, Function.identity(), (a, b) -> a));

        Set<Long> userIds = reviews.stream().map(Review::getUserId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        Map<Long, String> storeNames = batchNames(storeMapper, reviews, Review::getStoreId, Store::getId, Store::getName);
        Map<Long, String> seatNos = batchNames(seatMapper, reviews, Review::getSeatId, Seat::getId, Seat::getSeatNo);

        Set<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toSet());
        Map<Long, ReviewReply> replyMap = reviewIds.isEmpty() ? Collections.emptyMap()
                : reviewReplyMapper.selectList(new LambdaQueryWrapper<ReviewReply>()
                        .in(ReviewReply::getReviewId, reviewIds))
                .stream().collect(Collectors.toMap(ReviewReply::getReviewId, Function.identity(), (a, b) -> a));
        Map<Long, List<ReviewImage>> imageMap = reviewIds.isEmpty() ? Collections.emptyMap()
                : reviewImageMapper.selectList(new LambdaQueryWrapper<ReviewImage>()
                        .in(ReviewImage::getReviewId, reviewIds)
                        .orderByAsc(ReviewImage::getSort))
                .stream().collect(Collectors.groupingBy(ReviewImage::getReviewId));

        List<AdminReviewVO> list = new ArrayList<>(reviews.size());
        for (Review r : reviews) {
            AdminReviewVO vo = new AdminReviewVO();
            vo.setId(r.getId());
            vo.setOrderId(r.getOrderId());
            Orders order = orderMap.get(r.getOrderId());
            vo.setOrderNo(order != null ? order.getOrderNo() : null);
            vo.setUserId(r.getUserId());
            User user = userMap.get(r.getUserId());
            vo.setNickname(user != null && StringUtils.hasText(user.getNickname())
                    ? user.getNickname() : "用户#" + r.getUserId());
            vo.setStoreName(storeNames.get(r.getStoreId()));
            vo.setSeatNo(seatNos.get(r.getSeatId()));
            vo.setScore(r.getScore());
            vo.setTags(parseTags(r.getTags()));
            vo.setContent(r.getContent());
            List<ReviewImage> imgs = imageMap.getOrDefault(r.getId(), Collections.emptyList());
            vo.setImages(imgs.stream().map(ReviewImage::getImageUrl).collect(Collectors.toList()));
            vo.setAnonymous(r.getAnonymous());
            vo.setStatus(r.getStatus());
            ReviewReply reply = replyMap.get(r.getId());
            if (reply != null) {
                vo.setReplyContent(reply.getContent());
                vo.setReplyTime(reply.getUpdateTime() != null ? reply.getUpdateTime()
                        : reply.getCreateTime());
            }
            vo.setCreateTime(r.getCreateTime());
            list.add(vo);
        }
        return list;
    }

    private List<String> parseTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(tagsJson,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("[AdminReview] 标签解析失败: {}", tagsJson);
            return Collections.emptyList();
        }
    }

    /** 通用批量名称补齐（ID 空集合跳过，避免 IN () 非法 SQL） */
    private <T, E> Map<Long, String> batchNames(com.baomidou.mybatisplus.core.mapper.BaseMapper<E> mapper,
                                                List<T> records, Function<T, Long> idGetter,
                                                Function<E, Long> keyGetter, Function<E, String> nameGetter) {
        Set<Long> ids = records.stream().map(idGetter).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(keyGetter, nameGetter, (a, b) -> a));
    }
}
