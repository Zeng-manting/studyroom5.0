package com.studyroom.entity.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端门店新增/编辑 DTO —— FR-AD-004
 *
 * <p>名称、地址、营业时间、GPS坐标、设施标签、环境图片。
 * 营业时间格式 HH:mm-HH:mm（如 08:00-23:00）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-门店保存")
public class StoreSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "门店名称", required = true, example = "中科智慧自习室")
    @NotBlank(message = "门店名称不能为空")
    @Size(max = 64, message = "门店名称不能超过 64 字")
    private String name;

    @ApiModelProperty(value = "门店地址", required = true)
    @NotBlank(message = "门店地址不能为空")
    @Size(max = 255, message = "门店地址不能超过 255 字")
    private String address;

    @ApiModelProperty("联系电话")
    @Size(max = 32, message = "联系电话不能超过 32 字")
    private String phone;

    @ApiModelProperty(value = "营业时间", example = "08:00-23:00")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$",
            message = "营业时间格式应为 HH:mm-HH:mm，如 08:00-23:00")
    private String businessHours;

    @ApiModelProperty("GPS纬度")
    private BigDecimal latitude;

    @ApiModelProperty("GPS经度")
    private BigDecimal longitude;

    @ApiModelProperty("门店简介")
    @Size(max = 1000, message = "门店简介不能超过 1000 字")
    private String description;

    @ApiModelProperty("设施标签列表，如 [\"插座\",\"WiFi\",\"静音区\"]")
    @Valid
    private List<String> facilities;

    @ApiModelProperty("环境图片URL列表")
    @Valid
    private List<String> images;

    @ApiModelProperty("0休息中 1营业中 2维护中")
    private Integer status;

    @ApiModelProperty("排序（越小越靠前）")
    private Integer sort;
}
