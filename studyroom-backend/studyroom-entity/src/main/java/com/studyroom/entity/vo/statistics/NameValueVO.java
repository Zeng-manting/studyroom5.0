package com.studyroom.entity.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 统计名值对 VO —— FR-AD-019/020/021/022 通用
 *
 * <p>适用于：套餐占比、时段分布、类型分布、收入构成、时长直方图等
 * 「名称 + 数量/金额」型序列，前端直接喂 ECharts 饼图/柱图。</p>
 */
@Data
@ApiModel("统计名值对")
public class NameValueVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("名称（套餐名/时段/类型名等）")
    private String name;

    @ApiModelProperty("数量")
    private Long count;

    @ApiModelProperty("金额（收入构成等场景）")
    private BigDecimal amount;

    public NameValueVO() {
    }

    public NameValueVO(String name, Long count) {
        this.name = name;
        this.count = count;
    }

    public NameValueVO(String name, Long count, BigDecimal amount) {
        this.name = name;
        this.count = count;
        this.amount = amount;
    }
}
