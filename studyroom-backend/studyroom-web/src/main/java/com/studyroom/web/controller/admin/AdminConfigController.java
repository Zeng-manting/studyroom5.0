package com.studyroom.web.controller.admin;

import com.studyroom.common.result.Result;
import com.studyroom.entity.config.SystemConfig;
import com.studyroom.service.admin.AdminConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端-系统配置 Controller —— FR-AD-026
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/api/admin/config/list</td><td>按分组读取配置（group 空 = 全部）</td></tr>
 *   <tr><td>POST</td><td>/api/admin/config/save</td><td>批量保存配置值（只允许更新已有键）</td></tr>
 * </table>
 *
 * <p>退款规则配置（refund 分组）对应 FR-AD-026；保存即写 system_config 表，
 * 当前业务侧退款链路仍读编译期常量，本表为运营动态化预留。</p>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Api(tags = "14.管理端-系统配置")
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    @GetMapping("/list")
    @ApiOperation("按分组读取配置（group 空 = 全部分组）")
    public Result<Map<String, java.util.List<SystemConfig>>> list(
            @ApiParam("分组：refund/order/sign/pause/credit/limiter/jwt（空=全部）")
            @RequestParam(required = false) String group) {
        return Result.ok(adminConfigService.listByGroup(group));
    }

    @PostMapping("/save")
    @ApiOperation("批量保存配置值（FR-AD-026 退款规则等）")
    public Result<Void> save(@RequestBody Map<String, String> values) {
        adminConfigService.saveValues(values);
        return Result.ok();
    }
}
