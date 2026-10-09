package com.studyroom.service.user;

import com.studyroom.entity.dto.user.BindEmailDTO;
import com.studyroom.entity.dto.user.SendEmailCodeDTO;
import com.studyroom.entity.dto.user.UpdateProfileDTO;
import com.studyroom.entity.user.User;
import com.studyroom.entity.vo.user.EmailStatusVO;
import com.studyroom.entity.vo.user.UserVO;

/**
 * 用户信息服务接口（登录与账号模块）
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-003 用户信息授权：头像昵称更新</li>
 *   <li>FR-UC-004 邮箱绑定：SMTP 验证码 + AES-256-CBC 加密存储</li>
 * </ul>
 *
 * @author studyroom
 */
public interface UserInfoService {

    /** 查询当前用户信息（脱敏 VO） */
    UserVO getMyInfo(Long userId);

    /** 更新用户资料（头像/昵称/性别）—— FR-UC-003 */
    UserVO updateProfile(Long userId, UpdateProfileDTO dto);

    /** 发送邮箱验证码 —— FR-UC-004 第一步 */
    void sendEmailCode(Long userId, SendEmailCodeDTO dto);

    /** 绑定邮箱（验证码校验 + AES 加密落库）—— FR-UC-004 第二步 */
    void bindEmail(Long userId, BindEmailDTO dto);

    /** 邮箱绑定状态（是否绑定 + 脱敏地址） */
    EmailStatusVO getEmailStatus(Long userId);

    /** 实体 → 脱敏 VO（登录返回 / 个人中心复用） */
    UserVO toUserVO(User user);
}
