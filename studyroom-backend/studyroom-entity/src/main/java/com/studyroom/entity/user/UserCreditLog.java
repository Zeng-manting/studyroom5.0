package com.studyroom.entity.user;

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
 * 信用变更流水实体（user_credit_log 表）
 * <p>FR-UC-053 爽约扣分 / FR-UC-054 限制预约与解除限制 / 恢复信用的全量留痕；
 * 管理端爽约记录查询（FR-AD 系列）与用户端信用页均以此表为数据源。</p>
 *
 * @author studyroom
 */
@Data
@TableName("user_credit_log")
@ApiModel("信用变更流水")
public class UserCreditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 流水类型常量 ==================== */
    /** 爽约扣分（FR-UC-053） */
    public static final int TYPE_NO_SHOW = 1;
    /** 恢复信用（限制到期自动恢复，FR-UC-054） */
    public static final int TYPE_RESTORE = 2;
    /** 限制预约（30 天内爽约达上限，FR-UC-054） */
    public static final int TYPE_LIMIT = 3;
    /** 解除限制（管理端手动解除，FR-UC-054） */
    public static final int TYPE_UNLIMIT = 4;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty("流水ID（雪花ID）")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("关联订单ID（恢复/限制类流水填触发的订单ID，可空）")
    private Long orderId;

    /** 1爽约扣分 2恢复信用 3限制预约 4解除限制 */
    @ApiModelProperty("类型：1爽约扣分 2恢复信用 3限制预约 4解除限制")
    private Integer type;

    @ApiModelProperty("信用分变化（正数加分，负数扣分）")
    private Integer scoreDelta;

    @ApiModelProperty("原因说明")
    private String reason;

    @ApiModelProperty("操作人（管理员ID；系统自动为 NULL）")
    private Long operatorId;

    /** 本表仅 create_time，无 update_time/deleted（append-only 流水） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
