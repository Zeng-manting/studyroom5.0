package com.studyroom.entity.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 日期趋势 VO —— FR-AD-020/021/022 通用双序列趋势
 *
 * <p>适用于：上座率趋势、退款趋势、客单价趋势、DAU 趋势、新增 vs 活跃等
 * 「日期 + 两列数值」型序列；无数据的日期由后端补 0。</p>
 */
@Data
@ApiModel("日期双序列趋势")
public class DateTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("日期序列 yyyy-MM-dd（连续无跳日）")
    private List<String> dates = new ArrayList<>();

    @ApiModelProperty("主序列（上座率%/退款金额/客单价/DAU/新增用户数等）")
    private List<BigDecimal> primary = new ArrayList<>();

    @ApiModelProperty("副序列（活跃用户数/订单量等，可空）")
    private List<BigDecimal> secondary = new ArrayList<>();
}
