package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 座位类型新建/编辑 DTO —— FR-AD-025 座位类型
 *
 * <p>座位类型基础价通过 price_rule（extra_price/extra_ratio）表达，
 * 本 DTO 管理类型元数据。</p>
 */
@Data
@ApiModel("座位类型保存请求")
public class SeatTypeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("类型ID（编辑时必传）")
    private Long id;

    @NotBlank(message = "类型名称不能为空")
    @Size(max = 32)
    @ApiModelProperty(value = "类型名称（如 普通区/VIP）", required = true)
    private String name;

    @NotBlank(message = "类型编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "编码为大写字母开头，含大写字母/数字/下划线")
    @ApiModelProperty(value = "类型编码（如 NORMAL/VIP），唯一", required = true)
    private String code;

    @Size(max = 255)
    @ApiModelProperty("描述")
    private String description;

    @Size(max = 255)
    @ApiModelProperty("图标")
    private String icon;

    @ApiModelProperty("排序（默认 0）")
    private Integer sort;

    @Min(0)
    @Max(1)
    @ApiModelProperty("0下架 1上架（默认 1）")
    private Integer status;
}
