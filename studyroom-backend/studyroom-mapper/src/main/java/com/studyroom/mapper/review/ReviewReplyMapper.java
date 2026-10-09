package com.studyroom.mapper.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.review.ReviewReply;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价回复 Mapper —— FR-UC-040 管理员回复展示
 */
@Mapper
public interface ReviewReplyMapper extends BaseMapper<ReviewReply> {
}
