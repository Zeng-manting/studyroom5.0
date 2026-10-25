package com.studyroom.web.controller.user;

import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.user.H5LoginDTO;
import com.studyroom.entity.dto.user.WxLoginDTO;
import com.studyroom.entity.vo.user.LoginVO;
import com.studyroom.service.auth.AuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户端-登录 Controller
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>POST</td><td>/api/user/wx-login</td><td>微信静默登录（含自动注册）</td><td>FR-UC-001/002/005</td></tr>
 *   <tr><td>POST</td><td>/api/user/h5-login</td><td>H5 端账号登录（含自动注册）</td><td>H5 适配</td></tr>
 * </table>
 *
 * <p>限流：按 IP 10 次/分钟（FR-SY-001，code2Session 微信侧有频控，保护我方接口）。</p>
 *
 * @author studyroom
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Api(tags = "用户端-登录")
public class AuthController {

    private final AuthService authService;

    /**
     * 微信静默登录 —— FR-UC-001 / FR-UC-002 / FR-UC-005
     *
     * <p>前端 uni.login() 获取 code 后调用；openid 不存在自动注册（昵称"自习用户"）。
     * 返回 JWT（7 天有效），前端存 Storage 并自动注入请求头。</p>
     */
    @PostMapping("/wx-login")
    @ApiOperation("微信静默登录（首次自动注册）")
    @RateLimiter(value = "user:wx-login", permits = 10, interval = 1,
            unit = java.util.concurrent.TimeUnit.MINUTES, key = RateLimiter.KeyType.IP)
    public Result<LoginVO> wxLogin(@Valid @RequestBody WxLoginDTO dto) {
        return Result.ok(authService.wxLogin(dto));
    }

    /**
     * H5 端账号登录
     *
     * <p>H5 环境无微信登录能力，前端提交账号后由后端按账号建档/复用用户，
     * 返回与小程序一致的 JWT（7 天有效）。</p>
     */
    @PostMapping("/h5-login")
    @ApiOperation("H5 端账号登录（首次自动注册）")
    @RateLimiter(value = "user:h5-login", permits = 10, interval = 1,
            unit = java.util.concurrent.TimeUnit.MINUTES, key = RateLimiter.KeyType.IP)
    public Result<LoginVO> h5Login(@Valid @RequestBody H5LoginDTO dto) {
        return Result.ok(authService.h5Login(dto));
    }
}
