package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.OrderRefund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款记录 Mapper（FR-UC-026/027/034 / FR-AD-012）
 */
@Mapper
public interface OrderRefundMapper extends BaseMapper<OrderRefund> {
}
