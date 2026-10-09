package com.studyroom.web.controller.user;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.Result;
import com.studyroom.entity.vo.user.CreditVO;
import com.studyroom.service.user.CreditService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端-信用 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/credit/me</td><td>我的信用（分数/爽约记录/限制状态/流水）</td><td>FR-UC-053/054</td></tr>
 * </table>
 *
 * <p>说明：FR-UC-054 下单拦截在下单链路内生效（createOrder 前置校验），
 * 管理端"查看爽约记录 + 手动解除限制"由模块9 管理端 Controller 调用 CreditService 提供。</p>
 *
 * @author studyroom
 */
@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
@Api(tags = "用户端-信用")
public class CreditController {

    private final CreditService creditService;

    /**
     * 我的信用 —— FR-UC-053 爽约记录累计 / FR-UC-054 限制状态与到期时间
     */
    @GetMapping("/me")
    @ApiOperation("我的信用（信用分、30天爽约次数、限制状态、爽约明细、变更流水）")
    public Result<CreditVO> me(@CurrentUser LoginUser user) {
        return Result.ok(creditService.getMyCredit(user.getId()));
    }
}
