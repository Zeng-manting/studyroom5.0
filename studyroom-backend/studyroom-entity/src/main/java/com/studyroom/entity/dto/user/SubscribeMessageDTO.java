package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 订阅消息授权上报 DTO —— FR-UC-044
 * <p>前端 uni.requestSubscribeMessage 回调后上报授权结果。</p>
 */
@Data
@ApiModel("订阅消息授权上报参数")
public class SubscribeMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "业务场景不能为空")
    @Pattern(regexp = "orderSuccess|startSoon|endSoon|cancel|refund|timeout",
            message = "非法业务场景")
    @ApiModelProperty("业务场景：orderSuccess/startSoon/endSoon/cancel/refund/timeout")
    private String scene;

    @ApiModelProperty("微信订阅消息模板ID（可空，使用后端配置）")
    private String templateId;

    @ApiModelProperty("0未授权 1已授权 2已拒绝")
    private Integer acceptFlag;
}
