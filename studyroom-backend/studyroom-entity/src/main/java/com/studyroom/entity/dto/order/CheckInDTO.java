package com.studyroom.entity.dto.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 扫码签到请求 DTO（FR-UC-029 / FR-UC-030）
 * <p>qrcode 为 uni.scanCode 扫到的门店签到码全文；
 * latitude/longitude 为 uni.getLocation 获取的当前 GPS 坐标。</p>
 */
@Data
@ApiModel("扫码签到请求")
public class CheckInDTO {

    @NotBlank(message = "签到码不能为空")
    @ApiModelProperty(value = "扫码得到的签到码全文（SR-SIGN:密文:签名）", required = true)
    private String qrcode;

    @ApiModelProperty(value = "当前纬度（uni.getLocation）", required = true)
    private BigDecimal latitude;

    @ApiModelProperty(value = "当前经度（uni.getLocation）", required = true)
    private BigDecimal longitude;
}
