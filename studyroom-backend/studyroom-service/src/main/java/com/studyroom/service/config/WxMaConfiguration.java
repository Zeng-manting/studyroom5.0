package com.studyroom.service.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.studyroom.common.properties.WechatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WxJava 微信小程序客户端配置 —— FR-UC-001 / FR-CC-001
 *
 * <p>技术选型 1.3：WxJava 4.5.0（weixin-java-miniapp）。
 * Spring Boot 直接调用微信接口替代云函数（FR-CC-001 当前替代方案）。</p>
 *
 * <p>多线程安全：WxMaService 内部维护 accessToken 自动刷新，
 * 单实例即可支撑单店场景并发（峰值 QPS ≤ 50）。</p>
 *
 * @author studyroom
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WxMaConfiguration {

    private final WechatProperties wechatProperties;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(wechatProperties.getAppid());
        config.setSecret(wechatProperties.getSecret());
        // access_token 存于内存（单实例部署足够；集群部署可切换 WxMaRedisBetterConfigImpl，需要 Jedis）

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        log.info("[WxMaConfiguration] 微信小程序客户端初始化完成 appid={}", wechatProperties.getAppid());
        return service;
    }
}
