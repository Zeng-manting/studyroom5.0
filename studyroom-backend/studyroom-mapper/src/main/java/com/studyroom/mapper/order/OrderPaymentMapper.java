package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.OrderPayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单支付记录 Mapper（FR-UC-023）
 */
@Mapper
public interface OrderPaymentMapper extends BaseMapper<OrderPayment> {
}
