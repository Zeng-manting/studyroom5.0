package com.studyroom.mapper.store;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.store.StoreBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店时段黑名单 Mapper（FR-UC-020 / FR-AD-005）
 */
@Mapper
public interface StoreBlacklistMapper extends BaseMapper<StoreBlacklist> {
}
