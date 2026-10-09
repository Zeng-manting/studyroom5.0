package com.studyroom.web.config;

import com.studyroom.web.security.JwtAccessDeniedHandler;
import com.studyroom.web.security.JwtAuthenticationEntryPoint;
import com.studyroom.web.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（JWT 无状态模式）
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>注册 JWT 过滤器链、禁用 CSRF / 表单登录 / Session</li>
 *   <li>放行 Knife4j、Swagger、健康检查、登录/注册/签到等公开接口</li>
 *   <li>其余接口要求已登录；具体权限由 {@code @PreAuthorize("hasRole('ADMIN')")} 控制</li>
 * </ul>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-001 用户端 JWT 7 天</li>
 *   <li>FR-AD-001 管理端 JWT 2 小时</li>
 *   <li>FR-SY-002 黑名单踢下线</li>
 * </ul>
 *
 * @author studyroom
 */
@Configuration
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    // 注意：context-path 为 /api，Security 匹配路径不含该前缀，此处必须写应用内路径
    private static final String[] PUBLIC_URLS = new String[]{
            // 健康检查 / 文档
            "/ping",
            "/error",
            "/doc.html",
            "/doc.html/**",
            "/webjars/**",
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v2/api-docs/**",
            "/favicon.ico",
            // 静态资源
            "/static/**",
            // 本地存储的图片资源（<image> 标签加载无法携带 JWT，FR-CC-003）
            "/file/**",
            // 公开业务接口（无需登录）
            "/admin/login",                  // FR-AD-001 管理员登录
            "/admin/captcha",                // 验证码
            "/user/wx-login",                // FR-UC-001 微信静默登录
            "/user/wx-phone",                // FR-UC-002 手机号授权
            "/notice/public/**",             // 公告详情
            "/store/public/**",              // 门店列表/详情
            "/review/public/**",             // 评价展示
            // 幂等 token 签发（限流保护，按 IP 限流）
            "/idempotent/token",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（前后端分离 + JWT 无状态）
                .csrf().disable()
                // 禁用表单登录
                .formLogin().disable()
                // 禁用 Basic Auth
                .httpBasic().disable()
                // 无状态 Session
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(PUBLIC_URLS).permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .and()
                // 把 JWT 过滤器放在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}