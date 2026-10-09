package com.studyroom.entity.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务总览 VO —— FR-AD-024
 */
@Data
@ApiModel("财务总览")
public class FinanceSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("时间范围内总营收（实付合计）")
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @ApiModelProperty("时间范围内总退款")
    private BigDecimal totalRefund = BigDecimal.ZERO;

    @ApiModelProperty("净营收 = 总营收 - 总退款")
    private BigDecimal netRevenue = BigDecimal.ZERO;

    @ApiModelProperty("订单数（支付口径）")
    private Long orderCount = 0L;

    @ApiModelProperty("客单价 = 总营收 / 订单数")
    private BigDecimal avgOrderAmount = BigDecimal.ZERO;

    @ApiModelProperty("按日营收趋势（日期 + 营收 + 退款）")
    private List<DailyItem> dailyTrend = new ArrayList<>();

    /** 按日条目 */
    @Data
    @ApiModel("按日财务条目")
    public static class DailyItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("日期 yyyy-MM-dd")
        private String date;

        @ApiModelProperty("当日营收")
        private BigDecimal revenue;

        @ApiModelProperty("当日退款")
        private BigDecimal refund;

        @ApiModelProperty("当日净营收")
        private BigDecimal net;
    }
}
