package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理端签到二维码 VO —— FR-AD-008
 *
 * <p>返回 QR 内容字符串与有效期；前端用 qrcode 库渲染成图、下载、打印。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-签到二维码")
public class AdminQrCodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("QR 内容（SR-SIGN:{AES密文}:{HMAC签名}）")
    private String code;

    @ApiModelProperty("有效期至")
    private LocalDateTime expireAt;

    @ApiModelProperty("门店名称（打印页展示）")
    private String storeName;

    @ApiModelProperty("门店ID")
    private Long storeId;
}
