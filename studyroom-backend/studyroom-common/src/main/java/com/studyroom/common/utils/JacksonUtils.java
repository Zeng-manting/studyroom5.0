package com.studyroom.common.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Jackson 工具
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>统一 ObjectMapper 配置：Java8 时间、Long 序列化为字符串防精度丢失（前端 JS）</li>
 *   <li>WebMvcConfig 与 Knife4j 配置复用</li>
 * </ul>
 *
 * @author studyroom
 */
public final class JacksonUtils {

    private JacksonUtils() {
    }

    /**
     * 给定 ObjectMapper 注册常用模块（含 Long → String 防精度丢失）
     */
    public static void registerModules(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new ParameterNamesModule());
        // Long 序列化为字符串，避免前端 JS Number 精度丢失
        com.fasterxml.jackson.databind.module.SimpleModule simple = new com.fasterxml.jackson.databind.module.SimpleModule();
        simple.addSerializer(Long.class, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        simple.addSerializer(Long.TYPE, com.fasterxml.jackson.databind.ser.std.ToStringSerializer.instance);
        mapper.registerModule(simple);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 构造一个项目默认的 ObjectMapper（用于 Knife4j / 单独工具）
     */
    public static ObjectMapper defaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        registerModules(mapper);
        return mapper;
    }
}