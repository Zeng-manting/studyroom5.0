package com.studyroom.web.controller.store;

import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.order.StoreBlacklistVO;
import com.studyroom.entity.vo.order.StoreDetailVO;
import com.studyroom.entity.vo.order.StoreListVO;
import com.studyroom.service.store.StoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户端-门店 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/store/list</td><td>门店列表（关键词搜索）</td><td>FR-UC-007~010</td></tr>
 *   <tr><td>GET</td><td>/api/store/{id}</td><td>门店详情（轮播/信息/地图/设施）</td><td>FR-UC-015</td></tr>
 *   <tr><td>GET</td><td>/api/store/{id}/blacklist</td><td>某日时段黑名单</td><td>FR-UC-020</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
@Api(tags = "用户端-门店")
public class StoreController {

    private final StoreService storeService;

    /**
     * 门店列表 —— FR-UC-007~010
     */
    @GetMapping("/list")
    @ApiOperation("门店列表（支持名称/地址关键词）")
    @RateLimiter(value = "store:list", permits = 30, interval = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
    public Result<List<StoreListVO>> list(
            @ApiParam("关键词") @RequestParam(required = false) String keyword) {
        return Result.ok(storeService.listStores(keyword));
    }

    /**
     * 门店详情 —— FR-UC-015 详情页展示
     */
    @GetMapping("/{id}")
    @ApiOperation("门店详情")
    public Result<StoreDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(storeService.getDetail(id));
    }

    /**
     * 某日时段黑名单 —— FR-UC-020（前端时段选择器置灰）
     */
    @GetMapping("/{id}/blacklist")
    @ApiOperation("门店某日时段黑名单")
    public Result<StoreBlacklistVO> blacklist(
            @PathVariable("id") Long id,
            @ApiParam(value = "日期 yyyy-MM-dd", defaultValue = "今天")
            @RequestParam(value = "date", required = false) String date) {
        LocalDate target = (date == null || date.isEmpty()) ? LocalDate.now() : LocalDate.parse(date);
        return Result.ok(storeService.getBlacklist(id, target));
    }
}
