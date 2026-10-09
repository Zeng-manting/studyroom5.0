package com.studyroom.service.message;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.dto.user.SubscribeMessageDTO;
import com.studyroom.entity.vo.message.MessageVO;

/**
 * 消息中心服务接口
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-044 订阅消息授权上报（uni.requestSubscribeMessage 回调）</li>
 *   <li>FR-UC-045 新增提醒：优惠券到期/余额不足/座位即将释放（订单链路触发）</li>
 *   <li>FR-UC-047 消息中心：分类列表、左滑删除、全部已读、未读数</li>
 * </ul>
 */
public interface MessageService {

    /**
     * 消息分页列表（FR-UC-047）
     *
     * @param type 分类：0/null=全部，1系统通知 2订单消息 3活动消息
     */
    PageResult<MessageVO> listMessages(Long userId, Integer type, long page, long size);

    /** 未读消息数（首页角标） */
    long unreadCount(Long userId);

    /** 单条标记已读（进入详情/点击时调用） */
    void readMessage(Long userId, Long messageId);

    /** 全部已读（FR-UC-047） */
    void readAll(Long userId);

    /** 删除单条消息（左滑删除，逻辑删除） */
    void deleteMessage(Long userId, Long messageId);

    /** 订阅消息授权上报（FR-UC-044，uk_user_scene upsert） */
    void reportSubscribe(Long userId, SubscribeMessageDTO dto);

    /* ==================== 业务触发发送（供订单/超时链路调用） ==================== */

    /** 发送订单类消息（预约成功/即将开始/取消/退款/超时等，FR-UC-045/046） */
    void sendOrderMessage(Long userId, String title, String content, Long bizId);

    /** 发送系统通知 */
    void sendSystemMessage(Long userId, String title, String content);

    /** 发送活动消息 */
    void sendActivityMessage(Long userId, String title, String content);
}
