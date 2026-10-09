package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 续时费用预览 VO（FR-UC-055 使用中页续时弹层）
 *
 * @author studyroom
 */
@Data
@ApiModel("续时费用预览")
public class OrderExtendPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("当前套餐名称")
    private String packageName;

    @ApiModelProperty("当前结束时间")
    private LocalDateTime currentEndTime;

    @ApiModelProperty("续时小时数")
    private Integer hours;

    @ApiModelProperty("折算单价（元/小时）")
    private BigDecimal unitPrice;

    @ApiModelProperty("续时金额（单价×小时）")
    private BigDecimal amount;

    @ApiModelProperty("续时后新结束时间")
    private LocalDateTime newEndTime;

    @ApiModelProperty("账户余额（余额支付校验用）")
    private BigDecimal balance;

    @ApiModelProperty("已续时次数")
    private Integer extendCount;
}
