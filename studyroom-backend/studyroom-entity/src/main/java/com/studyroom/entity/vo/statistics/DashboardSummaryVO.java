package com.studyroom.entity.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 仪表盘核心指标 VO —— FR-AD-002（今日预约量 / 营收 / 上座率 / 活跃用户）
 *
 * <h3>统计口径</h3>
 * <ul>
 *   <li>今日预约量：当日创建且未取消（status != 4）的订单数；昨日同口径供环比</li>
 *   <li>今日营收：当日 pay_time 落在今日的实付金额合计（余额+微信，含后续退款，退款趋势见 FR-AD-021）</li>
 *   <li>上座率：此刻座位表状态 = 使用中(1) ÷ 可用座位（总数 - 维护中(2)），实时快照</li>
 *   <li>活跃用户：当日有登录行为（last_login_time 落在当日）的正常状态用户数</li>
 * </ul>
 *
 * <p>环比涨跌幅（%）由前端根据今日/昨日值计算，避免后端除零判断。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("仪表盘核心指标")
public class DashboardSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("今日预约量（当日创建且未取消）")
    private Long todayOrders;

    @ApiModelProperty("昨日预约量（同口径，供环比）")
    private Long yesterdayOrders;

    @ApiModelProperty("今日营收（元，实付合计）")
    private BigDecimal todayRevenue;

    @ApiModelProperty("昨日营收（元，供环比）")
    private BigDecimal yesterdayRevenue;

    @ApiModelProperty("今日活跃用户数（当日登录）")
    private Long activeUsers;

    @ApiModelProperty("昨日活跃用户数（供环比）")
    private Long yesterdayActiveUsers;

    @ApiModelProperty("座位总数")
    private Long seatTotal;

    @ApiModelProperty("使用中座位数（实时）")
    private Long seatInUse;

    @ApiModelProperty("可用座位数（总数 - 维护中）")
    private Long seatUsable;

    @ApiModelProperty("当前上座率（0~1 小数，前端转百分比展示）")
    private BigDecimal occupancyRate;
}
