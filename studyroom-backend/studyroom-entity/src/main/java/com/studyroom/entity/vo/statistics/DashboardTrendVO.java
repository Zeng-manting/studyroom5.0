package com.studyroom.entity.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 仪表盘趋势单日数据 VO —— FR-AD-003（7 天 / 30 天折线图）
 *
 * <p>预约量按订单创建日聚合（未取消），营收按支付日聚合；
 * 无数据日期由后端补 0，保证前端横轴连续。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("仪表盘趋势单日数据")
public class DashboardTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("日期 yyyy-MM-dd")
    private String date;

    @ApiModelProperty("当日预约量（创建且未取消）")
    private Long orderCount;

    @ApiModelProperty("当日营收（元，实付合计）")
    private BigDecimal revenue;
}
