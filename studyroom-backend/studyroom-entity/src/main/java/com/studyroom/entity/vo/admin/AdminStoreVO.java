package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端门店信息 VO —— FR-AD-004
 *
 * <p>列表与详情共用：facilities / images 已由 JSON 列解析为 List。
 * 与用户端 StoreDetailVO 的区别：包含排序、维护状态、创建时间等管理字段。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-门店信息")
public class AdminStoreVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("门店ID")
    private Long id;

    @ApiModelProperty("门店名称")
    private String name;

    @ApiModelProperty("门店地址")
    private String address;

    @ApiModelProperty("联系电话")
    private String phone;

    @ApiModelProperty("营业时间，如 08:00-23:00")
    private String businessHours;

    @ApiModelProperty("GPS纬度")
    private BigDecimal latitude;

    @ApiModelProperty("GPS经度")
    private BigDecimal longitude;

    @ApiModelProperty("门店简介")
    private String description;

    @ApiModelProperty("设施标签列表")
    private List<String> facilities;

    @ApiModelProperty("环境图片URL列表")
    private List<String> images;

    @ApiModelProperty("综合评分")
    private BigDecimal rating;

    @ApiModelProperty("0休息中 1营业中 2维护中")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("座位总数（含不可用）")
    private Long seatCount;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
