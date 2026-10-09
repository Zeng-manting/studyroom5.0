package com.studyroom.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置项
 *
 * <pre>
 * studyroom:
 *   wechat:
 *     appid: ${WECHAT_APPID:wx-your-appid}
 *     secret: ${WECHAT_SECRET:your-app-secret}
 * </pre>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-001 静默登录 code2Session</li>
 *   <li>FR-UC-044 订阅消息推送（后续模块使用）</li>
 * </ul>
 *
 * @author studyroom
 */
@Data
@Component
@ConfigurationProperties(prefix = "studyroom.wechat")
public class WechatProperties {

    /** 小程序 appid */
    private String appid;

    /** 小程序 secret */
    private String secret;
}
