package com.studyroom.service.auth;

import com.studyroom.common.login.LoginUser;
import com.studyroom.entity.admin.Admin;
import com.studyroom.entity.dto.admin.AdminLoginDTO;
import com.studyroom.entity.vo.admin.AdminInfoVO;
import com.studyroom.entity.vo.admin.AdminLoginVO;
import com.studyroom.entity.vo.admin.CaptchaVO;

/**
 * 管理端登录服务 —— FR-AD-001
 *
 * <h3>接口清单</h3>
 * <ul>
 *   <li>{@link #getCaptcha()}：生成图形验证码（Hutool，Redis 5 分钟一次性）</li>
 *   <li>{@link #login(AdminLoginDTO)}：账号 + 密码（BCrypt）+ 验证码，签发 JWT（2 小时）</li>
 *   <li>{@link #logout(LoginUser)}：jti 加入 Redis 黑名单（FR-SY-002）</li>
 *   <li>{@link #getInfo(Long)}：当前管理员信息（顶栏展示）</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminAuthService {

    /** 生成图形验证码（FR-AD-001） */
    CaptchaVO getCaptcha();

    /** 管理员登录（FR-AD-001） */
    AdminLoginVO login(AdminLoginDTO dto);

    /** 退出登录：当前 token 拉黑（FR-AD-001 / FR-SY-002） */
    void logout(LoginUser loginUser);

    /** 管理员信息（FR-AD-001 登录后顶栏 / 个人信息） */
    AdminInfoVO getInfo(Long adminId);

    /** Admin 实体转 VO（内部复用） */
    AdminInfoVO toAdminInfoVO(Admin admin);
}
