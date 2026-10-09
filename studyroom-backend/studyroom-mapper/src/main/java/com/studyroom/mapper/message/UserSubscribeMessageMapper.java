package com.studyroom.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.message.UserSubscribeMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订阅消息授权 Mapper —— FR-UC-044
 */
@Mapper
public interface UserSubscribeMessageMapper extends BaseMapper<UserSubscribeMessage> {
}
