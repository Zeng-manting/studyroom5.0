package com.studyroom.mapper.pkg;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.pkg.PriceRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 价格加价规则 Mapper（FR-AD-025 / FR-UC-019 计费）
 */
@Mapper
public interface PriceRuleMapper extends BaseMapper<PriceRule> {
}
