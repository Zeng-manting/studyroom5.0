package com.studyroom.web.config;

import org.springframework.context.annotation.Configuration;

/**
 * 跨域占位配置（CORS 已统一在 {@link WebMvcConfig} 中注册）。
 * <p>保留此类仅为可观测性与后续扩展（如有管理端独立跨域策略可在 WebMvcConfig 之外再扩展）。</p>
 *
 * @author studyroom
 */
@Configuration
public class CorsConfig {

}