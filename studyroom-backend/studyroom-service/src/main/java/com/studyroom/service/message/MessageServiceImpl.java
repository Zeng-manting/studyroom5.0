package com.studyroom.service.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.entity.dto.user.SubscribeMessageDTO;
import com.studyroom.entity.message.UserMessage;
import com.studyroom.entity.message.UserSubscribeMessage;
import com.studyroom.entity.vo.message.MessageVO;
import com.studyroom.mapper.message.UserMessageMapper;
import com.studyroom.mapper.message.UserSubscribeMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息中心服务实现（FR-UC-044/045/047）
 * <p>发送方法内部 try-catch：消息落库失败不影响订单主流程（best-effort）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserMessageMapper messageMapper;
    private final UserSubscribeMessageMapper subscribeMapper;

    @Override
    public PageResult<MessageVO> listMessages(Long userId, Integer type, long page, long size) {
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .orderByDesc(UserMessage::getCreateTime);
        // FR-UC-047 分类
        if (type != null && type > 0) {
            wrapper.eq(UserMessage::getType, type);
        }
        Page<UserMessage> p = messageMapper.selectPage(new Page<>(page, size), wrapper);
        List<MessageVO> vos = p.getRecords().stream().map(m -> {
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            vo.setType(m.getType());
            vo.setTitle(m.getTitle());
            vo.setContent(m.getContent());
            vo.setBizType(m.getBizType());
            vo.setBizId(m.getBizId());
            vo.setIsRead(m.getIsRead() != null && m.getIsRead() == 1);
            vo.setCreateTime(m.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public long unreadCount(Long userId) {
        Long cnt = messageMapper.selectCount(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0));
        return cnt == null ? 0 : cnt;
    }

    @Override
    public void readMessage(Long userId, Long messageId) {
        messageMapper.update(null, new LambdaUpdateWrapper<UserMessage>()
                .eq(UserMessage::getId, messageId)
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0)
                .set(UserMessage::getIsRead, 1)
                .set(UserMessage::getReadTime, LocalDateTime.now()));
    }

    @Override
    public void readAll(Long userId) {
        messageMapper.update(null, new LambdaUpdateWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0)
                .set(UserMessage::getIsRead, 1)
                .set(UserMessage::getReadTime, LocalDateTime.now()));
    }

    @Override
    public void deleteMessage(Long userId, Long messageId) {
        int rows = messageMapper.delete(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getId, messageId)
                .eq(UserMessage::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "消息不存在");
        }
    }

    @Override
    public void reportSubscribe(Long userId, SubscribeMessageDTO dto) {
        // FR-UC-044：uk_user_scene upsert
        UserSubscribeMessage existed = subscribeMapper.selectOne(
                new LambdaQueryWrapper<UserSubscribeMessage>()
                        .eq(UserSubscribeMessage::getUserId, userId)
                        .eq(UserSubscribeMessage::getScene, dto.getScene()));
        int accept = dto.getAcceptFlag() == null ? UserSubscribeMessage.ACCEPT_NONE : dto.getAcceptFlag();
        if (existed == null) {
            UserSubscribeMessage sub = new UserSubscribeMessage();
            sub.setId(IdGenerator.nextId());
            sub.setUserId(userId);
            sub.setScene(dto.getScene());
            sub.setTemplateId(dto.getTemplateId());
            sub.setAcceptFlag(accept);
            sub.setAcceptTime(accept == UserSubscribeMessage.ACCEPT_YES ? LocalDateTime.now() : null);
            subscribeMapper.insert(sub);
        } else {
            subscribeMapper.update(null, new LambdaUpdateWrapper<UserSubscribeMessage>()
                    .eq(UserSubscribeMessage::getId, existed.getId())
                    .set(UserSubscribeMessage::getTemplateId, dto.getTemplateId())
                    .set(UserSubscribeMessage::getAcceptFlag, accept)
                    .set(UserSubscribeMessage::getAcceptTime,
                            accept == UserSubscribeMessage.ACCEPT_YES ? LocalDateTime.now() : null));
        }
        log.info("[Message] FR-UC-044 订阅授权上报: userId={}, scene={}, accept={}",
                userId, dto.getScene(), accept);
    }

    /* ==================== 业务触发发送（best-effort） ==================== */

    @Override
    public void sendOrderMessage(Long userId, String title, String content, Long bizId) {
        send(userId, UserMessage.TYPE_ORDER, title, content, "order", bizId);
    }

    @Override
    public void sendSystemMessage(Long userId, String title, String content) {
        send(userId, UserMessage.TYPE_SYSTEM, title, content, null, null);
    }

    @Override
    public void sendActivityMessage(Long userId, String title, String content) {
        send(userId, UserMessage.TYPE_ACTIVITY, title, content, null, null);
    }

    private void send(Long userId, int type, String title, String content,
                      String bizType, Long bizId) {
        try {
            UserMessage msg = new UserMessage();
            msg.setId(IdGenerator.nextId());
            msg.setUserId(userId);
            msg.setType(type);
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBizType(bizType);
            msg.setBizId(bizId);
            msg.setIsRead(0);
            messageMapper.insert(msg);
        } catch (Exception e) {
            // FR-UC-045：消息为辅助通知，失败不影响主流程
            log.error("[Message] 消息落库失败: userId={}, title={}", userId, title, e);
        }
    }
}
