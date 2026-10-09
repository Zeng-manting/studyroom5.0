package com.studyroom.service.order;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.order.UserBalanceLog;
import com.studyroom.entity.user.User;
import com.studyroom.mapper.order.UserBalanceLogMapper;
import com.studyroom.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 钱包服务实现（FR-UC-023/027/048）
 * <p>扣减依赖 user.balance >= amount 的原子 UPDATE（UserMapper.deductBalance），
 * 事务内写流水；流水 balance_before/after 以扣减后重读为准。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserMapper userMapper;
    private final UserBalanceLogMapper balanceLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payFromBalance(Long userId, BigDecimal amount, Long orderId, String orderNo) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // FR-UC-023 余额支付：原子扣减，余额不足直接失败
        int rows = userMapper.deductBalance(userId, amount);
        if (rows == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "余额不足，请选择微信支付或充值");
        }
        User after = userMapper.selectById(userId);
        saveLog(userId, UserBalanceLog.TYPE_CONSUME, amount.negate(), after.getBalance(),
                "order", orderId, "余额支付订单 " + orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundToBalance(Long userId, BigDecimal amount, Long orderId, String remark) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        userMapper.addBalance(userId, amount);
        User after = userMapper.selectById(userId);
        saveLog(userId, UserBalanceLog.TYPE_REFUND, amount, after.getBalance(),
                "refund", orderId, remark);
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null || user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    private void saveLog(Long userId, int type, BigDecimal amount, BigDecimal balanceAfter,
                         String bizType, Long bizId, String remark) {
        UserBalanceLog balanceLog = new UserBalanceLog();
        balanceLog.setUserId(userId);
        balanceLog.setType(type);
        balanceLog.setAmount(amount);
        balanceLog.setBalanceBefore(balanceAfter.subtract(amount));
        balanceLog.setBalanceAfter(balanceAfter);
        balanceLog.setBizType(bizType);
        balanceLog.setBizId(bizId);
        balanceLog.setRemark(remark);
        balanceLogMapper.insert(balanceLog);
    }
}
