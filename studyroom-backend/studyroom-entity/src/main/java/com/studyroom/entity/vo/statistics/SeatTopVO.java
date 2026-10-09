package com.studyroom.entity.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 座位利用率排行项 VO —— FR-AD-003（Top10 排行）
 *
 * <p>口径：统计窗口内已消费订单（已签到流转：2进行中/3已完成/6已退款/7申诉中），
 * 使用时长优先取实际使用分钟数，未结算订单回退预约时长估算；
 * 按累计使用时长降序取前 N。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("座位利用率排行项")
public class SeatTopVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("座位ID")
    private Long seatId;

    @ApiModelProperty("座位编号（如 A-12）")
    private String seatNo;

    @ApiModelProperty("座位类型名称（普通区/静音区/包间/VIP）")
    private String typeName;

    @ApiModelProperty("窗口内订单数")
    private Long orderCount;

    @ApiModelProperty("窗口内累计使用小时（保留 1 位小数）")
    private BigDecimal usedHours;

    @ApiModelProperty("窗口内营收（元，实付合计）")
    private BigDecimal revenue;
}
