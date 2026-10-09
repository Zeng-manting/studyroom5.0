package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.OrderExtension;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单续时记录 Mapper（FR-UC-055）
 *
 * @author studyroom
 */
@Mapper
public interface OrderExtensionMapper extends BaseMapper<OrderExtension> {
}
