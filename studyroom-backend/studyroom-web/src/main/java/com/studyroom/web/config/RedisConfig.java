package com.studyroom.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.utils.JacksonUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 配置
 * <p>项目里绝大多数场景使用 {@link com.studyroom.common.utils.RedisUtils}（基于 StringRedisTemplate）；
 * 这里额外提供一个使用 JSON 序列化的 RedisTemplate，供需要存储对象的场景使用（如缓存用户对象等）。</p>
 *
 * @author studyroom
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        ObjectMapper mapper = JacksonUtils.defaultMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}