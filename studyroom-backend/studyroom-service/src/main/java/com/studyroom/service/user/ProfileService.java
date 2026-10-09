package com.studyroom.service.user;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.dto.user.FeedbackCreateDTO;
import com.studyroom.entity.dto.user.RechargeDTO;
import com.studyroom.entity.vo.user.BalanceLogVO;
import com.studyroom.entity.vo.user.StudyStatVO;

import java.math.BigDecimal;

/**
 * 个人中心服务接口（模块7）
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-048 个人信息与钱包：余额、充值、交易记录</li>
 *   <li>FR-UC-049 学习统计：累计时长/消费/天数 + 时段分布柱状图 + 打卡日历热力图</li>
 *   <li>FR-UC-050 首次隐私弹窗：隐私协议同意落库</li>
 *   <li>FR-UC-052 意见反馈</li>
 * </ul>
 */
public interface ProfileService {

    /** 当前余额（FR-UC-048） */
    BigDecimal getBalance(Long userId);

    /**
     * 充值（FR-UC-048，模拟微信支付直接到账）
     * <p>真实环境替换为微信支付统一下单 + 支付回调到账。</p>
     */
    BigDecimal recharge(Long userId, RechargeDTO dto);

    /** 余额流水分页（FR-UC-048 交易记录） */
    PageResult<BalanceLogVO> pageBalanceLogs(Long userId, long page, long size);

    /** 学习统计（FR-UC-049） */
    StudyStatVO studyStats(Long userId);

    /** 同意隐私协议（FR-UC-050） */
    void agreePrivacy(Long userId);

    /** 提交意见反馈（FR-UC-052） */
    Long submitFeedback(Long userId, FeedbackCreateDTO dto);
}
