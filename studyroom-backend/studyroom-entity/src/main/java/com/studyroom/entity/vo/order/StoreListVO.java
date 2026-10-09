package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店列表 VO（FR-UC-007~010 门店浏览）
 */
@Data
@ApiModel("门店列表项")
public class StoreListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("门店ID")
    private Long id;

    @ApiModelProperty("门店名称")
    private String name;

    @ApiModelProperty("门店地址")
    private String address;

    @ApiModelProperty("封面图（images 首图）")
    private String coverImage;

    @ApiModelProperty("综合评分")
    private BigDecimal rating;

    @ApiModelProperty("0休息中 1营业中 2维护中")
    private Integer status;

    @ApiModelProperty("状态描述：休息中/营业中/维护中")
    private String statusDesc;

    @ApiModelProperty("营业时间")
    private String businessHours;

    @ApiModelProperty("设施标签列表")
    private List<String> facilityList;
}
