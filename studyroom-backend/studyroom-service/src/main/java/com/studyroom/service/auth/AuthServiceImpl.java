package com.studyroom.service.auth;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.enums.UserTypeEnum;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.properties.JwtProperties;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.JwtTokenProvider;
import com.studyroom.common.utils.RedisUtils;
import com.studyroom.common.utils.WebUtils;
import com.studyroom.entity.dto.user.H5LoginDTO;
import com.studyroom.entity.dto.user.WxLoginDTO;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.user.LoginVO;
import com.studyroom.entity.vo.user.UserVO;
import com.studyroom.mapper.user.UserMapper;
import com.studyroom.service.user.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 登录服务实现
 *
 * <h3>流程（FR-UC-001 / FR-UC-002）</h3>
 * <ol>
 *   <li>WxJava code2Session：code → openid / unionid / sessionKey</li>
 *   <li>按 openid 查询用户；不存在则自动注册（默认昵称"自习用户"，信用分 100）</li>
 *   <li>账号禁用校验（FR-AD-014 禁用后无法登录）</li>
 *   <li>更新最近登录时间 / IP</li>
 *   <li>签发 JWT（用户端 7 天 —— FR-UC-005），sessionKey 缓存 Redis 供后续解密使用</li>
 * </ol>
 *
 * <h3>并发说明</h3>
 * <p>同一 openid 首次并发登录可能同时触发注册，依赖 uk_openid 唯一键兜底：
 * 捕获 DuplicateKeyException 后重查一次。</p>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 微信 sessionKey 缓存时长（微信官方 session 有效期约 3 天） */
    private static final Duration SESSION_KEY_TTL = Duration.ofDays(3);

    /** 默认昵称 —— FR-UC-002 */
    private static final String DEFAULT_NICKNAME = "自习用户";

    /** H5 端 openid 前缀（账号派生标识，避免与微信 openid 冲突） */
    private static final String H5_OPENID_PREFIX = "h5_";

    private final WxMaService wxMaService;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisUtils redisUtils;
    private final UserInfoService userInfoService;

    @Override
    public LoginVO wxLogin(WxLoginDTO dto) {
        // 1) code 换取 openid —— FR-UC-001
        WxMaJscode2SessionResult session;
        try {
            // WxJava 4.5.0：jsCode 换取 session（openid/sessionKey/unionid）
            session = wxMaService.getUserService().getSessionInfo(dto.getCode());
        } catch (Exception e) {
            // code 无效/已使用（40029）、频率限制等
            log.warn("[AuthServiceImpl] code2Session 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "微信登录失败，请重新进入小程序");
        }
        String openid = session.getOpenid();
        if (openid == null || openid.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "微信登录失败：未获取到 openid");
        }

        // 2) 查询用户，不存在则自动注册 —— FR-UC-002
        boolean newUser = false;
        User user = userMapper.selectByOpenid(openid);
        if (user == null) {
            user = registerByOpenid(openid, session.getUnionid());
            newUser = true;
            log.info("[AuthServiceImpl] 新用户自动注册 uid={} openid={}", user.getId(), mask(openid));
        }

        // 3) 账号禁用校验
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("[AuthServiceImpl] 禁用账号尝试登录 uid={}", user.getId());
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，如有疑问请联系门店");
        }

        // 4) 更新登录信息（失败不影响主流程）
        try {
            User patch = new User();
            patch.setId(user.getId());
            patch.setLastLoginTime(LocalDateTime.now());
            patch.setLastLoginIp(WebUtils.getClientIp());
            userMapper.updateById(patch);
        } catch (Exception e) {
            log.warn("[AuthServiceImpl] 更新登录信息失败 uid={}: {}", user.getId(), e.getMessage());
        }

        // 5) 签发 JWT —— FR-UC-005（用户端 7 天）
        LoginUser loginUser = LoginUser.builder()
                .id(user.getId())
                .openid(openid)
                .userType(UserTypeEnum.USER)
                .roleKey(CommonConstants.ROLE_USER)
                .build();
        String token = jwtTokenProvider.generate(loginUser);

        // sessionKey 缓存 Redis（供后续手机号解密等场景；对用户无感）
        redisUtils.set(CommonConstants.WX_SESSION_KEY + user.getId(),
                session.getSessionKey() == null ? "" : session.getSessionKey(), SESSION_KEY_TTL);

        UserVO userVO = userInfoService.toUserVO(user);
        log.info("[AuthServiceImpl] 登录成功 uid={} newUser={}", user.getId(), newUser);
        return LoginVO.of(token, jwtProperties.getUserTtlSeconds(), newUser, userVO);
    }

    @Override
    public LoginVO h5Login(H5LoginDTO dto) {
        String account = dto.getAccount().trim();
        // H5 无微信 openid，用账号派生唯一标识（前缀与真实 openid 隔离）
        String openid = H5_OPENID_PREFIX + account;

        // 1) 查询用户，不存在则自动注册
        boolean newUser = false;
        User user = userMapper.selectByOpenid(openid);
        if (user == null) {
            user = registerByOpenid(openid, null);
            // 新用户昵称默认取账号，便于在列表/评价中辨识
            if (DEFAULT_NICKNAME.equals(user.getNickname())) {
                User patch = new User();
                patch.setId(user.getId());
                patch.setNickname(account);
                userMapper.updateById(patch);
                user.setNickname(account);
            }
            newUser = true;
            log.info("[AuthServiceImpl] H5 新用户自动注册 uid={} account={}", user.getId(), account);
        }

        // 2) 账号禁用校验
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("[AuthServiceImpl] 禁用账号尝试登录 uid={}", user.getId());
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，如有疑问请联系门店");
        }

        // 3) 更新登录信息（失败不影响主流程）
        try {
            User patch = new User();
            patch.setId(user.getId());
            patch.setLastLoginTime(LocalDateTime.now());
            patch.setLastLoginIp(WebUtils.getClientIp());
            userMapper.updateById(patch);
        } catch (Exception e) {
            log.warn("[AuthServiceImpl] 更新登录信息失败 uid={}: {}", user.getId(), e.getMessage());
        }

        // 4) 签发 JWT（用户端 7 天）
        LoginUser loginUser = LoginUser.builder()
                .id(user.getId())
                .openid(openid)
                .userType(UserTypeEnum.USER)
                .roleKey(CommonConstants.ROLE_USER)
                .build();
        String token = jwtTokenProvider.generate(loginUser);

        UserVO userVO = userInfoService.toUserVO(user);
        log.info("[AuthServiceImpl] H5 登录成功 uid={} newUser={}", user.getId(), newUser);
        return LoginVO.of(token, jwtProperties.getUserTtlSeconds(), newUser, userVO);
    }

    /**
     * 首次自动注册 —— FR-UC-002
     * <p>默认昵称"自习用户"、信用分 100、余额 0；并发插入冲突时依赖唯一键重查。</p>
     */
    private User registerByOpenid(String openid, String unionid) {
        User user = new User();
        user.setOpenid(openid);
        user.setUnionid(unionid);
        user.setNickname(DEFAULT_NICKNAME);
        user.setGender(0);
        user.setBalance(java.math.BigDecimal.ZERO);
        user.setFrozenBalance(java.math.BigDecimal.ZERO);
        user.setCreditScore(100);
        user.setCreditStatus(0);
        user.setNoShowCount30d(0);
        user.setStatus(1);
        user.setPrivacyAgreed(0);
        try {
            userMapper.insert(user);
            return user;
        } catch (DuplicateKeyException e) {
            // 并发注册：唯一键 uk_openid 兜底，重查一次
            User exists = userMapper.selectByOpenid(openid);
            if (exists != null) {
                return exists;
            }
            throw new BusinessException(ResultCode.ERROR, "注册失败，请重试");
        }
    }

    /** openid 脱敏输出（日志安全） */
    private String mask(String openid) {
        if (openid == null || openid.length() < 8) {
            return "***";
        }
        return openid.substring(0, 4) + "****" + openid.substring(openid.length() - 4);
    }
}
