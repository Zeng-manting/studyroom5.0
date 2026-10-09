package com.studyroom.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录身份类型
 * <p>用于 JWT claims 中区分签发对象，避免小程序用户和管理员 token 混淆。</p>
 *
 * @author studyroom
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {

    /** 微信小程序普通用户 —— FR-UC-001 */
    USER(1, "USER"),

    /** 管理端后台管理员 —— FR-AD-001 */
    ADMIN(2, "ADMIN");

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String desc;

    public static UserTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}