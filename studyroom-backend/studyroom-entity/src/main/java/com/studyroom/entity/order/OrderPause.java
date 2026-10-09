package com.studyroom.entity.order;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单暂停记录实体（order_pause 表）
 * <p>FR-UC-033 中途临时离座：暂停计时、保留座位 30 分钟；
 * 超时未恢复由延迟队列自动结算（FR-SY-002）。</p>
 */
@Data
@TableName("order_pause")
@ApiModel("订单暂停记录")
public class OrderPause implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 暂停状态常量（FR-UC-033） ==================== */
    /** 暂停中 */
    public static final int STATUS_PAUSING = 0;
    /** 已恢复（用户回座） */
    public static final int STATUS_RESUMED = 1;
    /** 超时自动结算（30 分钟未回，FR-SY-002） */
    public static final int STATUS_TIMEOUT_SETTLED = 2;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty("暂停记录ID（雪花ID）")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("暂停开始时间")
    private LocalDateTime pauseStart;

    @ApiModelProperty("暂停结束时间（恢复/结算时回填）")
    private LocalDateTime pauseEnd;

    /** 0暂停中 1已恢复 2超时自动结算 */
    @ApiModelProperty("0暂停中 1已恢复 2超时自动结算")
    private Integer status;

    /** 是否系统自动结束：0否 1是 */
    @ApiModelProperty("是否系统自动结束")
    private Integer autoEnd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
