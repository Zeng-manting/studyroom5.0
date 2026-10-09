package com.studyroom.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.common.utils.JacksonUtils;
import com.studyroom.entity.dto.user.FeedbackCreateDTO;
import com.studyroom.entity.dto.user.RechargeDTO;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.order.UserBalanceLog;
import com.studyroom.entity.user.Feedback;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.user.BalanceLogVO;
import com.studyroom.entity.vo.user.StudyStatVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.order.UserBalanceLogMapper;
import com.studyroom.mapper.user.FeedbackMapper;
import com.studyroom.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人中心服务实现（FR-UC-048/049/050/052）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    /** 热力图回溯天数（FR-UC-049 打卡日历） */
    private static final int HEATMAP_DAYS = 90;

    private final UserMapper userMapper;
    private final UserBalanceLogMapper balanceLogMapper;
    private final OrderMapper orderMapper;
    private final FeedbackMapper feedbackMapper;

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null || user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal recharge(Long userId, RechargeDTO dto) {
        // FR-UC-048：模拟微信支付充值（真实环境为统一下单+回调），原子加余额
        userMapper.addBalance(userId, dto.getAmount());
        User after = userMapper.selectById(userId);

        // 充值流水（TYPE_RECHARGE）
        UserBalanceLog balanceLog = new UserBalanceLog();
        balanceLog.setId(IdGenerator.nextId());
        balanceLog.setUserId(userId);
        balanceLog.setType(UserBalanceLog.TYPE_RECHARGE);
        balanceLog.setAmount(dto.getAmount());
        balanceLog.setBalanceBefore(after.getBalance().subtract(dto.getAmount()));
        balanceLog.setBalanceAfter(after.getBalance());
        balanceLog.setBizType("recharge");
        balanceLog.setBizId(IdGenerator.nextId());
        balanceLog.setRemark("模拟微信支付充值");
        balanceLogMapper.insert(balanceLog);

        log.info("[Profile] FR-UC-048 充值成功: userId={}, amount={}, balance={}",
                userId, dto.getAmount(), after.getBalance());
        return after.getBalance();
    }

    @Override
    public PageResult<BalanceLogVO> pageBalanceLogs(Long userId, long page, long size) {
        Page<UserBalanceLog> p = balanceLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserBalanceLog>()
                        .eq(UserBalanceLog::getUserId, userId)
                        .orderByDesc(UserBalanceLog::getCreateTime));
        List<BalanceLogVO> vos = new ArrayList<>();
        for (UserBalanceLog l : p.getRecords()) {
            BalanceLogVO vo = new BalanceLogVO();
            vo.setId(l.getId());
            vo.setType(l.getType());
            vo.setTypeDesc(typeDesc(l.getType()));
            vo.setAmount(l.getAmount());
            vo.setBalanceAfter(l.getBalanceAfter());
            vo.setRemark(l.getRemark());
            vo.setCreateTime(l.getCreateTime());
            vos.add(vo);
        }
        return new PageResult<>(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public StudyStatVO studyStats(Long userId) {
        StudyStatVO vo = new StudyStatVO();

        // FR-UC-049 累计统计（口径：已支付订单 2/3/6/7）
        Map<String, Object> stats = orderMapper.selectUserOrderStats(userId);
        vo.setTotalMinutes(toLong(stats == null ? null : stats.get("totalMinutes")));
        vo.setTotalDays(toLong(stats == null ? null : stats.get("totalDays")));
        vo.setTotalConsume(toDecimal(stats == null ? null : stats.get("totalConsume")));
        vo.setTotalOrders(toLong(stats == null ? null : stats.get("totalOrders")));

        // 24 小时时段分布（下标=小时）
        List<Long> hours = new ArrayList<>(Arrays.asList(new Long[24]));
        for (int i = 0; i < 24; i++) {
            hours.set(i, 0L);
        }
        for (Map<String, Object> row : orderMapper.selectHourDistribution(userId)) {
            int hour = ((Number) row.get("hour")).intValue();
            hours.set(hour, toLong(row.get("cnt")));
        }
        vo.setHourDistribution(hours);

        // 近 90 天打卡热力图（含无数据日，前端按日期网格渲染）
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(HEATMAP_DAYS - 1);
        Map<String, Long> dayMinutes = new HashMap<>();
        for (Map<String, Object> row : orderMapper.selectDailyHeatmap(userId, since.atStartOfDay())) {
            dayMinutes.put(String.valueOf(row.get("day")), toLong(row.get("minutes")));
        }
        List<StudyStatVO.DayHeat> heatmap = new ArrayList<>();
        for (LocalDate d = since; !d.isAfter(today); d = d.plusDays(1)) {
            StudyStatVO.DayHeat heat = new StudyStatVO.DayHeat();
            heat.setDate(d.toString());
            heat.setMinutes(dayMinutes.getOrDefault(d.toString(), 0L));
            heatmap.add(heat);
        }
        vo.setHeatmap(heatmap);
        return vo;
    }

    @Override
    public void agreePrivacy(Long userId) {
        // FR-UC-050：幂等（已同意重复调用无副作用）
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPrivacyAgreed, 1)
                .set(User::getPrivacyAgreeTime, LocalDateTime.now()));
    }

    @Override
    public Long submitFeedback(Long userId, FeedbackCreateDTO dto) {
        Feedback feedback = new Feedback();
        feedback.setId(IdGenerator.nextId());
        feedback.setUserId(userId);
        feedback.setType(dto.getType());
        feedback.setContent(dto.getContent());
        // 图片 URL 数组 → JSON
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                feedback.setImages(JacksonUtils.defaultMapper().writeValueAsString(dto.getImages()));
            } catch (Exception e) {
                log.warn("[Profile] 反馈图片序列化失败: {}", e.getMessage());
            }
        }
        feedback.setContact(dto.getContact());
        feedback.setStatus(Feedback.STATUS_PENDING);
        feedbackMapper.insert(feedback);
        log.info("[Profile] FR-UC-052 反馈提交: userId={}, type={}", userId, dto.getType());
        return feedback.getId();
    }

    /* ==================== 私有辅助 ==================== */

    private String typeDesc(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case UserBalanceLog.TYPE_RECHARGE: return "充值";
            case UserBalanceLog.TYPE_CONSUME: return "消费";
            case UserBalanceLog.TYPE_REFUND: return "退款";
            case UserBalanceLog.TYPE_FREEZE: return "冻结";
            case UserBalanceLog.TYPE_UNFREEZE: return "解冻";
            case UserBalanceLog.TYPE_ADJUST: return "管理员调整";
            default: return "未知";
        }
    }

    private Long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v instanceof BigDecimal ? (BigDecimal) v : new BigDecimal(String.valueOf(v));
    }
}
