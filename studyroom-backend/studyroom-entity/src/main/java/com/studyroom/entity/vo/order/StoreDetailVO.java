package com.studyroom.entity.vo.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 门店详情 VO（FR-UC-015 详情页展示：图片轮播、基本信息、地图、设施标签、评价摘要）
 */
@Data
@ApiModel("门店详情")
public class StoreDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("门店ID")
    private Long id;

    @ApiModelProperty("门店名称")
    private String name;

    @ApiModelProperty("门店地址")
    private String address;

    @ApiModelProperty("联系电话")
    private String phone;

    @ApiModelProperty("营业时间")
    private String businessHours;

    @ApiModelProperty("GPS纬度")
    private BigDecimal latitude;

    @ApiModelProperty("GPS经度")
    private BigDecimal longitude;

    @ApiModelProperty("门店简介")
    private String description;

    @ApiModelProperty("图片轮播列表")
    private List<String> imageList;

    @ApiModelProperty("设施标签列表")
    private List<String> facilityList;

    @ApiModelProperty("综合评分")
    private BigDecimal rating;

    @ApiModelProperty("0休息中 1营业中 2维护中")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;
}
