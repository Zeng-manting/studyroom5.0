package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 管理端时段黑名单新增/编辑 DTO —— FR-AD-005
 *
 * <p>周期性规则（type=1）需填 weekDays（逗号分隔 1~7，1=周一 7=周日）；
 * 单次规则（type=2）需填 singleDate；时间区间 start &lt; end。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-时段黑名单保存")
public class BlacklistSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "规则类型：1周期性 2单次", required = true)
    @NotNull(message = "规则类型不能为空")
    @Min(value = 1, message = "规则类型非法")
    @Max(value = 2, message = "规则类型非法")
    private Integer type;

    @ApiModelProperty(value = "周期性：周几，逗号分隔（1=周一 7=周日），type=1 必填", example = "1,3,5")
    @Pattern(regexp = "^[1-7](,[1-7])*$", message = "周几格式应为逗号分隔的 1~7（1=周一 7=周日）")
    private String weekDays;

    @ApiModelProperty("单次：具体日期，type=2 必填")
    private LocalDate singleDate;

    @ApiModelProperty("开始时间（空=全天闭店）")
    private LocalTime startTime;

    @ApiModelProperty("结束时间（空=全天闭店）")
    private LocalTime endTime;

    @ApiModelProperty("原因（停业维护等）")
    @Size(max = 255, message = "原因不能超过 255 字")
    private String reason;

    @ApiModelProperty("0禁用 1启用（默认 1）")
    private Integer status;
}
