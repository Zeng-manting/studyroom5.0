package com.studyroom.entity.admin;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员操作日志实体（admin_log 表）—— FR-AD-028
 *
 * <p>由 AOP 切面（AdminLogAspect）自动记录 /admin/** 写操作；保留 90 天，
 * 由 AdminLogCleanupTask 定时清理。</p>
 */
@Data
@TableName("admin_log")
@ApiModel("管理员操作日志")
public class AdminLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("管理员ID")
    private Long adminId;

    @ApiModelProperty("冗余账号")
    private String username;

    @ApiModelProperty("操作描述")
    private String operation;

    @ApiModelProperty("模块")
    private String module;

    @ApiModelProperty("请求方法")
    private String method;

    @ApiModelProperty("请求URL")
    private String requestUrl;

    @ApiModelProperty("请求参数（脱敏后 JSON）")
    private String params;

    @ApiModelProperty("IP")
    private String ip;

    @ApiModelProperty("IP归属地")
    private String location;

    @ApiModelProperty("User-Agent")
    private String userAgent;

    @ApiModelProperty("0失败 1成功")
    private Integer status;

    @ApiModelProperty("错误信息")
    private String errorMsg;

    @ApiModelProperty("耗时（毫秒）")
    private Long costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
