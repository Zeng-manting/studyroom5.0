package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 门店某日时段黑名单 VO（FR-UC-020 时段黑名单置灰）
 */
@Data
@ApiModel("时段黑名单")
public class StoreBlacklistVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("日期")
    private LocalDate date;

    @ApiModelProperty("黑名单时段列表")
    private List<Slot> slots;

    @ApiModelProperty("全天闭店")
    private Boolean allDayClosed;

    /**
     * 单个闭店时段
     */
    @Data
    @ApiModel("闭店时段")
    public static class Slot implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("开始时间 HH:mm")
        private LocalTime startTime;

        @ApiModelProperty("结束时间 HH:mm")
        private LocalTime endTime;

        @ApiModelProperty("原因")
        private String reason;
    }
}
