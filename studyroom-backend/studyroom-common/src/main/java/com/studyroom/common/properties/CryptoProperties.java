package com.studyroom.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 加密配置项（AES + HMAC）
 *
 * <h3>生产环境</h3>
 * <p>所有密钥必须通过环境变量注入：
 * {@code STUDYROOM_CRYPTO_AES_KEY} / {@code STUDYROOM_CRYPTO_HMAC_KEY}</p>
 *
 * <pre>
 * studyroom:
 *   crypto:
 *     aes-key: ${STUDYROOM_CRYPTO_AES_KEY:studyroom-dev-aes-32bytes-key-1234567890}
 *     hmac-key: ${STUDYROOM_CRYPTO_HMAC_KEY:studyroom-dev-hmac-key-please-override-in-production}
 * </pre>
 *
 * @author studyroom
 */
@Data
@Component
@ConfigurationProperties(prefix = "studyroom.crypto")
public class CryptoProperties {

    /** AES-256-CBC 密钥（≥32 字节；IV 取前 16 字节） */
    private String aesKey;

    /** HMAC-SHA256 密钥 */
    private String hmacKey;
}