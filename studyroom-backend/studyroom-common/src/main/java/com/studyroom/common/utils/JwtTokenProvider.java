package com.studyroom.common.utils;

import com.studyroom.common.enums.UserTypeEnum;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.properties.JwtProperties;
import com.studyroom.common.result.ResultCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类（jjwt 0.11.5）
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>HS256 签名，密钥由 application.yml 注入，≥32 字节</li>
 *   <li>claims 中携带 uid / openid / userType / roleKey，Controller 侧零查询即可鉴权</li>
 *   <li>jti 作为 token 唯一标识，配合 Redis 黑名单实现"踢下线"</li>
 *   <li>TTL 区分用户端（7 天 FR-UC-001）与管理端（2 小时 FR-AD-001）</li>
 *   <li>解析失败统一抛业务异常，由 GlobalExceptionHandler 转译</li>
 * </ul>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-001 微信静默登录后由本类签发用户 token</li>
 *   <li>FR-AD-001 管理员登录后由本类签发管理员 token</li>
 *   <li>FR-SY-002 续期 / 失效场景依赖 jti + Redis 黑名单</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "studyroom.jwt.secret 长度不足 32 字节，当前=" + bytes.length
                            + "；请在生产环境通过 JWT_SECRET 环境变量注入足够长度密钥。");
        }
        this.secretKey = Keys.hmacShaKeyFor(bytes);
        log.info("[JwtTokenProvider] JWT 工具初始化完成，issuer={}, userTtl={}s, adminTtl={}s",
                props.getIssuer(), props.getUserTtlSeconds(), props.getAdminTtlSeconds());
    }

    /**
     * 生成 token
     *
     * @param loginUser 登录用户快照（id / openid / userType / roleKey 必填）
     * @return 形如 "header.payload.signature" 的 JWT 字符串
     */
    public String generate(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserType() == null) {
            throw new IllegalArgumentException("loginUser / userType 不能为空");
        }
        long ttl = UserTypeEnum.ADMIN.equals(loginUser.getUserType())
                ? props.getAdminTtlSeconds()
                : props.getUserTtlSeconds();
        long now = System.currentTimeMillis();
        String jti = UUID.randomUUID().toString().replace("-", "");
        loginUser.setJti(jti);

        Map<String, Object> claims = new HashMap<>(8);
        claims.put("uid", loginUser.getId());
        claims.put("openid", loginUser.getOpenid());
        claims.put("userType", loginUser.getUserType().getCode());
        claims.put("roleKey", loginUser.getRoleKey());

        return Jwts.builder()
                .setId(jti)
                .setIssuer(props.getIssuer())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttl * 1000L))
                .setSubject(String.valueOf(loginUser.getId()))
                .addClaims(claims)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 token 解析登录用户。解析失败抛业务异常，由全局异常处理返回 401。
     */
    public LoginUser parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .requireIssuer(props.getIssuer())
                    .build()
                    .parseClaimsJws(token);

            Claims c = jws.getBody();
            Long uid = c.get("uid", Long.class);
            Integer userTypeCode = c.get("userType", Integer.class);
            UserTypeEnum userType = UserTypeEnum.of(userTypeCode);
            if (userType == null) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的 token 类型");
            }
            return LoginUser.builder()
                    .id(uid)
                    .openid(c.get("openid", String.class))
                    .userType(userType)
                    .roleKey(c.get("roleKey", String.class))
                    .jti(c.getId())
                    .build();
        } catch (ExpiredJwtException e) {
            log.warn("[JwtTokenProvider] token 已过期: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.warn("[JwtTokenProvider] token 非法: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效的登录凭证");
        } catch (MissingClaimException e) {
            log.warn("[JwtTokenProvider] token 缺少必要声明: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录凭证格式错误");
        }
    }

    /**
     * 从 "Bearer xxx" 中提取裸 token；无前缀时原样返回。
     */
    public String resolve(String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) {
            return null;
        }
        String prefix = props.getPrefix();
        if (prefix != null && !prefix.isEmpty() && headerValue.startsWith(prefix)) {
            return headerValue.substring(prefix.length()).trim();
        }
        return headerValue.trim();
    }

    /** 计算 token 剩余有效期（毫秒），用于 Redis 黑名单 TTL */
    public long getRemainingMillis(String token) {
        try {
            Claims c = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return c.getExpiration().getTime() - System.currentTimeMillis();
        } catch (ExpiredJwtException e) {
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}