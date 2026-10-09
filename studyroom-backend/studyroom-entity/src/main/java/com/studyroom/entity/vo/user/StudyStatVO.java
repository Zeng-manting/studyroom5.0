package com.studyroom.entity.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 学习统计 VO（FR-UC-049）
 * <p>累计时长/消费/天数 + 24 小时时段分布（柱状图）+ 近 90 天打卡日历热力图。</p>
 */
@Data
@ApiModel("学习统计")
public class StudyStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("累计学习分钟数（已完成订单实际时长）")
    private Long totalMinutes;

    @ApiModelProperty("累计学习天数（去重打卡日）")
    private Long totalDays;

    @ApiModelProperty("累计消费金额")
    private BigDecimal totalConsume;

    @ApiModelProperty("累计订单数")
    private Long totalOrders;

    @ApiModelProperty("24小时时段分布订单数（下标=小时 0~23，柱状图）")
    private List<Long> hourDistribution;

    @ApiModelProperty("近90天打卡热力图（按日期升序）")
    private List<DayHeat> heatmap;

    /**
     * 单日热力节点
     */
    @Data
    @ApiModel("打卡日热力节点")
    public static class DayHeat implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("日期 yyyy-MM-dd")
        private String date;

        @ApiModelProperty("当日学习分钟数（0=未打卡）")
        private Long minutes;
    }
}
