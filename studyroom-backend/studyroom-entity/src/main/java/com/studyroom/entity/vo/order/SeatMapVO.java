package com.studyroom.entity.vo.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 座位平面图 VO（FR-UC-017 Canvas 座位平面图，六色状态标识）
 * <p>status 与颜色映射（前端 Canvas 渲染）：0空闲(绿) 1占用(灰) 2使用中(蓝) 3维护(橙) 4打扫(黄) 5锁定(红)</p>
 */
@Data
@ApiModel("座位平面图")
public class SeatMapVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("画布宽度（座位坐标最大 X + 边距）")
    private Integer canvasWidth;

    @ApiModelProperty("画布高度（座位坐标最大 Y + 边距）")
    private Integer canvasHeight;

    @ApiModelProperty("座位类型图例")
    private List<SeatTypeVO> seatTypes;

    @ApiModelProperty("座位列表")
    private List<SeatVO> seats;

    /**
     * 座位类型图例项
     */
    @Data
    @ApiModel("座位类型图例")
    public static class SeatTypeVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("类型ID")
        private Long id;

        @ApiModelProperty("类型名称")
        private String name;

        @ApiModelProperty("类型编码")
        private String code;

        @ApiModelProperty("图标")
        private String icon;
    }

    /**
     * 单个座位（含查询时段占用计算结果）
     */
    @Data
    @ApiModel("座位节点")
    public static class SeatVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("座位ID")
        private Long id;

        @ApiModelProperty("座位编号")
        private String seatNo;

        @ApiModelProperty("座位类型ID")
        private Long seatTypeId;

        @ApiModelProperty("楼层")
        private String floor;

        @ApiModelProperty("画布X坐标")
        @JsonProperty("xCoord")
        private Integer xCoord;

        @ApiModelProperty("画布Y坐标")
        @JsonProperty("yCoord")
        private Integer yCoord;

        /** 0空闲(绿) 1占用(灰) 2使用中(蓝) 3维护(橙) 4打扫(黄) 5锁定(红) */
        @ApiModelProperty("展示状态：0空闲 1占用 2使用中 3维护 4打扫 5锁定")
        private Integer displayStatus;

        @ApiModelProperty("状态描述")
        private String statusDesc;

        @ApiModelProperty("当前查询时段是否可选")
        private Boolean selectable;
    }
}
