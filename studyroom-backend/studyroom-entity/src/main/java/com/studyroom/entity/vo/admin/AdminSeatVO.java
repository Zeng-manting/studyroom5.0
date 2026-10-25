package com.studyroom.entity.vo.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端座位项 VO —— FR-AD-006 / FR-AD-007
 *
 * <p>与管理端布局编辑画布对应：真实座位状态（seat.status 0空闲 1使用中 2维护 3打扫中 4锁定），
 * 区别于用户端 SeatMapVO 的展示状态（六色含订单占用计算）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-座位项")
public class AdminSeatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("座位ID（新建未保存时为空）")
    private Long id;

    @ApiModelProperty("座位编号（如 A-12）")
    private String seatNo;

    @ApiModelProperty("座位类型ID")
    private Long seatTypeId;

    @ApiModelProperty("座位类型名称")
    private String seatTypeName;

    @ApiModelProperty("区域编码：window/corner/center")
    private String areaCode;

    @ApiModelProperty("画布X坐标")
    @JsonProperty("xCoord")
    private Integer xCoord;

    @ApiModelProperty("画布Y坐标")
    @JsonProperty("yCoord")
    private Integer yCoord;

    @ApiModelProperty("楼层")
    private String floor;

    @ApiModelProperty("备注")
    private String description;

    @ApiModelProperty("真实状态：0空闲 1使用中 2维护 3打扫中 4锁定")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;

    @ApiModelProperty("是否有进行中订单（状态管理参考：使用中/占用不可置维护）")
    private Boolean hasActiveOrder;
}
