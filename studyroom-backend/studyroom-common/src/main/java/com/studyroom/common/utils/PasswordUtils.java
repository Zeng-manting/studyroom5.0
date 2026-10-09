package com.studyroom.common.utils;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具（BCrypt）
 * <p>用于管理端 BCrypt 校验（FR-AD-001）；普通用户侧无密码概念，
 * 微信静默登录 + JWT 鉴权即可。</p>
 *
 * @author studyroom
 */
@Slf4j
public final class PasswordUtils {

    /** strength = 10，与 Spring Security 默认一致 */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    private PasswordUtils() {
    }

    public static String encode(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码不能为空");
        }
        return ENCODER.encode(raw);
    }

    public static boolean matches(String raw, String hashed) {
        if (raw == null || hashed == null) {
            return false;
        }
        return ENCODER.matches(raw, hashed);
    }
}