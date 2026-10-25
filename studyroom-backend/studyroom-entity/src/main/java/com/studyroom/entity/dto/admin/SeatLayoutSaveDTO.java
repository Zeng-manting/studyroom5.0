package com.studyroom.entity.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 管理端座位布局批量保存 DTO —— FR-AD-006
 *
 * <p>全量同步语义：payload 内带 id 的座位更新、不带 id 的插入；
 * 数据库中存在但 payload 缺失的座位软删除（有关联进行中订单时整体拒绝）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-座位布局保存")
public class SeatLayoutSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "画布上的全部座位（全量同步）", required = true)
    @Valid
    private List<SeatItem> seats;

    /**
     * 单个座位项
     */
    @Data
    @ApiModel("管理端-座位布局项")
    public static class SeatItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("座位ID（新增时为空，由后端生成）")
        private Long id;

        @ApiModelProperty(value = "座位编号（门店内唯一）", required = true, example = "A-12")
        @NotBlank(message = "座位编号不能为空")
        @Size(max = 32, message = "座位编号不能超过 32 字")
        private String seatNo;

        @ApiModelProperty(value = "座位类型ID", required = true)
        @NotNull(message = "座位类型不能为空")
        private Long seatTypeId;

        @ApiModelProperty("区域编码：window/corner/center")
        @Size(max = 32, message = "区域编码不能超过 32 字")
        private String areaCode;

        @ApiModelProperty(value = "画布X坐标", required = true)
        @NotNull(message = "画布X坐标不能为空")
        @Min(value = 0, message = "X坐标不能为负")
        @Max(value = 5000, message = "X坐标超出画布范围")
        @JsonProperty("xCoord")
        private Integer xCoord;

        @ApiModelProperty(value = "画布Y坐标", required = true)
        @NotNull(message = "画布Y坐标不能为空")
        @Min(value = 0, message = "Y坐标不能为负")
        @Max(value = 5000, message = "Y坐标超出画布范围")
        @JsonProperty("yCoord")
        private Integer yCoord;

        @ApiModelProperty("楼层")
        @Size(max = 16, message = "楼层不能超过 16 字")
        private String floor;

        @ApiModelProperty("备注")
        @Size(max = 255, message = "备注不能超过 255 字")
        private String description;
    }
}
