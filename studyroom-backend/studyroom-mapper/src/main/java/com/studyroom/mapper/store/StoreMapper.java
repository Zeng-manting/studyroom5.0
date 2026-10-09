package com.studyroom.mapper.store;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.store.Store;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店 Mapper（FR-UC-007~015 / FR-AD-004）
 */
@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}
