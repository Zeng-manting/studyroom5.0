package com.studyroom.entity.store;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 门店时段黑名单实体（store_blacklist 表）
 * <p>FR-UC-020 时段黑名单：周期性/单次闭店时段自动置灰；FR-AD-005 黑名单管理</p>
 */
@Data
@TableName("store_blacklist")
@ApiModel("门店时段黑名单")
public class StoreBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("门店ID")
    private Long storeId;

    @ApiModelProperty("1周期性 2单次")
    private Integer type;

    /** 周期性：周几，逗号分隔，1=周一 7=周日 */
    @ApiModelProperty("周期性：周几，逗号分隔")
    private String weekDays;

    @ApiModelProperty("单次：具体日期")
    private LocalDate singleDate;

    @ApiModelProperty("开始时间")
    private LocalTime startTime;

    @ApiModelProperty("结束时间")
    private LocalTime endTime;

    @ApiModelProperty("原因（停业维护等）")
    private String reason;

    @ApiModelProperty("0禁用 1启用")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
