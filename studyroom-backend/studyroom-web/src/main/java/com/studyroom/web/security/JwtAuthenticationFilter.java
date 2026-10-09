package com.studyroom.web.security;

import com.studyroom.common.enums.UserTypeEnum;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.properties.JwtProperties;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.JwtTokenProvider;
import com.studyroom.common.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器（每个请求只执行一次）
 *
 * <h3>职责</h3>
 * <ol>
 *   <li>从 Header 提取 token 并通过 {@link JwtTokenProvider} 解析</li>
 *   <li>校验 Redis 黑名单（FR-SY-002 踢下线 / 主动失效）</li>
 *   <li>解析成功：写入 {@link AuthContextHolder} + Spring Security 上下文</li>
 *   <li>解析失败：放行（后续 SecurityConfig 的 AuthenticationEntryPoint 会处理 401）</li>
 * </ol>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-001 用户端 JWT 校验</li>
 *   <li>FR-AD-001 管理端 JWT 校验</li>
 *   <li>FR-SY-002 黑名单机制</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisUtils redisUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String header = request.getHeader(jwtProperties.getHeader());
            String token = jwtTokenProvider.resolve(header);
            if (token == null || token.isEmpty()) {
                chain.doFilter(request, response);
                return;
            }
            // 1) 解析（失败时抛 BusinessException，由 GlobalExceptionHandler 转 401）
            LoginUser loginUser;
            try {
                loginUser = jwtTokenProvider.parse(token);
            } catch (BusinessException e) {
                log.debug("[JwtAuthenticationFilter] token 解析失败: {}", e.getMessage());
                chain.doFilter(request, response);
                return;
            }

            // 2) 黑名单校验
            String blacklistKey = jwtProperties.getBlacklistPrefix() + loginUser.getJti();
            if (Boolean.TRUE.equals(redisUtils.hasKey(blacklistKey))) {
                log.debug("[JwtAuthenticationFilter] token 已被列入黑名单 jti={}", loginUser.getJti());
                chain.doFilter(request, response);
                return;
            }

            // 3) 写入 Spring Security 上下文（供 @PreAuthorize 鉴权）
            // 管理端接口统一要求 hasRole('ADMIN')：管理员类型 token 始终授予 ROLE_ADMIN，
            // 同时附加具体角色权限（如 ROLE_SUPER_ADMIN）供未来细粒度 RBAC 使用
            java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            if (UserTypeEnum.ADMIN.equals(loginUser.getUserType())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            if (loginUser.getRoleKey() != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + loginUser.getRoleKey().toUpperCase()));
            } else if (!UserTypeEnum.ADMIN.equals(loginUser.getUserType())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 4) 写入 ThreadLocal 业务上下文
            AuthContextHolder.set(loginUser);
            chain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
        }
    }
}