package com.studyroom.service.store;

import com.studyroom.entity.vo.order.SeatMapVO;

import java.time.LocalDateTime;

/**
 * 座位服务（FR-UC-017 Canvas 座位平面图）
 */
public interface SeatService {

    /**
     * 查询门店座位平面图（含指定时段的占用计算）
     * <p>FR-UC-017 六色状态：空闲(绿)/占用(灰)/使用中(蓝)/维护(橙)/打扫(黄)/锁定(红)</p>
     *
     * @param storeId 门店ID
     * @param start   查询时段开始
     * @param end     查询时段结束
     */
    SeatMapVO getSeatMap(Long storeId, LocalDateTime start, LocalDateTime end);
}
