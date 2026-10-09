package com.studyroom.web.controller.admin;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.admin.AdminLoginDTO;
import com.studyroom.entity.vo.admin.AdminInfoVO;
import com.studyroom.entity.vo.admin.AdminLoginVO;
import com.studyroom.entity.vo.admin.CaptchaVO;
import com.studyroom.service.auth.AdminAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * 管理端-登录 Controller —— FR-AD-001
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/admin/captcha</td><td>图形验证码（Base64，5 分钟一次性）</td><td>FR-AD-001</td></tr>
 *   <tr><td>POST</td><td>/api/admin/login</td><td>账号+密码+验证码登录，JWT 2 小时</td><td>FR-AD-001</td></tr>
 *   <tr><td>POST</td><td>/api/admin/logout</td><td>退出登录（token 拉黑）</td><td>FR-AD-001</td></tr>
 *   <tr><td>GET</td><td>/api/admin/me</td><td>当前管理员信息</td><td>FR-AD-001</td></tr>
 * </table>
 *
 * <p>限流（FR-SY-001）：验证码 20 次/分钟/IP（防止验证码轰炸占满 Redis）；
 * 登录 10 次/分钟/IP（配合账号级 5 次失败锁定双保险）。</p>
 *
 * <p>JWT 过期：JwtTokenProvider 抛 UNAUTHORIZED(401)，前端响应拦截器跳转登录页重新认证。</p>
 *
 * @author studyroom
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Api(tags = "3.管理端-登录与权限")
public class AdminController {

    private final AdminAuthService adminAuthService;

    /**
     * 图形验证码 —— FR-AD-001
     * <p>Hutool 线干扰验证码 130x40 / 4 位；答案存 Redis 5 分钟，
     * 登录时一次性消费（无论校验结果如何均作废）。</p>
     */
    @GetMapping("/captcha")
    @ApiOperation("获取图形验证码")
    @RateLimiter(value = "admin:captcha", permits = 20, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.IP)
    public Result<CaptchaVO> captcha() {
        return Result.ok(adminAuthService.getCaptcha());
    }

    /**
     * 管理员登录 —— FR-AD-001
     * <p>账号 + 密码（BCrypt 校验）+ 图形验证码；成功返回 JWT（2 小时）与管理员信息。
     * 连续失败 5 次锁定账号 10 分钟。</p>
     */
    @PostMapping("/login")
    @ApiOperation("管理员登录")
    @RateLimiter(value = "admin:login", permits = 10, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.IP)
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.ok(adminAuthService.login(dto));
    }

    /**
     * 退出登录 —— FR-AD-001
     * <p>当前 token jti 加入 Redis 黑名单，立即失效（FR-SY-002）。</p>
     */
    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public Result<Void> logout(@CurrentUser LoginUser user) {
        adminAuthService.logout(user);
        return Result.ok();
    }

    /**
     * 当前管理员信息 —— FR-AD-001
     */
    @GetMapping("/me")
    @ApiOperation("当前管理员信息")
    public Result<AdminInfoVO> me(@CurrentUser LoginUser user) {
        return Result.ok(adminAuthService.getInfo(user.getId()));
    }
}
