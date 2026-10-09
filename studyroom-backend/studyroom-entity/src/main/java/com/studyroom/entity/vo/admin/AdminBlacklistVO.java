package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端时段黑名单 VO —— FR-AD-005
 *
 * <p>规则列表项 + 月历视图数据（{@link CalendarDay} 按日聚合命中规则）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-时段黑名单")
public class AdminBlacklistVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("规则ID")
    private Long id;

    @ApiModelProperty("门店ID")
    private Long storeId;

    @ApiModelProperty("1周期性 2单次")
    private Integer type;

    @ApiModelProperty("类型描述")
    private String typeDesc;

    @ApiModelProperty("周期性：周几，逗号分隔（1=周一 7=周日）")
    private String weekDays;

    @ApiModelProperty("周期性：周几描述（如 周一,周三,周五）")
    private String weekDaysDesc;

    @ApiModelProperty("单次：具体日期")
    private LocalDate singleDate;

    @ApiModelProperty("开始时间")
    private LocalTime startTime;

    @ApiModelProperty("结束时间")
    private LocalTime endTime;

    @ApiModelProperty("原因")
    private String reason;

    @ApiModelProperty("0禁用 1启用")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 月历视图单日数据（FR-AD-005 日历视图展示）
     */
    @Data
    @ApiModel("管理端-黑名单月历单日")
    public static class CalendarDay implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("日期 yyyy-MM-dd")
        private LocalDate date;

        @ApiModelProperty("是否全天闭店")
        private Boolean allDayClosed;

        @ApiModelProperty("命中的闭店时段")
        private List<Slot> slots = new ArrayList<>();

        @ApiModelProperty("今日（供前端高亮）")
        private Boolean today;
    }

    /**
     * 月历视图单日下的闭店时段
     */
    @Data
    @ApiModel("管理端-黑名单时段")
    public static class Slot implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("开始时间")
        private LocalTime startTime;

        @ApiModelProperty("结束时间")
        private LocalTime endTime;

        @ApiModelProperty("原因")
        private String reason;
    }
}
