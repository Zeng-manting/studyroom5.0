package com.studyroom.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.message.UserMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息 Mapper —— FR-UC-047 消息中心
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {
}
