# studyroom-common 模块说明

> 模块3：公共模块 —— 整个项目复用的基础设施，业务模块不再重复实现。

## 目录结构

```
com.studyroom.common/
├── annotation/       # AOP 注解：@RateLimiter / @Idempotent / @CurrentUser
├── aspect/           # AOP 实现：RateLimiterAspect / IdempotentAspect
├── constants/        # CommonConstants：Redis Key 前缀 / 分页默认值 / 角色常量
├── enums/            # 业务枚举：UserTypeEnum
├── exception/        # BusinessException / GlobalExceptionHandler (FR-SY-003)
├── login/            # 当前登录用户上下文：LoginUser / AuthContextHolder (FR-UC-001 / FR-AD-001)
├── properties/       # 配置映射：JwtProperties / CryptoProperties
├── result/           # 统一响应：Result / PageResult / ResultCode (FR-SY-003)
└── utils/            # 工具类
    ├── JwtTokenProvider      # JWT 签发/解析 (jjwt 0.11.5)
    ├── RedisUtils            # Redis 二次封装（基于 StringRedisTemplate）
    ├── RedissonUtils         # Redisson RLock / RRateLimiter 高频模板
    ├── CryptoUtils           # AES-256-CBC + HMAC-SHA256（FR-UC-004 / FR-UC-030）
    ├── PasswordUtils         # BCrypt（FR-AD-001）
    ├── IdGenerator           # 雪花 ID
    ├── JacksonUtils          # Jackson 统一配置（LocalDateTime + Long→String）
    └── WebUtils              # HttpServletRequest/Response/IP 获取
```

## FR 映射

| FR 编号 | 实现类 / 接口 |
|---------|---------------|
| FR-SY-001 接口幂等 | `@Idempotent` 注解 + `IdempotentAspect`（Redis Token）|
| FR-SY-001 接口限流 | `@RateLimiter` 注解 + `RateLimiterAspect`（Redisson RRateLimiter）|
| FR-SY-003 全局异常 | `GlobalExceptionHandler`（参数校验 / 业务 / 权限 / 系统）|
| FR-UC-001 用户端 JWT | `JwtTokenProvider.generate(LoginUser)` 7 天 TTL |
| FR-UC-004 邮箱加密 | `CryptoUtils.aesEncrypt` / `aesDecrypt` |
| FR-UC-030 签到五重校验 | `CryptoUtils.aesDecrypt` + `hmacVerify` |
| FR-AD-001 管理员登录 | `JwtTokenProvider` 2 小时 TTL + `PasswordUtils.matches` |

## 注解使用示例

### @RateLimiter

```java
// 全局 5 QPS
@RateLimiter(value = "global", permits = 5, interval = 1, unit = TimeUnit.SECONDS)

// 按登录用户：每用户每 10 秒 3 次
@RateLimiter(value = "order:create", permits = 3, interval = 10, unit = TimeUnit.SECONDS, key = RateLimiter.KeyType.USER)

// 按 IP：每 IP 每分钟 10 次
@RateLimiter(value = "sign", permits = 10, interval = 1, unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.IP)
```

### @Idempotent

```java
@PostMapping("/order/create")
@Idempotent(value = "order:create", timeout = 30, unit = TimeUnit.SECONDS)
public Result<Long> createOrder(@CurrentUser LoginUser user, @RequestBody CreateOrderReq req) {
    // 业务逻辑
}
```

前端使用流程：

```
1. 进入订单确认页 → GET /api/idempotent/token?biz=order:create&ttl=30
   响应：{ "code":200, "data":{ "token":"abc123", "header":"X-Idempotent-Token" } }
2. 提交订单时携带请求头 X-Idempotent-Token: abc123
3. 后端 AOP 消费 token，重复提交会被拒绝
```

### @CurrentUser

```java
@GetMapping("/me")
public Result<UserVO> me(@CurrentUser LoginUser user) {
    // user.id / user.openid / user.userType 直接可用
}
```

## Redis Key 命名规范（CommonConstants）

所有模块共用前缀 `studyroom:`：

| Key 前缀 | 用途 | 归属模块 |
|---------|------|---------|
| `studyroom:seat:lock:` | 座位时段分布式锁 | 模块5 |
| `studyroom:order:pay:` | 支付超时延迟队列 | 模块5 |
| `studyroom:order:checkin:` | 未签到超时 | 模块6 |
| `studyroom:pause:` | 中途暂停计时 | 模块6 |
| `studyroom:token:checkin:` | 签到二维码 | 模块6 |
| `studyroom:idempotent:` | 接口幂等 token | 系统级 |
| `studyroom:limiter:` | RRateLimiter 限流器 | 系统级 |
| `studyroom:captcha:` | 图形验证码 | 模块9 |
| `studyroom:email:code:` | 邮箱验证码 | 模块4 |
| `auth:token:` | 在线 token（jti） | 系统级 |
| `auth:blacklist:` | 踢下线黑名单 | 系统级 |

## 注意事项

1. **密钥管理**：生产环境必须通过 `STUDYROOM_JWT_SECRET` / `STUDYROOM_CRYPTO_AES_KEY` 等环境变量覆盖，application.yml 默认值仅供开发。
2. **AOP 顺序**：`@Idempotent` 优先于 `@RateLimiter`（IdempotentAspect 上加了 `@Order(0)`），防止被限流消耗请求后再消费 token。
3. **JWT 黑名单**：用户登出后 token 不会立即失效（JWT 自带无状态特性），需在 JwtAuthenticationFilter 中查 Redis 黑名单；TTL 与 token 剩余有效期一致即可自动清理。
4. **Long → String**：Jackson 配置会把 Long 序列化为字符串，前端 JS Number 不会精度丢失。但若后端服务间调用，需要按需还原。