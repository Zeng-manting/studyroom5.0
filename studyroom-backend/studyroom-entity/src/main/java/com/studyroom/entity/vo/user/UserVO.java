package com.studyroom.entity.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息 VO（个人中心展示、登录返回）
 *
 * <h3>安全说明</h3>
 * <ul>
 *   <li>不返回 openid / unionid / 邮箱密文 / 手机号密文等敏感字段</li>
 *   <li>邮箱仅返回"是否已绑定 + 脱敏地址"（a***@example.com）—— FR-UC-004</li>
 * </ul>
 *
 * @author studyroom
 */
@Data
@ApiModel("用户信息")
public class UserVO {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("性别：0未知 1男 2女")
    private Integer gender;

    @ApiModelProperty("是否已绑定邮箱 —— FR-UC-004")
    private Boolean emailBound;

    @ApiModelProperty("脱敏邮箱（未绑定为空）")
    private String emailMasked;

    @ApiModelProperty("钱包余额")
    private BigDecimal balance;

    @ApiModelProperty("信用分")
    private Integer creditScore;

    @ApiModelProperty("信用状态：0正常 1限制中 —— FR-UC-054")
    private Integer creditStatus;

    @ApiModelProperty("信用限制结束时间（未限制为空）")
    private LocalDateTime creditLimitEnd;

    @ApiModelProperty("30天内爽约次数 —— FR-UC-053")
    private Integer noShowCount30d;

    @ApiModelProperty("是否已同意隐私协议 —— FR-UC-050")
    private Boolean privacyAgreed;

    @ApiModelProperty("注册时间")
    private LocalDateTime createTime;
}
