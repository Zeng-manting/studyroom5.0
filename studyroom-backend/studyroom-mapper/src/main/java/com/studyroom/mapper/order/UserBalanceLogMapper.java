package com.studyroom.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.order.UserBalanceLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 余额流水 Mapper（FR-UC-023/027/048）
 */
@Mapper
public interface UserBalanceLogMapper extends BaseMapper<UserBalanceLog> {
}
