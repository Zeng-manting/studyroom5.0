package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.properties.CryptoProperties;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.CryptoUtils;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.review.Review;
import com.studyroom.entity.review.ReviewReply;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.admin.AdminUserDetailVO;
import com.studyroom.entity.vo.admin.AdminUserListVO;
import com.studyroom.mapper.admin.AdminUserStatsMapper;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.pkg.PackageMapper;
import com.studyroom.mapper.review.ReviewMapper;
import com.studyroom.mapper.review.ReviewReplyMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.StoreMapper;
import com.studyroom.mapper.user.UserMapper;
import com.studyroom.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端用户管理服务实现 —— FR-AD-014
 *
 * <h3>口径说明</h3>
 * <ul>
 *   <li>累计订单数 = pay_time 非空的订单数（已支付口径，含退款）</li>
 *   <li>累计学习时长 = actual_duration_minutes 合计（未结算订单该字段为空按 0 计）</li>
 *   <li>手机号/邮箱 AES 加密存储，本服务解密后仅输出脱敏形式（138****1234 / a***@xx.com）</li>
 *   <li>学习记录取最近 20 笔（含全部状态，按下单时间倒序），评价记录取全部评价</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    /** 学习记录条数上限 */
    private static final int STUDY_RECORD_LIMIT = 20;

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final SeatMapper seatMapper;
    private final StoreMapper storeMapper;
    private final PackageMapper packageMapper;
    private final AdminUserStatsMapper adminUserStatsMapper;
    private final CryptoProperties cryptoProperties;
    private final MessageService messageService;

    /* ==================== 用户列表 ==================== */

    @Override
    public PageResult<AdminUserListVO> pageUsers(String keyword, Integer status, Integer creditStatus,
                                                 long page, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getNickname, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (creditStatus != null) {
            wrapper.eq(User::getCreditStatus, creditStatus);
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> p = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<User> users = p.getRecords();
        if (users.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), p.getTotal(), p.getCurrent(), p.getSize());
        }

        // 批量聚合订单数/学习时长（单页 ≤100 用户，两条 GROUP BY SQL）
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, Long> orderCountMap = toLongMap(adminUserStatsMapper.countPaidOrders(userIds));
        Map<Long, Long> studyMinutesMap = toLongMap(adminUserStatsMapper.sumStudyMinutes(userIds));

        List<AdminUserListVO> list = new ArrayList<>(users.size());
        for (User u : users) {
            list.add(toListVO(u, orderCountMap, studyMinutesMap));
        }
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /* ==================== 用户详情 ==================== */

    @Override
    public AdminUserDetailVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        AdminUserDetailVO vo = new AdminUserDetailVO();
        vo.setBaseInfo(toListVO(user,
                toLongMap(adminUserStatsMapper.countPaidOrders(Collections.singletonList(userId))),
                toLongMap(adminUserStatsMapper.sumStudyMinutes(Collections.singletonList(userId)))));

        // 学习记录：最近 20 笔订单（全部状态）
        List<Orders> orders = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreateTime)
                .last("LIMIT " + STUDY_RECORD_LIMIT));
        vo.setStudyRecords(orders.isEmpty() ? Collections.emptyList() : buildStudyRecords(orders));

        // 评价记录 + 管理员回复
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .orderByDesc(Review::getCreateTime));
        vo.setReviewRecords(reviews.isEmpty() ? Collections.emptyList() : buildReviewRecords(reviews));

        return vo;
    }

    /* ==================== 禁用/启用 ==================== */

    @Override
    public void toggleStatus(Long userId, boolean disable) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        if (disable) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该账号已是禁用状态");
            }
            // 禁用前校验：无已预约/进行中订单，避免影响在场学习与占座秩序
            Long activeCount = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getUserId, userId)
                    .in(Orders::getStatus, Orders.STATUS_RESERVED, Orders.STATUS_IN_USE));
            if (activeCount != null && activeCount > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "该用户有 " + activeCount + " 笔已预约/进行中订单，请先处理后再禁用");
            }
            User patch = new User();
            patch.setId(userId);
            patch.setStatus(0);
            userMapper.updateById(patch);
            messageService.sendSystemMessage(userId, "账号状态变更通知",
                    "您的账号已被管理员禁用。如有疑问请联系门店客服。");
            log.info("[AdminUser] FR-AD-014 禁用账号: userId={}", userId);
        } else {
            if (user.getStatus() != null && user.getStatus() == 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该账号已是启用状态");
            }
            User patch = new User();
            patch.setId(userId);
            patch.setStatus(1);
            userMapper.updateById(patch);
            messageService.sendSystemMessage(userId, "账号状态变更通知",
                    "您的账号已恢复启用，欢迎回来。");
            log.info("[AdminUser] FR-AD-014 启用账号: userId={}", userId);
        }
    }

    /* ==================== 内部方法 ==================== */

    /** 聚合结果 Map 列表 → userId → 数值 */
    private Map<Long, Long> toLongMap(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            Object userId = row.get("user_id");
            Object cnt = row.get("cnt") != null ? row.get("cnt") : row.get("minutes");
            if (userId != null && cnt != null) {
                map.put(((Number) userId).longValue(), ((Number) cnt).longValue());
            }
        }
        return map;
    }

    private AdminUserListVO toListVO(User u, Map<Long, Long> orderCountMap, Map<Long, Long> studyMinutesMap) {
        AdminUserListVO vo = new AdminUserListVO();
        vo.setId(u.getId());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhoneMasked(decryptAndMaskPhone(u.getPhoneEncrypted()));
        vo.setEmailMasked(decryptAndMaskEmail(u.getEmailEncrypted()));
        vo.setGender(u.getGender());
        vo.setBalance(u.getBalance());
        vo.setCreditScore(u.getCreditScore());
        vo.setCreditStatus(u.getCreditStatus());
        vo.setStatus(u.getStatus());
        vo.setOrderCount(orderCountMap.getOrDefault(u.getId(), 0L));
        vo.setTotalStudyMinutes(studyMinutesMap.getOrDefault(u.getId(), 0L));
        vo.setLastLoginTime(u.getLastLoginTime());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }

    private List<AdminUserDetailVO.StudyRecordItem> buildStudyRecords(List<Orders> orders) {
        // 批量补门店/座位名（ID 集合为空时跳过，避免 IN () 非法 SQL）
        Map<Long, String> storeNames = nameMap(
                selectByIdsSafe(storeMapper, distinctIds(orders, Orders::getStoreId)),
                Store::getId, Store::getName);
        Map<Long, String> seatNos = nameMap(
                selectByIdsSafe(seatMapper, distinctIds(orders, Orders::getSeatId)),
                Seat::getId, Seat::getSeatNo);

        return orders.stream().map(o -> {
            AdminUserDetailVO.StudyRecordItem item = new AdminUserDetailVO.StudyRecordItem();
            item.setOrderId(o.getId());
            item.setOrderNo(o.getOrderNo());
            item.setStoreName(storeNames.get(o.getStoreId()));
            item.setSeatNo(seatNos.get(o.getSeatId()));
            item.setPackageName(packageNameOf(o.getPackageId()));
            item.setStartTime(o.getStartTime());
            item.setEndTime(o.getEndTime());
            item.setStatus(o.getStatus());
            item.setStatusDesc(orderStatusDesc(o.getStatus()));
            item.setPayAmount(o.getPayAmount());
            item.setNoShow(o.getIsNoShow() != null && o.getIsNoShow() == 1);
            item.setActualDurationMinutes(o.getActualDurationMinutes());
            item.setCreateTime(o.getCreateTime());
            return item;
        }).collect(Collectors.toList());
    }

    private List<AdminUserDetailVO.ReviewRecordItem> buildReviewRecords(List<Review> reviews) {
        Map<Long, String> storeNames = nameMap(
                selectByIdsSafe(storeMapper, distinctIds(reviews, Review::getStoreId)),
                Store::getId, Store::getName);
        // 批量查回复（一评一回复）
        Set<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toSet());
        Map<Long, ReviewReply> replyMap = reviewIds.isEmpty() ? Collections.emptyMap()
                : reviewReplyMapper.selectList(new LambdaQueryWrapper<ReviewReply>()
                        .in(ReviewReply::getReviewId, reviewIds))
                .stream().collect(Collectors.toMap(ReviewReply::getReviewId, Function.identity(), (a, b) -> a));

        return reviews.stream().map(r -> {
            AdminUserDetailVO.ReviewRecordItem item = new AdminUserDetailVO.ReviewRecordItem();
            item.setReviewId(r.getId());
            item.setStoreName(storeNames.get(r.getStoreId()));
            item.setScore(r.getScore());
            item.setContent(r.getContent());
            item.setStatus(r.getStatus());
            ReviewReply reply = replyMap.get(r.getId());
            if (reply != null) {
                item.setReplyContent(reply.getContent());
            }
            item.setCreateTime(r.getCreateTime());
            return item;
        }).collect(Collectors.toList());
    }

    /** 套餐名（学习记录逐条调用，单页 ≤20 次；套餐数据量小，后续可加缓存） */
    private String packageNameOf(Long packageId) {
        if (packageId == null) {
            return null;
        }
        com.studyroom.entity.pkg.Package pkg = packageMapper.selectById(packageId);
        return pkg != null ? pkg.getName() : null;
    }

    private String orderStatusDesc(Integer status) {
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

    /** 手机号解密 + 脱敏：138****1234 */
    private String decryptAndMaskPhone(byte[] cipherBytes) {
        String phone = decrypt(cipherBytes);
        if (phone == null || phone.length() < 7) {
            return phone == null ? null : "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 邮箱解密 + 脱敏：a***@example.com */
    private String decryptAndMaskEmail(byte[] cipherBytes) {
        String email = decrypt(cipherBytes);
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String decrypt(byte[] cipherBytes) {
        if (cipherBytes == null || cipherBytes.length == 0) {
            return null;
        }
        try {
            return CryptoUtils.aesDecrypt(new String(cipherBytes, StandardCharsets.UTF_8),
                    cryptoProperties.getAesKey());
        } catch (Exception e) {
            log.warn("[AdminUser] 加密字段解密失败（密钥是否变更？）: {}", e.getMessage());
            return null;
        }
    }

    /* ---------- 泛型小工具 ---------- */

    @SuppressWarnings("unchecked")
    private <T> Set<Long> distinctIds(List<T> records, Function<T, Long> getter) {
        return records.stream().map(getter).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** selectBatchIds 空集合保护：空集合直接返回空列表，避免生成 IN () 非法 SQL */
    private <T> List<T> selectByIdsSafe(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                                        Set<Long> ids) {
        return ids.isEmpty() ? Collections.emptyList() : mapper.selectBatchIds(ids);
    }

    private <T> Map<Long, String> nameMap(List<T> records, Function<T, Long> idGetter,
                                          Function<T, String> nameGetter) {
        return records.stream().collect(Collectors.toMap(idGetter, nameGetter, (a, b) -> a));
    }
}
