package com.studyroom.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项
 * <p>由 application.yml 中 studyroom.jwt 前缀注入；密钥从外部环境变量注入，
 * 保证生产环境不硬编码。</p>
 *
 * <pre>
 * studyroom:
 *   jwt:
 *     secret: ${JWT_SECRET:please-change-me-32bytes-minimum-123456}
 *     issuer: studyroom
 *     user-ttl-seconds: 604800      # 用户端 7 天  FR-UC-001
 *     admin-ttl-seconds: 7200       # 管理端 2 小时 FR-AD-001
 *     header: Authorization
 *     prefix: "Bearer "
 *     redis-prefix: "auth:token:"
 *     blacklist-prefix: "auth:blacklist:"
 * </pre>
 *
 * @author studyroom
 */
@Data
@Component
@ConfigurationProperties(prefix = "studyroom.jwt")
public class JwtProperties {

    /** 签名密钥（≥32 字节） */
    private String secret;

    /** 签发者 */
    private String issuer = "studyroom";

    /** 用户端 token 有效期（秒）。FR-UC-001 默认 7 天 */
    private Long userTtlSeconds = 7 * 24 * 3600L;

    /** 管理端 token 有效期（秒）。FR-AD-001 默认 2 小时 */
    private Long adminTtlSeconds = 2 * 3600L;

    /** 请求头名称 */
    private String header = "Authorization";

    /** token 前缀 */
    private String prefix = "Bearer ";

    /** 在线 token 在 Redis 中的 key 前缀：auth:token:{userType}:{jti} */
    private String redisPrefix = "auth:token:";

    /** 黑名单前缀：auth:blacklist:{jti} 值为过期时间戳（ms） */
    private String blacklistPrefix = "auth:blacklist:";
}