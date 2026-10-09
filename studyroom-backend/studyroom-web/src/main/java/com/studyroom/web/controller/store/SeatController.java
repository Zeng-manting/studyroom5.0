package com.studyroom.web.controller.store;

import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.order.SeatMapVO;
import com.studyroom.service.store.SeatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户端-座位 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/seat/map</td><td>座位平面图（六色状态 + 时段占用计算）</td><td>FR-UC-017</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/seat")
@RequiredArgsConstructor
@Api(tags = "用户端-座位")
public class SeatController {

    private final SeatService seatService;

    /**
     * 座位平面图 —— FR-UC-017
     * <p>返回门店全部座位 + 画布尺寸 + 类型图例；
     * displayStatus：0空闲(绿) 1占用(灰) 2使用中(蓝) 3维护(橙) 4打扫(黄) 5锁定(红)。</p>
     */
    @GetMapping("/map")
    @ApiOperation("座位平面图（指定时段的占用计算）")
    @RateLimiter(value = "seat:map", permits = 20, interval = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Result<SeatMapVO> seatMap(
            @ApiParam(value = "门店ID", required = true) @RequestParam Long storeId,
            @ApiParam(value = "时段开始 yyyy-MM-dd HH:mm", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime start,
            @ApiParam(value = "时段结束 yyyy-MM-dd HH:mm", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime end) {
        return Result.ok(seatService.getSeatMap(storeId, start, end));
    }
}
