package com.studyroom.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.user.User;
import com.studyroom.entity.user.UserCreditLog;
import com.studyroom.entity.vo.user.CreditVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.user.UserCreditLogMapper;
import com.studyroom.mapper.user.UserMapper;
import com.studyroom.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 信用服务实现（FR-UC-053 爽约记录 / FR-UC-054 信用限制）
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final UserMapper userMapper;
    private final UserCreditLogMapper creditLogMapper;
    private final OrderMapper orderMapper;
    private final MessageService messageService;

    /* ==================== FR-UC-053：爽约记录 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordNoShow(Orders order) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = order.getUserId();

        // 1. 实时统计 30 天滚动周期内爽约次数（以流水表 type=1 为准，重放/重复触发天然幂等于调用方 CAS）
        LocalDateTime since = now.minusDays(CommonConstants.CREDIT_PERIOD_DAYS);
        int count30d = creditLogMapper.countNoShowSince(userId, since);

        // 2. 落爽约流水（FR-UC-053）
        UserCreditLog noShowLog = new UserCreditLog();
        noShowLog.setId(IdGenerator.nextId());
        noShowLog.setUserId(userId);
        noShowLog.setOrderId(order.getId());
        noShowLog.setType(UserCreditLog.TYPE_NO_SHOW);
        noShowLog.setScoreDelta(-CommonConstants.CREDIT_NO_SHOW_SCORE_DELTA);
        noShowLog.setReason("订单 " + order.getOrderNo() + " 开始后 30 分钟未签到，记爽约一次");
        creditLogMapper.insert(noShowLog);

        // 3. 信用分原子扣减（下限 0）+ 冗余 30 天爽约计数
        int afterCount = count30d + 1;
        userMapper.deductCreditOnNoShow(userId, CommonConstants.CREDIT_NO_SHOW_SCORE_DELTA, afterCount);

        // 4. FR-UC-054：周期内达上限 → 置 7 天限制（CAS 防重复限制）
        if (afterCount >= CommonConstants.CREDIT_NO_SHOW_LIMIT) {
            LocalDateTime limitEnd = now.plusDays(CommonConstants.CREDIT_LIMIT_DAYS);
            int limited = userMapper.casStartCreditLimit(userId, limitEnd);
            if (limited > 0) {
                UserCreditLog limitLog = new UserCreditLog();
                limitLog.setId(IdGenerator.nextId());
                limitLog.setUserId(userId);
                limitLog.setOrderId(order.getId());
                limitLog.setType(UserCreditLog.TYPE_LIMIT);
                limitLog.setScoreDelta(0);
                limitLog.setReason("30 天内爽约 " + afterCount + " 次，限制预约 "
                        + CommonConstants.CREDIT_LIMIT_DAYS + " 天至 "
                        + limitEnd.toLocalDate());
                creditLogMapper.insert(limitLog);

                // FR-UC-045 消息推送（限制告知，best-effort）
                messageService.sendSystemMessage(userId, "预约功能受限",
                        "您 30 天内累计爽约 " + afterCount + " 次，即日起 "
                                + CommonConstants.CREDIT_LIMIT_DAYS + " 天内无法预约座位，"
                                + limitEnd.toLocalDate() + " 自动恢复。如有疑问请联系门店客服。");
                log.warn("[Credit] FR-UC-054 信用限制生效: userId={}, noShow={}, limitEnd={}",
                        userId, afterCount, limitEnd);
            }
        }
        log.info("[Credit] FR-UC-053 爽约记账: userId={}, orderNo={}, count30d={}",
                userId, order.getOrderNo(), afterCount);
    }

    /* ==================== FR-UC-054：下单拦截与到期恢复 ==================== */

    @Override
    public void checkOrderAllowed(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getCreditStatus() == null || user.getCreditStatus() == 0) {
            return; // 正常，放行
        }
        LocalDateTime now = LocalDateTime.now();
        // 限制已到期：懒恢复（CAS 解除 + 恢复流水），随后放行
        if (user.getCreditLimitEnd() == null || !now.isBefore(user.getCreditLimitEnd())) {
            restoreIfNeeded(user, now);
            return;
        }
        long remainDays = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(),
                user.getCreditLimitEnd().toLocalDate()) + 1;
        throw new BusinessException("您的预约功能已被限制：30 天内爽约达 "
                + CommonConstants.CREDIT_NO_SHOW_LIMIT + " 次，预计 "
                + user.getCreditLimitEnd().toLocalDate() + " 自动恢复（剩余约 "
                + remainDays + " 天）。如有疑问请联系门店客服申诉。");
    }

    /**
     * 懒恢复：CAS 解除限制 + 落恢复流水 + 通知（FR-UC-054 到期自动恢复）
     */
    private void restoreIfNeeded(User user, LocalDateTime now) {
        // 恢复后的周期爽约计数按当前时间重新统计（旧爽约已滑出 30 天窗口）
        int currentCount = creditLogMapper.countNoShowSince(user.getId(),
                now.minusDays(CommonConstants.CREDIT_PERIOD_DAYS));
        int rows = userMapper.casRemoveCreditLimit(user.getId(), currentCount);
        if (rows > 0) {
            UserCreditLog restoreLog = new UserCreditLog();
            restoreLog.setId(IdGenerator.nextId());
            restoreLog.setUserId(user.getId());
            restoreLog.setType(UserCreditLog.TYPE_RESTORE);
            restoreLog.setScoreDelta(0);
            restoreLog.setReason("限制期满，系统自动恢复预约功能");
            creditLogMapper.insert(restoreLog);
            messageService.sendSystemMessage(user.getId(), "预约功能已恢复",
                    "您的信用限制已期满解除，现在可以正常预约座位啦。请按时到店签到，避免爽约哦。");
            log.info("[Credit] FR-UC-054 限制到期自动恢复: userId={}", user.getId());
        }
    }

    /* ==================== 用户端信用页 ==================== */

    @Override
    public CreditVO getMyCredit(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        LocalDateTime now = LocalDateTime.now();

        // 懒恢复兜底（信用页访问也可能触发恢复）
        if (user.getCreditStatus() != null && user.getCreditStatus() == 1
                && (user.getCreditLimitEnd() == null || !now.isBefore(user.getCreditLimitEnd()))) {
            restoreIfNeeded(user, now);
            user = userMapper.selectById(userId);
        }

        CreditVO vo = new CreditVO();
        vo.setCreditScore(user.getCreditScore() == null ? 100 : user.getCreditScore());
        vo.setCreditStatus(user.getCreditStatus() == null ? 0 : user.getCreditStatus());
        vo.setCreditLimitEnd(user.getCreditLimitEnd());
        if (vo.getCreditStatus() == 1 && user.getCreditLimitEnd() != null) {
            long remainDays = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(),
                    user.getCreditLimitEnd().toLocalDate()) + 1;
            vo.setLimitRemainingDays((int) Math.max(remainDays, 0));
        }

        // 30 天爽约次数按流水实时统计（不依赖冗余字段）
        LocalDateTime since = now.minusDays(CommonConstants.CREDIT_PERIOD_DAYS);
        int count30d = creditLogMapper.countNoShowSince(userId, since);
        vo.setNoShowCount30d(count30d);
        vo.setNoShowLimit(CommonConstants.CREDIT_NO_SHOW_LIMIT);
        vo.setPeriodRemainingDays(CommonConstants.CREDIT_PERIOD_DAYS);

        // 近期爽约订单（FR-UC-053 爽约明细，最近 10 条）
        List<Orders> noShowOrders = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .eq(Orders::getIsNoShow, 1)
                .orderByDesc(Orders::getCancelTime)
                .last("LIMIT 10"));
        vo.setNoShows(noShowOrders.stream().map(o -> {
            CreditVO.NoShowItem item = new CreditVO.NoShowItem();
            item.setOrderId(o.getId());
            item.setOrderNo(o.getOrderNo());
            item.setStartTime(o.getStartTime());
            // 爽约判定时间 = 取消时间（开始后 30 分钟自动取消时刻）
            item.setNoShowTime(o.getCancelTime());
            return item;
        }).collect(Collectors.toList()));

        // 信用流水（最近 20 条）
        List<UserCreditLog> logs = creditLogMapper.selectList(new LambdaQueryWrapper<UserCreditLog>()
                .eq(UserCreditLog::getUserId, userId)
                .orderByDesc(UserCreditLog::getCreateTime)
                .last("LIMIT 20"));
        vo.setLogs(logs.stream().map(l -> {
            CreditVO.CreditLogItem item = new CreditVO.CreditLogItem();
            item.setType(l.getType());
            item.setScoreDelta(l.getScoreDelta());
            item.setReason(l.getReason());
            item.setTime(l.getCreateTime());
            return item;
        }).collect(Collectors.toList()));
        return vo;
    }

    /* ==================== 管理端手动解除（模块9 调用） ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminRemoveLimit(Long userId, Long operatorId) {
        int rows = userMapper.casRemoveCreditLimit(userId, 0);
        if (rows == 0) {
            log.info("[Credit] 手动解除未命中（用户非限制中）: userId={}", userId);
            return false;
        }
        UserCreditLog unLimitLog = new UserCreditLog();
        unLimitLog.setId(IdGenerator.nextId());
        unLimitLog.setUserId(userId);
        unLimitLog.setType(UserCreditLog.TYPE_UNLIMIT);
        unLimitLog.setScoreDelta(0);
        unLimitLog.setReason("管理员手动解除预约限制");
        unLimitLog.setOperatorId(operatorId);
        creditLogMapper.insert(unLimitLog);
        messageService.sendSystemMessage(userId, "预约功能已恢复",
                "您的预约限制已由管理员解除，请珍惜信用按时签到。");
        log.info("[Credit] FR-UC-054 管理员手动解除限制: userId={}, operator={}", userId, operatorId);
        return true;
    }

    /* ==================== FR-AD-011：申诉通过撤销爽约 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreNoShowCredit(Orders order, Long operatorId) {
        Long orderId = order.getId();
        // 幂等双保险：无爽约流水不恢复；已有申诉恢复流水不重复回加
        if (creditLogMapper.countNoShowByOrderId(orderId) == 0
                || creditLogMapper.countAppealRestoreByOrderId(orderId) > 0) {
            log.info("[Credit] 撤销爽约跳过（无爽约记录或已恢复）: orderNo={}", order.getOrderNo());
            return false;
        }

        // 1. 回加信用分（上限 100）+ 冗余爽约计数 -1
        userMapper.addCreditOnRestore(order.getUserId(), CommonConstants.CREDIT_NO_SHOW_SCORE_DELTA);

        // 2. 落恢复流水（reason 前缀"申诉通过"供 countAppealRestoreByOrderId 判重）
        UserCreditLog restoreLog = new UserCreditLog();
        restoreLog.setId(IdGenerator.nextId());
        restoreLog.setUserId(order.getUserId());
        restoreLog.setOrderId(orderId);
        restoreLog.setType(UserCreditLog.TYPE_RESTORE);
        restoreLog.setScoreDelta(CommonConstants.CREDIT_NO_SHOW_SCORE_DELTA);
        restoreLog.setReason("申诉通过，撤销订单 " + order.getOrderNo() + " 的爽约记录");
        restoreLog.setOperatorId(operatorId);
        creditLogMapper.insert(restoreLog);

        // 3. 若用户因此单爽约被限制且仍处限制中 → 解除限制（管理员已认定爽约误判）
        User user = userMapper.selectById(order.getUserId());
        if (user != null && user.getCreditStatus() != null && user.getCreditStatus() == 1) {
            int currentCount = creditLogMapper.countNoShowSince(user.getId(),
                    LocalDateTime.now().minusDays(CommonConstants.CREDIT_PERIOD_DAYS));
            if (userMapper.casRemoveCreditLimit(user.getId(), currentCount) > 0) {
                messageService.sendSystemMessage(user.getId(), "预约功能已恢复",
                        "您的爽约申诉已通过，相关预约限制已解除。");
                log.info("[Credit] 申诉通过解除限制: userId={}, orderNo={}", user.getId(), order.getOrderNo());
            }
        }
        log.info("[Credit] FR-AD-011 撤销爽约恢复信用: userId={}, orderNo={}, operator={}",
                order.getUserId(), order.getOrderNo(), operatorId);
        return true;
    }
}
