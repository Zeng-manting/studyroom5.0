package com.studyroom.web.controller.admin;

import com.studyroom.common.annotation.OperationLog;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.admin.PackageSaveDTO;
import com.studyroom.entity.dto.admin.PriceRuleSaveDTO;
import com.studyroom.entity.dto.admin.SeatTypeSaveDTO;
import com.studyroom.entity.vo.admin.AdminPriceVO;
import com.studyroom.service.admin.AdminPricingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 管理端-价格配置 Controller —— FR-AD-025
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/api/admin/price/packages</td><td>套餐列表（含下架）</td></tr>
 *   <tr><td>POST</td><td>/api/admin/price/package/save</td><td>新建/编辑套餐</td></tr>
 *   <tr><td>POST</td><td>/api/admin/price/package/{id}/status</td><td>套餐上/下架</td></tr>
 *   <tr><td>GET</td><td>/api/admin/price/rules</td><td>时段加价规则列表</td></tr>
 *   <tr><td>POST</td><td>/api/admin/price/rule/save</td><td>新建/编辑加价规则</td></tr>
 *   <tr><td>DELETE</td><td>/api/admin/price/rule/{id}</td><td>删除加价规则</td></tr>
 *   <tr><td>GET</td><td>/api/admin/price/seat-types</td><td>座位类型列表</td></tr>
 *   <tr><td>POST</td><td>/api/admin/price/seat-type/save</td><td>新建/编辑座位类型</td></tr>
 * </table>
 *
 * <p>数据源关系：套餐/规则变更即刻影响用户端 FR-UC-019 套餐列表与
 * PricingService 计价引擎（计价查询实时读库）。</p>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/price")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "12.管理端-价格配置")
public class AdminPricingController {

    private final AdminPricingService adminPricingService;

    /* ==================== 套餐 ==================== */

    @GetMapping("/packages")
    @ApiOperation("套餐列表（含下架）")
    public Result<List<AdminPriceVO>> listPackages() {
        return Result.ok(adminPricingService.listPackages());
    }

    @PostMapping("/package/save")
    @ApiOperation("新建/编辑套餐（id 空 = 新建）")
    public Result<Long> savePackage(@Valid @RequestBody PackageSaveDTO dto) {
        return Result.ok(adminPricingService.savePackage(dto));
    }

    @PostMapping("/package/{id}/status")
    @ApiOperation("套餐上/下架")
    public Result<Void> togglePackageStatus(
            @PathVariable Long id, @RequestParam boolean online) {
        adminPricingService.togglePackageStatus(id, online);
        return Result.ok();
    }

    /* ==================== 时段加价规则 ==================== */

    @GetMapping("/rules")
    @ApiOperation("时段加价规则列表（含禁用）")
    public Result<List<AdminPriceVO>> listPriceRules() {
        return Result.ok(adminPricingService.listPriceRules());
    }

    @PostMapping("/rule/save")
    @ApiOperation("新建/编辑加价规则（id 空 = 新建）")
    public Result<Long> savePriceRule(@Valid @RequestBody PriceRuleSaveDTO dto) {
        return Result.ok(adminPricingService.savePriceRule(dto));
    }

    @DeleteMapping("/rule/{id}")
    @ApiOperation("删除加价规则")
    public Result<Void> deletePriceRule(@PathVariable Long id) {
        adminPricingService.deletePriceRule(id);
        return Result.ok();
    }

    /* ==================== 座位类型 ==================== */

    @GetMapping("/seat-types")
    @ApiOperation("座位类型列表（含各类型座位数）")
    public Result<List<AdminPriceVO>> listSeatTypes() {
        return Result.ok(adminPricingService.listSeatTypes());
    }

    @PostMapping("/seat-type/save")
    @ApiOperation("新建/编辑座位类型（id 空 = 新建）")
    public Result<Long> saveSeatType(@Valid @RequestBody SeatTypeSaveDTO dto) {
        return Result.ok(adminPricingService.saveSeatType(dto));
    }
}
