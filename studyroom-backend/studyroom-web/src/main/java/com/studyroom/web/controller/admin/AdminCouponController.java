package com.studyroom.web.controller.admin;

import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.admin.CouponGrantDTO;
import com.studyroom.entity.dto.admin.CouponSaveDTO;
import com.studyroom.entity.vo.admin.AdminCouponVO;
import com.studyroom.service.admin.AdminCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
 * 管理端-优惠券配置 Controller —— FR-AD-027
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/api/admin/coupon/page</td><td>优惠券分页列表（含下架）</td></tr>
 *   <tr><td>POST</td><td>/api/admin/coupon/save</td><td>新建/编辑优惠券</td></tr>
 *   <tr><td>POST</td><td>/api/admin/coupon/{id}/status</td><td>上/下架</td></tr>
 *   <tr><td>POST</td><td>/api/admin/coupon/grant</td><td>定向发放（按用户ID列表）</td></tr>
 *   <tr><td>GET</td><td>/api/admin/coupon/{id}/claims</td><td>领取明细（最近 100 条）</td></tr>
 * </table>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/coupon")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "13.管理端-优惠券管理")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @GetMapping("/page")
    @ApiOperation("优惠券分页列表")
    public Result<PageResult<AdminCouponVO>> page(
            @ApiParam("类型：1新人券 2时长抵扣券 3满减券") @RequestParam(required = false) Integer type,
            @ApiParam("状态：0下架 1上架") @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminCouponService.pageCoupons(type, status, page, size));
    }

    @PostMapping("/save")
    @ApiOperation("新建/编辑优惠券（id 空 = 新建）")
    public Result<Long> save(@Valid @RequestBody CouponSaveDTO dto) {
        return Result.ok(adminCouponService.saveCoupon(dto, AuthContextHolder.require().getId()));
    }

    @PostMapping("/{id}/status")
    @ApiOperation("上架/下架（下架后不可再领取，已领取的继续可用）")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam boolean online) {
        adminCouponService.toggleStatus(id, online);
        return Result.ok();
    }

    @PostMapping("/grant")
    @ApiOperation("定向发放优惠券（按用户ID列表写 coupon_claim）")
    public Result<Integer> grant(@Valid @RequestBody CouponGrantDTO dto) {
        return Result.ok(adminCouponService.grantCoupon(dto));
    }

    @GetMapping("/{id}/claims")
    @ApiOperation("领取明细（最近 100 条，发放效果跟踪）")
    public Result<List<AdminCouponVO.ClaimRecordItem>> claims(@PathVariable Long id) {
        return Result.ok(adminCouponService.listClaims(id));
    }
}
