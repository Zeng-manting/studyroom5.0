package com.studyroom.mapper.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.review.Review;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价 Mapper —— FR-UC-039/040
 * <p>一单一评由 uk_order 唯一键保证；基础 CRUD 由 BaseMapper 提供。</p>
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
