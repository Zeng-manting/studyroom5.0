package com.studyroom.entity.user;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体（对应 user 表）
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>邮箱/手机号以 AES-256-CBC 密文（Base64 后的字节）落库 VARBINARY —— FR-UC-004</li>
 *   <li>version 乐观锁：余额变动、信用状态变更等并发场景（配合 MybatisPlusConfig 插件）</li>
 *   <li>逻辑删除 deleted：@TableLogic，全局配置 0-未删 1-已删</li>
 *   <li>createTime/updateTime 由 MybatisMetaObjectHandler 自动填充</li>
 * </ul>
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-001 微信静默登录（openid 唯一标识）</li>
 *   <li>FR-UC-002 首次自动注册（默认昵称"自习用户"）</li>
 *   <li>FR-UC-003 用户信息授权（nickname/avatar）</li>
 *   <li>FR-UC-004 邮箱绑定（emailEncrypted）</li>
 * </ul>
 *
 * @author studyroom
 */
@Data
@TableName("user")
@ApiModel("用户")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花ID */
    @TableId
    @ApiModelProperty("用户ID")
    private Long id;

    /** 微信openid，静默登录唯一标识 */
    @ApiModelProperty("微信openid")
    private String openid;

    /** 微信unionid（多应用统一身份，可为空） */
    @ApiModelProperty("微信unionid")
    private String unionid;

    /** 昵称，默认"自习用户" */
    @ApiModelProperty("昵称")
    private String nickname;

    /** 头像URL */
    @ApiModelProperty("头像URL")
    private String avatar;

    /** 邮箱 AES-256-CBC 密文（Base64 字节，VARBINARY 存储）—— FR-UC-004 */
    @ApiModelProperty(value = "邮箱密文", hidden = true)
    private byte[] emailEncrypted;

    /** 手机号 AES 密文（预留） */
    @ApiModelProperty(value = "手机号密文", hidden = true)
    private byte[] phoneEncrypted;

    /** 性别：0未知 1男 2女 */
    @ApiModelProperty("性别：0未知 1男 2女")
    private Integer gender;

    /** 钱包余额 */
    @ApiModelProperty("钱包余额")
    private BigDecimal balance;

    /** 冻结余额（下单预扣） */
    @ApiModelProperty("冻结余额")
    private BigDecimal frozenBalance;

    /** 信用分 */
    @ApiModelProperty("信用分")
    private Integer creditScore;

    /** 信用状态：0正常 1限制中 —— FR-UC-054 */
    @ApiModelProperty("信用状态：0正常 1限制中")
    private Integer creditStatus;

    /** 信用限制结束时间 */
    @ApiModelProperty("信用限制结束时间")
    private LocalDateTime creditLimitEnd;

    /** 30天内爽约次数 —— FR-UC-053 */
    @TableField("no_show_count_30d")
    @ApiModelProperty("30天内爽约次数")
    private Integer noShowCount30d;

    /** 爽约明细 JSON（最近N条） */
    @ApiModelProperty(value = "爽约明细", hidden = true)
    private String noShowRecords;

    /** 账号状态：0禁用 1正常 */
    @ApiModelProperty("账号状态：0禁用 1正常")
    private Integer status;

    /** 最近登录时间 */
    @ApiModelProperty("最近登录时间")
    private LocalDateTime lastLoginTime;

    /** 最近登录IP */
    @ApiModelProperty("最近登录IP")
    private String lastLoginIp;

    /** 是否已同意隐私协议 —— FR-UC-050 */
    @ApiModelProperty("是否已同意隐私协议")
    private Integer privacyAgreed;

    /** 协议同意时间 */
    @ApiModelProperty("协议同意时间")
    private LocalDateTime privacyAgreeTime;

    /** 乐观锁 */
    @Version
    @ApiModelProperty(value = "乐观锁版本", hidden = true)
    private Integer version;

    /** 逻辑删除：0否 1是 */
    @TableLogic
    @TableField(select = false)
    @ApiModelProperty(value = "逻辑删除", hidden = true)
    private Integer deleted;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
