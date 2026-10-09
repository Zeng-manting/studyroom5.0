package com.studyroom.service.auth;

import com.studyroom.entity.dto.user.WxLoginDTO;
import com.studyroom.entity.vo.user.LoginVO;

/**
 * 登录服务接口
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-001 微信静默登录：code → code2Session → openid → JWT</li>
 *   <li>FR-UC-002 首次自动注册：openid 不存在时自动建档（默认昵称"自习用户"）</li>
 *   <li>FR-UC-005 Token 管理：JWT 有效期 7 天</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AuthService {

    /**
     * 微信静默登录（含首次自动注册）
     *
     * @param dto 登录凭证（uni.login code）
     * @return token + 用户信息 + 是否新用户
     */
    LoginVO wxLogin(WxLoginDTO dto);
}
