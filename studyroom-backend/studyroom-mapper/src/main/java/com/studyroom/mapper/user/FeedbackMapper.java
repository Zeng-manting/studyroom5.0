package com.studyroom.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.user.Feedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 意见反馈 Mapper —— FR-UC-052
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
