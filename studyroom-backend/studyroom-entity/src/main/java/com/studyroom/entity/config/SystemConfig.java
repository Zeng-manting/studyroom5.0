package com.studyroom.entity.config;

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
 * 系统配置实体（system_config 表）—— FR-AD-026
 *
 * <p>键值型配置：退款规则（refund 分组）、订单超时（order）、签到窗口（sign）、
 * 暂停上限（pause）、信用阈值（credit）、限流（limiter）、JWT 有效期（jwt）等，
 * 种子数据见 db/studyroom-db.sql 第 12.7 节。</p>
 */
@Data
@TableName("system_config")
@ApiModel("系统配置")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("配置键，如 refund.free_minutes")
    private String configKey;

    @ApiModelProperty("配置值")
    private String configValue;

    @ApiModelProperty("分组：refund/order/sign/pause/credit/limiter/jwt")
    private String configGroup;

    @ApiModelProperty("说明")
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
