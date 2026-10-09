package com.studyroom.common.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密工具类
 *
 * <h3>AES-256-CBC（PKCS7Padding）</h3>
 * <ul>
 *   <li>用于签到二维码内容加密、用户邮箱/手机号落库加密（FR-UC-004、FR-UC-030）</li>
 *   <li>密钥由 application.yml 中 studyroom.crypto.aes-key 注入（≥32 字节）</li>
 *   <li>IV 取密钥前 16 字节，避免额外配置；密文统一 Base64 输出</li>
 * </ul>
 *
 * <h3>HMAC-SHA256</h3>
 * <ul>
 *   <li>用于签到内容防篡改签名（FR-UC-030 二重校验）</li>
 *   <li>密钥与 AES 独立，由 studyroom.crypto.hmac-key 注入</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
public final class CryptoUtils {

    private static final String AES_ALG = "AES";
    private static final String AES_TRANSFORM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALG = "HmacSHA256";

    private CryptoUtils() {
    }

    // ==================== AES ====================

    public static String aesEncrypt(String plain, String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALG);
            IvParameterSpec iv = new IvParameterSpec(keyBytes, 0, 16);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            log.error("[CryptoUtils] AES 加密失败", e);
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    public static String aesDecrypt(String cipherText, String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALG);
            IvParameterSpec iv = new IvParameterSpec(keyBytes, 0, 16);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            byte[] dec = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[CryptoUtils] AES 解密失败", e);
            throw new IllegalStateException("AES 解密失败", e);
        }
    }

    // ==================== HMAC-SHA256 ====================

    public static String hmacSign(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] sign = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sign);
        } catch (Exception e) {
            log.error("[CryptoUtils] HMAC 签名失败", e);
            throw new IllegalStateException("HMAC 签名失败", e);
        }
    }

    /**
     * 常量时间比较签名（防 timing attack）。
     */
    public static boolean hmacVerify(String data, String key, String expectedSign) {
        String actual = hmacSign(data, key);
        if (actual == null || expectedSign == null) {
            return false;
        }
        byte[] a = actual.getBytes(StandardCharsets.UTF_8);
        byte[] b = expectedSign.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}