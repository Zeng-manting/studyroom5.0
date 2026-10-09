package com.studyroom.service.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.SeatType;
import com.studyroom.entity.vo.order.SeatMapVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.SeatTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 座位服务实现（FR-UC-017）
 * <p>座位占用不由座位表状态直接决定，而是由订单状态保证（FR-UC-018：
 * 待支付订单也占用座位 15 分钟，与 FR-UC-025 支付超时窗口一致）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatMapper seatMapper;
    private final SeatTypeMapper seatTypeMapper;
    private final OrderMapper orderMapper;

    @Override
    public SeatMapVO getSeatMap(Long storeId, LocalDateTime start, LocalDateTime end) {
        // 1. 查询门店全部有效座位
        LambdaQueryWrapper<Seat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seat::getStoreId, storeId).eq(Seat::getDeleted, 0)
                .orderByAsc(Seat::getFloor).orderByAsc(Seat::getSeatNo);
        List<Seat> seats = seatMapper.selectList(wrapper);

        // 2. 计算时段占用（灰）与正在使用（蓝）
        Set<Long> occupiedIds = new HashSet<>(orderMapper.selectOccupiedSeatIds(storeId, start, end));
        Set<Long> inUseIds = new HashSet<>(orderMapper.selectInUseSeatIds(storeId));

        // 3. 座位类型图例
        LambdaQueryWrapper<SeatType> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.eq(SeatType::getStatus, 1).orderByAsc(SeatType::getSort);
        List<SeatMapVO.SeatTypeVO> typeVOs = seatTypeMapper.selectList(typeWrapper).stream()
                .map(t -> {
                    SeatMapVO.SeatTypeVO tv = new SeatMapVO.SeatTypeVO();
                    tv.setId(t.getId());
                    tv.setName(t.getName());
                    tv.setCode(t.getCode());
                    tv.setIcon(t.getIcon());
                    return tv;
                }).collect(Collectors.toList());

        // 4. 组装 VO
        SeatMapVO vo = new SeatMapVO();
        vo.setSeatTypes(typeVOs);
        vo.setSeats(seats.stream().map(seat -> toSeatVO(seat, occupiedIds, inUseIds))
                .collect(Collectors.toList()));

        // 5. 画布尺寸：坐标最大值 + 边距
        int maxX = seats.stream().mapToInt(s -> s.getXCoord() == null ? 0 : s.getXCoord()).max().orElse(0);
        int maxY = seats.stream().mapToInt(s -> s.getYCoord() == null ? 0 : s.getYCoord()).max().orElse(0);
        vo.setCanvasWidth(maxX + 60);
        vo.setCanvasHeight(maxY + 60);
        return vo;
    }

    /**
     * 单座位展示状态计算（FR-UC-017 六色）：
     * <ul>
     *   <li>seat.status=2 维护 → 3(橙)，不可选</li>
     *   <li>seat.status=3 打扫中 → 4(黄)，不可选</li>
     *   <li>seat.status=4 锁定 → 5(红)，不可选</li>
     *   <li>时段有重叠订单 → 1 占用(灰)，不可选</li>
     *   <li>此刻正在使用 → 2 使用中(蓝)，不可选</li>
     *   <li>其余 → 0 空闲(绿)，可选</li>
     * </ul>
     */
    private SeatMapVO.SeatVO toSeatVO(Seat seat, Set<Long> occupiedIds, Set<Long> inUseIds) {
        SeatMapVO.SeatVO vo = new SeatMapVO.SeatVO();
        vo.setId(seat.getId());
        vo.setSeatNo(seat.getSeatNo());
        vo.setSeatTypeId(seat.getSeatTypeId());
        vo.setFloor(seat.getFloor());
        vo.setXCoord(seat.getXCoord());
        vo.setYCoord(seat.getYCoord());

        int display;
        if (seat.getStatus() != null && seat.getStatus() == 2) {
            display = 3;
        } else if (seat.getStatus() != null && seat.getStatus() == 3) {
            display = 4;
        } else if (seat.getStatus() != null && seat.getStatus() == 4) {
            display = 5;
        } else if (inUseIds.contains(seat.getId())) {
            display = 2;
        } else if (occupiedIds.contains(seat.getId())) {
            display = 1;
        } else {
            display = 0;
        }
        vo.setDisplayStatus(display);
        vo.setStatusDesc(desc(display));
        // 仅空闲(绿)可选
        vo.setSelectable(display == 0);
        return vo;
    }

    private String desc(int display) {
        switch (display) {
            case 0: return "空闲";
            case 1: return "占用";
            case 2: return "使用中";
            case 3: return "维护";
            case 4: return "打扫中";
            case 5: return "锁定";
            default: return "未知";
        }
    }
}
