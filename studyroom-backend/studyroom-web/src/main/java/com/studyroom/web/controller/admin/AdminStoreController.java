package com.studyroom.web.controller.admin;

import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.admin.BlacklistSaveDTO;
import com.studyroom.entity.dto.admin.SeatLayoutSaveDTO;
import com.studyroom.entity.dto.admin.StoreSaveDTO;
import com.studyroom.entity.vo.admin.AdminBlacklistVO;
import com.studyroom.entity.vo.admin.AdminQrCodeVO;
import com.studyroom.entity.vo.admin.AdminSeatVO;
import com.studyroom.entity.vo.admin.AdminStoreVO;
import com.studyroom.entity.vo.order.SeatMapVO;
import com.studyroom.service.admin.AdminStoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.YearMonth;
import java.util.List;

/**
 * 管理端-门店与座位管理 Controller —— FR-AD-004 ~ FR-AD-008
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/store/list</td><td>门店列表（含维护中）</td><td>FR-AD-004</td></tr>
 *   <tr><td>GET</td><td>/api/admin/store/{id}</td><td>门店详情</td><td>FR-AD-004</td></tr>
 *   <tr><td>POST</td><td>/api/admin/store</td><td>新增门店</td><td>FR-AD-004</td></tr>
 *   <tr><td>PUT</td><td>/api/admin/store/{id}</td><td>编辑门店信息</td><td>FR-AD-004</td></tr>
 *   <tr><td>GET</td><td>/api/admin/store/{id}/blacklist/rules</td><td>黑名单规则列表</td><td>FR-AD-005</td></tr>
 *   <tr><td>POST</td><td>/api/admin/store/{id}/blacklist</td><td>新增黑名单规则</td><td>FR-AD-005</td></tr>
 *   <tr><td>PUT</td><td>/api/admin/blacklist/{ruleId}</td><td>编辑黑名单规则</td><td>FR-AD-005</td></tr>
 *   <tr><td>DELETE</td><td>/api/admin/blacklist/{ruleId}</td><td>删除黑名单规则</td><td>FR-AD-005</td></tr>
 *   <tr><td>GET</td><td>/api/admin/store/{id}/blacklist/calendar</td><td>黑名单月历视图</td><td>FR-AD-005</td></tr>
 *   <tr><td>GET</td><td>/api/admin/store/{id}/seats</td><td>座位列表（画布数据）</td><td>FR-AD-006</td></tr>
 *   <tr><td>GET</td><td>/api/admin/seat-types</td><td>座位类型列表</td><td>FR-AD-006</td></tr>
 *   <tr><td>PUT</td><td>/api/admin/store/{id}/seats/layout</td><td>座位布局全量保存</td><td>FR-AD-006</td></tr>
 *   <tr><td>POST</td><td>/api/admin/seat/{seatId}/status</td><td>座位状态手动切换</td><td>FR-AD-007</td></tr>
 *   <tr><td>GET</td><td>/api/admin/store/{id}/qrcode</td><td>生成签到二维码内容</td><td>FR-AD-008</td></tr>
 * </table>
 *
 * <p>全部接口需 ADMIN 角色。</p>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "6.管理端-门店与座位")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    /* ==================== FR-AD-004：门店信息 ==================== */

    @GetMapping("/store/list")
    @ApiOperation("门店列表（管理端，含维护中）")
    public Result<List<AdminStoreVO>> listStores() {
        return Result.ok(adminStoreService.listStores());
    }

    @GetMapping("/store/{storeId}")
    @ApiOperation("门店详情")
    public Result<AdminStoreVO> getStore(@PathVariable Long storeId) {
        return Result.ok(adminStoreService.getStore(storeId));
    }

    @PostMapping("/store")
    @ApiOperation("新增门店")
    public Result<Long> createStore(@RequestBody @Valid StoreSaveDTO dto) {
        return Result.ok(adminStoreService.createStore(dto));
    }

    @PutMapping("/store/{storeId}")
    @ApiOperation("编辑门店信息")
    public Result<Void> updateStore(@PathVariable Long storeId,
                                    @RequestBody @Valid StoreSaveDTO dto) {
        adminStoreService.updateStore(storeId, dto);
        return Result.ok();
    }

    /* ==================== FR-AD-005：时段黑名单 ==================== */

    @GetMapping("/store/{storeId}/blacklist/rules")
    @ApiOperation("黑名单规则列表（含禁用）")
    public Result<List<AdminBlacklistVO>> listBlacklistRules(@PathVariable Long storeId) {
        return Result.ok(adminStoreService.listBlacklistRules(storeId));
    }

    @PostMapping("/store/{storeId}/blacklist")
    @ApiOperation("新增黑名单规则（周期性/单次）")
    public Result<Long> addBlacklistRule(@PathVariable Long storeId,
                                         @RequestBody @Valid BlacklistSaveDTO dto) {
        return Result.ok(adminStoreService.addBlacklistRule(storeId, dto));
    }

    @PutMapping("/blacklist/{ruleId}")
    @ApiOperation("编辑黑名单规则")
    public Result<Void> updateBlacklistRule(@PathVariable Long ruleId,
                                            @RequestBody @Valid BlacklistSaveDTO dto) {
        adminStoreService.updateBlacklistRule(ruleId, dto);
        return Result.ok();
    }

    @DeleteMapping("/blacklist/{ruleId}")
    @ApiOperation("删除黑名单规则")
    public Result<Void> deleteBlacklistRule(@PathVariable Long ruleId) {
        adminStoreService.deleteBlacklistRule(ruleId);
        return Result.ok();
    }

    @GetMapping("/store/{storeId}/blacklist/calendar")
    @ApiOperation("黑名单月历视图（当月每日命中情况）")
    public Result<List<AdminBlacklistVO.CalendarDay>> blacklistCalendar(
            @PathVariable Long storeId,
            @ApiParam(value = "月份 yyyy-MM，默认当月", example = "2026-09")
            @RequestParam(required = false) String month) {
        YearMonth ym = (month == null || month.isEmpty())
                ? YearMonth.now() : YearMonth.parse(month);
        return Result.ok(adminStoreService.getBlacklistCalendar(storeId, ym));
    }

    /* ==================== FR-AD-006：座位布局 ==================== */

    @GetMapping("/store/{storeId}/seats")
    @ApiOperation("座位列表（管理端画布数据：真实状态+类型+进行中订单标记）")
    public Result<List<AdminSeatVO>> listSeats(@PathVariable Long storeId) {
        return Result.ok(adminStoreService.listSeats(storeId));
    }

    @GetMapping("/seat-types")
    @ApiOperation("座位类型列表（画布图例/编辑选项）")
    public Result<List<SeatMapVO.SeatTypeVO>> listSeatTypes() {
        return Result.ok(adminStoreService.listSeatTypes());
    }

    @PutMapping("/store/{storeId}/seats/layout")
    @ApiOperation("座位布局全量同步保存（拖拽编辑画布落库）")
    public Result<Void> saveSeatLayout(@PathVariable Long storeId,
                                       @RequestBody @Valid SeatLayoutSaveDTO dto) {
        adminStoreService.saveSeatLayout(storeId, dto);
        return Result.ok();
    }

    /* ==================== FR-AD-007：座位状态管理 ==================== */

    @PostMapping("/seat/{seatId}/status")
    @ApiOperation("手动切换座位状态（维护/恢复空闲/打扫完成/锁定-解锁）")
    public Result<Void> changeSeatStatus(
            @PathVariable Long seatId,
            @ApiParam(value = "目标状态：0空闲 2维护 4锁定", required = true)
            @RequestParam Integer targetStatus) {
        adminStoreService.changeSeatStatus(seatId, targetStatus);
        return Result.ok();
    }

    /* ==================== FR-AD-008：签到二维码 ==================== */

    @GetMapping("/store/{storeId}/qrcode")
    @ApiOperation("生成签到二维码（AES加密+HMAC签名，前端渲染/下载/打印）")
    public Result<AdminQrCodeVO> generateQrCode(
            @PathVariable Long storeId,
            @ApiParam(value = "有效小时数 1~72，默认 4", example = "4")
            @RequestParam(defaultValue = "4") int validHours) {
        return Result.ok(adminStoreService.generateQrCode(storeId, validHours));
    }
}
