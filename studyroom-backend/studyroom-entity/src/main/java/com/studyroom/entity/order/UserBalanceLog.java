package com.studyroom.entity.order;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额流水实体（user_balance_log 表）
 * <p>FR-UC-023 余额支付扣减；FR-UC-027 阶梯退款回充；FR-UC-048 充值/消费记录。</p>
 */
@Data
@TableName("user_balance_log")
@ApiModel("余额流水")
public class UserBalanceLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流水类型：1充值 2消费 3退款 4冻结 5解冻 6管理员调整 */
    public static final int TYPE_RECHARGE = 1;
    public static final int TYPE_CONSUME = 2;
    public static final int TYPE_REFUND = 3;
    public static final int TYPE_FREEZE = 4;
    public static final int TYPE_UNFREEZE = 5;
    public static final int TYPE_ADJUST = 6;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("1充值 2消费 3退款 4冻结 5解冻 6管理员调整")
    private Integer type;

    @ApiModelProperty("变动金额（正负）")
    private BigDecimal amount;

    @ApiModelProperty("变动前余额")
    private BigDecimal balanceBefore;

    @ApiModelProperty("变动后余额")
    private BigDecimal balanceAfter;

    @ApiModelProperty("业务类型：order/refund/recharge")
    private String bizType;

    @ApiModelProperty("业务ID")
    private Long bizId;

    @ApiModelProperty("备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
