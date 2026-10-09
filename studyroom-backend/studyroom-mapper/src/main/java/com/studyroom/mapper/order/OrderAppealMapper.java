package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.OrderAppeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 签到申诉 Mapper（FR-UC-032 提交 / FR-AD-011 处理）
 */
@Mapper
public interface OrderAppealMapper extends BaseMapper<OrderAppeal> {

    /**
     * 统计订单"待审核"申诉数（FR-UC-032 防重复提交）
     */
    @Select("SELECT COUNT(*) FROM order_appeal WHERE order_id = #{orderId} AND status = 0")
    int countPendingByOrderId(@Param("orderId") Long orderId);
}
