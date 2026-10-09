package com.studyroom.web.controller.user;

import com.studyroom.common.annotation.CurrentUser;
import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.result.Result;
import com.studyroom.entity.dto.user.BindEmailDTO;
import com.studyroom.entity.dto.user.SendEmailCodeDTO;
import com.studyroom.entity.dto.user.UpdateProfileDTO;
import com.studyroom.entity.vo.user.EmailStatusVO;
import com.studyroom.entity.vo.user.UserVO;
import com.studyroom.service.user.UserInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.TimeUnit;

/**
 * 用户端-账号 Controller（需登录）
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>GET</td><td>/api/user/me</td><td>当前用户信息</td><td>FR-UC-003</td></tr>
 *   <tr><td>PUT</td><td>/api/user/profile</td><td>更新头像/昵称/性别</td><td>FR-UC-003</td></tr>
 *   <tr><td>GET</td><td>/api/user/email</td><td>邮箱绑定状态（脱敏）</td><td>FR-UC-004</td></tr>
 *   <tr><td>POST</td><td>/api/user/email/code</td><td>发送邮箱验证码</td><td>FR-UC-004</td></tr>
 *   <tr><td>POST</td><td>/api/user/email/bind</td><td>验证码校验并绑定邮箱</td><td>FR-UC-004</td></tr>
 * </table>
 *
 * <p>所有接口经 JwtAuthenticationFilter 鉴权，@CurrentUser 注入当前登录用户。</p>
 *
 * @author studyroom
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Api(tags = "用户端-账号")
public class UserController {

    private final UserInfoService userInfoService;

    /** 当前用户信息（进入个人中心首页拉取）—— FR-UC-003 */
    @GetMapping("/me")
    @ApiOperation("当前登录用户信息")
    public Result<UserVO> me(@CurrentUser LoginUser loginUser) {
        return Result.ok(userInfoService.getMyInfo(loginUser.getId()));
    }

    /** 更新用户资料 —— FR-UC-003（微信 chooseAvatar + nickname 填写能力） */
    @PutMapping("/profile")
    @ApiOperation("更新头像/昵称/性别")
    @RateLimiter(value = "user:profile", permits = 10, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.USER)
    public Result<UserVO> updateProfile(@CurrentUser LoginUser loginUser,
                                        @Valid @RequestBody UpdateProfileDTO dto) {
        return Result.ok(userInfoService.updateProfile(loginUser.getId(), dto));
    }

    /** 邮箱绑定状态 —— FR-UC-004 */
    @GetMapping("/email")
    @ApiOperation("邮箱绑定状态（脱敏地址）")
    public Result<EmailStatusVO> emailStatus(@CurrentUser LoginUser loginUser) {
        return Result.ok(userInfoService.getEmailStatus(loginUser.getId()));
    }

    /** 发送邮箱验证码 —— FR-UC-004（业务层含 60s 冷却 + 每日 5 次上限） */
    @PostMapping("/email/code")
    @ApiOperation("发送邮箱验证码")
    @RateLimiter(value = "user:email:code", permits = 3, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.USER)
    public Result<Void> sendEmailCode(@CurrentUser LoginUser loginUser,
                                      @Valid @RequestBody SendEmailCodeDTO dto) {
        userInfoService.sendEmailCode(loginUser.getId(), dto);
        return Result.ok();
    }

    /** 绑定邮箱 —— FR-UC-004（验证码一次性消费，邮箱 AES-256-CBC 加密落库） */
    @PostMapping("/email/bind")
    @ApiOperation("绑定邮箱（验证码校验）")
    @RateLimiter(value = "user:email:bind", permits = 5, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.USER)
    public Result<Void> bindEmail(@CurrentUser LoginUser loginUser,
                                  @Valid @RequestBody BindEmailDTO dto) {
        userInfoService.bindEmail(loginUser.getId(), dto);
        return Result.ok();
    }
}
