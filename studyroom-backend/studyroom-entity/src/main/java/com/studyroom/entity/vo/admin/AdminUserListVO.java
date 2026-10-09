package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端用户列表 VO —— FR-AD-014
 *
 * <p>手机号/邮箱为加密存储，列表仅输出解密后脱敏形式（138****1234 / a***@xx.com），
 * 明文不离开服务端。</p>
 */
@Data
@ApiModel("管理端-用户列表项")
public class AdminUserListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像")
    private String avatar;

    @ApiModelProperty("手机号（脱敏，未绑定为空）")
    private String phoneMasked;

    @ApiModelProperty("邮箱（脱敏，未绑定为空）")
    private String emailMasked;

    @ApiModelProperty("性别：0未知 1男 2女")
    private Integer gender;

    @ApiModelProperty("余额（元）")
    private BigDecimal balance;

    @ApiModelProperty("信用分")
    private Integer creditScore;

    @ApiModelProperty("信用状态：0正常 1受限")
    private Integer creditStatus;

    @ApiModelProperty("账号状态：0禁用 1正常")
    private Integer status;

    @ApiModelProperty("累计订单数（已支付口径）")
    private Long orderCount;

    @ApiModelProperty("累计学习时长（分钟，实际使用时长合计）")
    private Long totalStudyMinutes;

    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty("注册时间")
    private LocalDateTime createTime;
}
