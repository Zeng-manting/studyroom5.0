package com.studyroom.web.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import com.studyroom.common.properties.JwtProperties;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Predicate;

/**
 * Knife4j / Swagger 配置
 *
 * <h3>要点</h3>
 * <ul>
 *   <li>基础路径：{@code /api/doc.html}</li>
 *   <li>默认开启 JWT 调试头（Authorization），便于在 Knife4j UI 中模拟登录</li>
 *   <li>分组：admin（管理端）、user（小程序用户端）、common（公共）</li>
 * </ul>
 *
 * <p>注：knife4j-openapi2-spring-boot-starter 4.3.0 传递依赖 springfox 2.10.5，
 * 启用注解为 {@code @EnableSwagger2WebMvc}，选择器返回 {@code java.util.function.Predicate}。</p>
 *
 * @author studyroom
 */
@Configuration
@EnableSwagger2WebMvc
@EnableKnife4j
@RequiredArgsConstructor
public class Knife4jConfig {

    private final JwtProperties jwtProperties;

    @Autowired
    private OpenApiExtensionResolver openApiExtensionResolver;

    // ==================== Springfox 兼容修复 ====================
    // Spring Boot 2.6+ 默认 PathPatternParser，Springfox 不兼容会 NPE，
    // 通过 BeanPostProcessor 过滤掉使用 PathPatternParser 的 HandlerMapping。
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                List<T> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    // ==================== Docket 分组 ====================

    @Bean
    public Docket commonApi() {
        return buildDocket("1.公共接口",
                RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class),
                PathSelectors.ant("/api/idempotent/**"));
    }

    @Bean
    public Docket userApi() {
        return buildDocket("2.用户端-小程序",
                RequestHandlerSelectors.basePackage("com.studyroom.web.controller"),
                PathSelectors.ant("/api/user/**"));
    }

    @Bean
    public Docket adminApi() {
        return buildDocket("3.管理端-Web",
                RequestHandlerSelectors.basePackage("com.studyroom.web.controller"),
                PathSelectors.ant("/api/admin/**"));
    }

    private Docket buildDocket(String groupName,
                               Predicate<RequestHandler> apis,
                               Predicate<String> paths) {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .apiInfo(apiInfo())
                .select()
                .apis(apis)
                .paths(paths)
                .build()
                .securityContexts(securityContext())
                .securitySchemes(securitySchemes())
                .extensions(openApiExtensionResolver.buildExtensions(groupName));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("无人自习室管理系统 API")
                .description("基于 Spring Boot 2.7 + uni-app + Vue 3 + Element Plus")
                .version("1.0.0")
                .build();
    }

    private List<SecurityContext> securityContext() {
        AuthorizationScope scope = new AuthorizationScope("global", "accessEverything");
        return Collections.singletonList(SecurityContext.builder()
                .securityReferences(Collections.singletonList(
                        new SecurityReference(jwtProperties.getHeader(),
                                new AuthorizationScope[]{scope})))
                .forPaths(PathSelectors.any())
                .build());
    }

    private List<ApiKey> securitySchemes() {
        List<ApiKey> list = new ArrayList<>();
        list.add(new ApiKey(jwtProperties.getHeader(),
                jwtProperties.getHeader(),
                "header"));
        return list;
    }
}
