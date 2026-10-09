package com.studyroom.mapper.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.review.ReviewImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价图片 Mapper —— FR-UC-039（最多 6 张）
 */
@Mapper
public interface ReviewImageMapper extends BaseMapper<ReviewImage> {
}
