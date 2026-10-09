package com.studyroom.web.controller;

import com.studyroom.common.annotation.Idempotent;
import com.studyroom.common.aspect.IdempotentAspect;
import com.studyroom.common.enums.UserTypeEnum;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.properties.JwtProperties;
import com.studyroom.common.result.Result;
import com.studyroom.common.utils.JwtTokenProvider;
import com.studyroom.common.utils.RedisUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 公共 Controller
 * <ul>
 *   <li>{@code GET /api/idempotent/token?biz=xxx&ttl=60} —— 签发幂等 token（公开，限流保护）</li>
 *   <li>{@code POST /api/logout} —— 登出（将当前 token 列入 Redis 黑名单）</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "公共接口")
public class CommonController {

    private final IdempotentAspect idempotentAspect;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtils redisUtils;
    private final JwtProperties jwtProperties;

    @GetMapping("/api/idempotent/token")
    @ApiOperation("获取幂等 token（前端表单提交前预取）")
    public Result<Map<String, String>> issueToken(
            @ApiParam(value = "业务域 key（order:create / refund / sign ...）", required = true)
            @RequestParam String biz,
            @ApiParam(value = "token 有效期（秒），默认 60", defaultValue = "60")
            @RequestParam(defaultValue = "60") int ttl) {
        String token = idempotentAspect.issue(biz, ttl, TimeUnit.SECONDS, Idempotent.KeyType.USER);
        Map<String, String> data = new HashMap<>(4);
        data.put("token", token);
        data.put("header", "X-Idempotent-Token");
        data.put("ttlSeconds", String.valueOf(ttl));
        data.put("biz", biz);
        return Result.ok(data);
    }

    @PostMapping("/api/logout")
    @ApiOperation("登出：把当前 token 列入 Redis 黑名单")
    public Result<Void> logout() {
        LoginUser u = AuthContextHolder.get();
        if (u == null || u.getJti() == null) {
            return Result.ok();
        }
        String key = jwtProperties.getBlacklistPrefix() + u.getJti();
        long ttl = UserTypeEnum.ADMIN.equals(u.getUserType())
                ? jwtProperties.getAdminTtlSeconds()
                : jwtProperties.getUserTtlSeconds();
        redisUtils.set(key, "1", Duration.ofSeconds(ttl));
        log.info("[CommonController] 用户登出 jti={}, userType={}, uid={}", u.getJti(), u.getUserType(), u.getId());
        return Result.ok();
    }
}