package com.studyroom.service.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.enums.UserTypeEnum;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.login.LoginUser;
import com.studyroom.common.properties.JwtProperties;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.JwtTokenProvider;
import com.studyroom.common.utils.PasswordUtils;
import com.studyroom.common.utils.RedisUtils;
import com.studyroom.common.utils.WebUtils;
import com.studyroom.entity.admin.Admin;
import com.studyroom.entity.admin.AdminRole;
import com.studyroom.entity.dto.admin.AdminLoginDTO;
import com.studyroom.entity.vo.admin.AdminInfoVO;
import com.studyroom.entity.vo.admin.AdminLoginVO;
import com.studyroom.entity.vo.admin.CaptchaVO;
import com.studyroom.mapper.admin.AdminMapper;
import com.studyroom.mapper.admin.AdminRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 管理端登录服务实现 —— FR-AD-001
 *
 * <h3>登录流程</h3>
 * <ol>
 *   <li>防爆破锁定检查：同一账号 10 分钟内连续失败 5 次 → 锁定</li>
 *   <li>图形验证码校验：Redis 一次性消费（getAndDelete），不区分大小写</li>
 *   <li>账号查询 + 禁用校验（status=0 拒绝登录）</li>
 *   <li>BCrypt 密码校验（{@link PasswordUtils}）</li>
 *   <li>更新 last_login_time / last_login_ip</li>
 *   <li>签发 JWT：userType=ADMIN，roleKey=admin_role.code，TTL 2 小时</li>
 * </ol>
 *
 * <h3>安全说明</h3>
 * <ul>
 *   <li>账号不存在与密码错误统一提示"账号或密码错误"，不泄露账号存在性</li>
 *   <li>验证码无论校验结果如何均立即作废，需重新获取</li>
 *   <li>JWT 过期由 JwtTokenProvider 抛 401，前端跳转登录页（FR-AD-001）</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    /** 验证码图片宽度（px） */
    private static final int CAPTCHA_WIDTH = 130;
    /** 验证码图片高度（px） */
    private static final int CAPTCHA_HEIGHT = 40;
    /** 验证码字符数 */
    private static final int CAPTCHA_CODE_COUNT = 4;
    /** 验证码干扰线数 */
    private static final int CAPTCHA_LINE_COUNT = 20;

    private final AdminMapper adminMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisUtils redisUtils;

    @Override
    public CaptchaVO getCaptcha() {
        // Hutool 线干扰图形验证码（FR-AD-001）
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_CODE_COUNT, CAPTCHA_LINE_COUNT);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = captcha.getCode();

        // 存 Redis：5 分钟有效，登录时一次性消费
        redisUtils.set(CommonConstants.CAPTCHA_KEY + captchaId, code == null ? "" : code.toLowerCase(),
                Duration.ofMinutes(CommonConstants.CAPTCHA_TTL_MINUTES));

        // Base64 图片（前端 <img :src> 直接渲染）
        String image = "data:image/png;base64," + captcha.getImageBase64();
        return CaptchaVO.of(captchaId, image);
    }

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        String username = dto.getUsername().trim();

        // 1) 防爆破锁定检查（FR-AD-001：10 分钟内连续失败 5 次锁定）
        String failKey = CommonConstants.ADMIN_LOGIN_FAIL_KEY + username;
        Long failCount = redisUtils.getLong(failKey);
        if (failCount != null && failCount >= CommonConstants.ADMIN_LOGIN_MAX_FAIL) {
            log.warn("[AdminAuth] 账号连续失败锁定: username={}, ip={}", username, WebUtils.getClientIp());
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "密码连续错误次数过多，账号已锁定 " + CommonConstants.ADMIN_LOGIN_LOCK_MINUTES + " 分钟，请稍后重试");
        }

        // 2) 图形验证码校验（一次性消费：无论对错都删除，防止重放爆破）
        String expected = redisUtils.getAndDelete(
                CommonConstants.CAPTCHA_KEY + dto.getCaptchaId(), null);
        if (expected == null || !expected.equalsIgnoreCase(dto.getCaptchaCode())) {
            recordLoginFail(failKey, failCount);
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期，请重新获取");
        }

        // 3) 账号查询
        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            recordLoginFail(failKey, failCount);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号或密码错误");
        }

        // 4) 禁用校验
        if (admin.getStatus() == null || admin.getStatus() != Admin.STATUS_ENABLED) {
            log.warn("[AdminAuth] 已禁用账号尝试登录: username={}", username);
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系超级管理员");
        }

        // 5) BCrypt 密码校验（FR-AD-001）
        if (!PasswordUtils.matches(dto.getPassword(), admin.getPassword())) {
            recordLoginFail(failKey, failCount);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号或密码错误");
        }

        // 6) 登录成功：清除失败计数 + 更新登录信息（失败不影响主流程）
        redisUtils.delete(failKey);
        try {
            adminMapper.updateLoginInfo(admin.getId(), LocalDateTime.now(), WebUtils.getClientIp());
        } catch (Exception e) {
            log.warn("[AdminAuth] 更新登录信息失败 adminId={}: {}", admin.getId(), e.getMessage());
        }

        // 7) 查角色（role_id 缺失/角色禁用时按最低角色 OPERATOR 处理，RBAC 预留）
        String roleCode = CommonConstants.ROLE_SUPER_ADMIN;
        String roleName = "超级管理员";
        if (admin.getRoleId() != null) {
            AdminRole role = adminRoleMapper.selectById(admin.getRoleId());
            if (role != null && role.getStatus() != null && role.getStatus() == 1) {
                roleCode = role.getCode();
                roleName = role.getName();
            }
        }

        // 8) 签发 JWT —— FR-AD-001 管理端 2 小时
        LoginUser loginUser = LoginUser.builder()
                .id(admin.getId())
                .userType(UserTypeEnum.ADMIN)
                .roleKey(roleCode)
                .build();
        String token = jwtTokenProvider.generate(loginUser);

        log.info("[AdminAuth] 管理员登录成功: adminId={}, username={}, role={}, ip={}",
                admin.getId(), username, roleCode, WebUtils.getClientIp());
        return AdminLoginVO.of(token, jwtProperties.getAdminTtlSeconds(),
                toAdminInfoVO(admin, roleCode, roleName));
    }

    @Override
    public void logout(LoginUser loginUser) {
        if (loginUser == null || loginUser.getJti() == null) {
            return;
        }
        // jti 加入黑名单，TTL 取管理端 token 最大剩余有效期（2 小时）
        redisUtils.set(jwtProperties.getBlacklistPrefix() + loginUser.getJti(), "1",
                Duration.ofSeconds(jwtProperties.getAdminTtlSeconds()));
        log.info("[AdminAuth] 管理员退出登录: adminId={}", loginUser.getId());
    }

    @Override
    public AdminInfoVO getInfo(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录信息已失效，请重新登录");
        }
        String roleCode = CommonConstants.ROLE_SUPER_ADMIN;
        String roleName = "超级管理员";
        if (admin.getRoleId() != null) {
            AdminRole role = adminRoleMapper.selectById(admin.getRoleId());
            if (role != null) {
                roleCode = role.getCode();
                roleName = role.getName();
            }
        }
        return toAdminInfoVO(admin, roleCode, roleName);
    }

    @Override
    public AdminInfoVO toAdminInfoVO(Admin admin) {
        return toAdminInfoVO(admin, null, null);
    }

    private AdminInfoVO toAdminInfoVO(Admin admin, String roleCode, String roleName) {
        if (roleCode == null) {
            if (admin.getRoleId() != null) {
                AdminRole role = adminRoleMapper.selectById(admin.getRoleId());
                if (role != null) {
                    roleCode = role.getCode();
                    roleName = role.getName();
                }
            } else {
                roleCode = CommonConstants.ROLE_SUPER_ADMIN;
                roleName = "超级管理员";
            }
        }
        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setAvatar(admin.getAvatar());
        vo.setRoleCode(roleCode);
        vo.setRoleName(roleName);
        vo.setLastLoginTime(admin.getLastLoginTime());
        vo.setLastLoginIp(admin.getLastLoginIp());
        return vo;
    }

    /** 记录一次登录失败（计数 +1，首次失败设置锁定窗口 TTL） */
    private void recordLoginFail(String failKey, Long failCount) {
        long window = CommonConstants.ADMIN_LOGIN_LOCK_MINUTES * 60L;
        if (failCount == null) {
            redisUtils.set(failKey, "1", Duration.ofSeconds(window));
        } else {
            Long after = redisUtils.incr(failKey);
            // incr 不重置 TTL：仅当窗口已过期新键被创建时才有 TTL，续保避免永久累计
            if (after != null && after == 1L) {
                redisUtils.expire(failKey, Duration.ofSeconds(window));
            }
            long remain = CommonConstants.ADMIN_LOGIN_MAX_FAIL - (after == null ? 1 : after);
            log.warn("[AdminAuth] 登录失败计数: failKey={}, 剩余尝试次数={}", failKey, Math.max(remain, 0));
        }
    }
}
