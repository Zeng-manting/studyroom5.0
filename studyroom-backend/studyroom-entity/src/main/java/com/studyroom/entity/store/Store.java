package com.studyroom.entity.store;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店实体（store 表）
 * <p>FR-UC-007~011 门店浏览；FR-UC-015 详情页展示；FR-AD-004 门店管理</p>
 */
@Data
@TableName("store")
@ApiModel("门店")
public class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
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

    /** JSON 数组字符串，如 ["插座","WiFi","静音区"] */
    @ApiModelProperty("设施标签JSON数组")
    private String facilities;

    /** JSON 数组字符串（图片 URL 列表，FR-UC-015 轮播） */
    @ApiModelProperty("环境图片JSON数组")
    private String images;

    @ApiModelProperty("综合评分")
    private BigDecimal rating;

    @ApiModelProperty("0休息中 1营业中 2维护中")
    private Integer status;

    @ApiModelProperty("排序")
    private Integer sort;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
