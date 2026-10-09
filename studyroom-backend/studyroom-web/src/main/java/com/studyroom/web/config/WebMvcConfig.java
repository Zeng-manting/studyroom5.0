package com.studyroom.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.studyroom.common.utils.JacksonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 配置
 * <ul>
 *   <li>注册 {@link CurrentUserArgumentResolver}（@CurrentUser 自动注入 LoginUser）</li>
 *   <li>注册自定义 Jackson 时间模块（兼容 LocalDateTime）+ Long→String 防 JS 精度丢失</li>
 *   <li>统一 CORS 跨域（开发环境）；生产由 Nginx 代理</li>
 * </ul>
 *
 * @author studyroom
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Value("${studyroom.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    /** 本地文件存储根目录（FR-CC-003 当前替代方案） */
    @Value("${studyroom.storage.dir:./storage}")
    private String storageDir;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    /**
     * 静态资源映射：/api/file/{scene}/{date}/{file} → 本地存储目录
     * <p>图片在小程序 <image> 标签加载，无法携带 JWT，路径在 SecurityConfig 中放行。</p>
     */
    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        String location = java.nio.file.Paths.get(storageDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/file/**").addResourceLocations(location);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jackson2 = (MappingJackson2HttpMessageConverter)
                converters.stream()
                        .filter(c -> c instanceof MappingJackson2HttpMessageConverter)
                        .findFirst()
                        .orElseGet(() -> {
                            MappingJackson2HttpMessageConverter c = new MappingJackson2HttpMessageConverter();
                            converters.add(c);
                            return c;
                        });
        ObjectMapper mapper = jackson2.getObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JacksonUtils.registerModules(mapper);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}