package com.studyroom.service.store;

import com.studyroom.entity.vo.order.StoreBlacklistVO;
import com.studyroom.entity.vo.order.StoreDetailVO;
import com.studyroom.entity.vo.order.StoreListVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 门店服务（用户端浏览）
 * <p>FR-UC-007~011 门店列表；FR-UC-015 详情页；FR-UC-020 时段黑名单。</p>
 */
public interface StoreService {

    /**
     * 门店列表（FR-UC-007~010，支持关键词搜索）
     *
     * @param keyword 名称/地址关键词（可空）
     * @return 列表（仅营业相关状态）
     */
    List<StoreListVO> listStores(String keyword);

    /**
     * 门店详情（FR-UC-015：图片轮播、基本信息、地图坐标、设施标签、评分）
     */
    StoreDetailVO getDetail(Long storeId);

    /**
     * 某日时段黑名单（FR-UC-020：周期性/单次闭店时段，前端置灰）
     *
     * @param storeId 门店ID
     * @param date    日期
     */
    StoreBlacklistVO getBlacklist(Long storeId, LocalDate date);

    /**
     * 校验时段是否命中黑名单（FR-UC-018 下单时后端兜底校验）
     *
     * @return true 表示命中闭店时段，不可预约
     */
    boolean isBlocked(Long storeId, LocalDate date, java.time.LocalTime start, java.time.LocalTime end);
}
