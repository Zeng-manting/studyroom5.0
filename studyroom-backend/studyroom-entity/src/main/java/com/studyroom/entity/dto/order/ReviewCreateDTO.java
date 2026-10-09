package com.studyroom.entity.dto.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 评价提交 DTO —— FR-UC-039
 * <p>5星打分 + 预设标签多选 + 文字 10-500 字 + 图片最多 6 张。</p>
 */
@Data
@ApiModel("评价提交参数")
public class ReviewCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单ID不能为空")
    @ApiModelProperty("订单ID（需已完成）")
    private Long orderId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    @ApiModelProperty("评分1-5")
    private Integer score;

    @Size(max = 10, message = "标签最多选择10个")
    @ApiModelProperty("预设标签（多选）")
    private List<String> tags;

    @NotNull(message = "评价内容不能为空")
    @Size(min = 10, max = 500, message = "评价内容需10-500字")
    @ApiModelProperty("文字评价 10-500 字")
    private String content;

    @ApiModelProperty("是否匿名：0否 1是")
    private Integer anonymous;

    @Valid
    @Size(max = 6, message = "图片最多6张")
    @ApiModelProperty("图片URL列表（先调 /file/upload），最多6张")
    private List<String> images;
}
