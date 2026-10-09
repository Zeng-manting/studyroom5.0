package com.studyroom.web.controller.store;

import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.order.PackageVO;
import com.studyroom.service.pkg.PricingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端-套餐 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/package/list</td><td>套餐列表（四选项卡）</td><td>FR-UC-019</td></tr>
 * </table>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/package")
@RequiredArgsConstructor
@Api(tags = "用户端-套餐")
public class PackageController {

    private final PricingService pricingService;

    /**
     * 套餐列表 —— FR-UC-019 套餐计费入口
     *
     * @param type 1按时长 2日卡 3周卡 4月卡（空 = 全部）
     */
    @GetMapping("/list")
    @ApiOperation("套餐列表（1按时长 2日卡 3周卡 4月卡）")
    public Result<List<PackageVO>> list(
            @ApiParam("套餐类型（空=全部）") @RequestParam(required = false) Integer type) {
        return Result.ok(pricingService.listPackages(type));
    }
}
