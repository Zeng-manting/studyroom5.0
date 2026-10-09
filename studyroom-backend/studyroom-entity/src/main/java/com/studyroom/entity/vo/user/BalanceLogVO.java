package com.studyroom.entity.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额流水 VO（FR-UC-048 交易记录）
 */
@Data
@ApiModel("余额流水")
public class BalanceLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("流水ID")
    private Long id;

    @ApiModelProperty("1充值 2消费 3退款 4冻结 5解冻 6管理员调整")
    private Integer type;

    @ApiModelProperty("类型描述")
    private String typeDesc;

    @ApiModelProperty("变动金额（正负）")
    private BigDecimal amount;

    @ApiModelProperty("变动后余额")
    private BigDecimal balanceAfter;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("发生时间")
    private LocalDateTime createTime;
}
