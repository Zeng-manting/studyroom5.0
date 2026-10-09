package com.studyroom.entity.order;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到记录实体（order_sign 表）
 * <p>FR-UC-029 扫码签到、FR-UC-030 五重校验结果留痕、FR-UC-031 签到成功落库。
 * 无论校验成功还是失败均插入一条记录（审计 + 申诉依据，FR-SY-003）。</p>
 */
@Data
@TableName("order_sign")
@ApiModel("签到记录")
public class OrderSign implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 校验结果常量（FR-UC-030） ==================== */
    /** 校验失败 */
    public static final int RESULT_FAIL = 0;
    /** 校验成功 */
    public static final int RESULT_SUCCESS = 1;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty("签到记录ID（雪花ID）")
    private Long id;

    @ApiModelProperty("订单ID（校验前期失败可能无法关联，为空）")
    private Long orderId;

    @ApiModelProperty("扫码内容明文（仅签名比对，不存敏感字段）")
    private String qrcodePlain;

    /** AES-256-CBC 密文 VARBINARY(512)，实体按 Base64 字符串承载 */
    @ApiModelProperty("AES 加密内容（Base64）")
    private String qrcodeEncrypted;

    @ApiModelProperty("HMAC-SHA256 签名（Base64）")
    private String qrcodeSign;

    @ApiModelProperty("签到时间")
    private LocalDateTime signTime;

    @ApiModelProperty("签到纬度")
    private BigDecimal signLatitude;

    @ApiModelProperty("签到经度")
    private BigDecimal signLongitude;

    @ApiModelProperty("GPS 距离门店米数")
    private Integer gpsDistance;

    /** 0失败 1成功，FR-UC-030 五重校验结果 */
    @ApiModelProperty("五重校验结果：0失败 1成功")
    private Integer checkResult;

    @ApiModelProperty("失败原因（成功为空）")
    private String failReason;

    @ApiModelProperty("客户端UA")
    private String userAgent;

    @ApiModelProperty("客户端IP")
    private String clientIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
