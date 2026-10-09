package com.studyroom.entity.dto.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 签到申诉提交 DTO（FR-UC-032）
 * <p>校验失败后用户提交：申诉原因 + 文字说明 + 凭证图片（先经 /api/file/upload 上传取回 URL）。</p>
 */
@Data
@ApiModel("签到申诉提交请求")
public class AppealCreateDTO {

    @NotNull(message = "订单ID不能为空")
    @ApiModelProperty(value = "订单ID", required = true)
    private Long orderId;

    @NotBlank(message = "申诉原因不能为空")
    @ApiModelProperty(value = "申诉原因：QR_EXPIRED/GPS_FAIL/TIME_WINDOW/ORDER_INVALID/OTHER", required = true)
    private String reasonType;

    @NotBlank(message = "请填写申诉说明")
    @Size(max = 500, message = "申诉说明不超过500字")
    @ApiModelProperty(value = "申诉说明（≤500字）", required = true)
    private String description;

    @Size(max = 6, message = "凭证图片最多6张")
    @ApiModelProperty("凭证图片URL列表（≤6张，FR-CC-003）")
    private List<String> images;
}
