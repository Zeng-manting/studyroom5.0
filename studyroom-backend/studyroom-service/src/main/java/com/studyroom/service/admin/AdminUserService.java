package com.studyroom.service.admin;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.vo.admin.AdminUserDetailVO;
import com.studyroom.entity.vo.admin.AdminUserListVO;

/**
 * 管理端用户管理服务 —— FR-AD-014
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>用户列表：关键字（昵称模糊）/ 账号状态 / 信用状态筛选，批量聚合订单数与学习时长</li>
 *   <li>用户详情：基本信息 + 学习记录（最近 20 笔订单）+ 评价记录</li>
 *   <li>禁用/启用账号：禁用前校验无进行中订单；禁用时站内信告知</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminUserService {

    /**
     * 用户分页列表（FR-AD-014）
     *
     * @param keyword      昵称模糊
     * @param status       账号状态：0禁用 1正常（空=全部）
     * @param creditStatus 信用状态：0正常 1受限（空=全部）
     */
    PageResult<AdminUserListVO> pageUsers(String keyword, Integer status, Integer creditStatus,
                                          long page, long size);

    /**
     * 用户详情（FR-AD-014）：基本信息 + 学习记录 + 评价记录
     */
    AdminUserDetailVO getUserDetail(Long userId);

    /**
     * 禁用/启用账号（FR-AD-014）
     *
     * <p>禁用校验：该用户无进行中(2)/已预约(1)订单，避免影响在场学习；
     * 禁用后用户端登录即被拒（UserInfoService/登录链路 status=0 拦截）。</p>
     *
     * @param userId  用户ID
     * @param disable true=禁用 false=启用
     */
    void toggleStatus(Long userId, boolean disable);
}
