package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.OrderSign;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签到记录 Mapper（FR-UC-029/030/031）
 */
@Mapper
public interface OrderSignMapper extends BaseMapper<OrderSign> {
}
