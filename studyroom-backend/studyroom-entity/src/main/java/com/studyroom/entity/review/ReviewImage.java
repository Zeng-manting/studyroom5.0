package com.studyroom.entity.review;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 评价图片实体（review_image 表）—— FR-UC-039 图片最多 6 张
 */
@Data
@TableName("review_image")
@ApiModel("评价图片")
public class ReviewImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("评价ID")
    private Long reviewId;

    @ApiModelProperty("图片URL")
    private String imageUrl;

    @ApiModelProperty("排序")
    private Integer sort;
}
