package com.studyroom.entity.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 邮箱绑定状态 VO —— FR-UC-004
 *
 * @author studyroom
 */
@Data
@ApiModel("邮箱绑定状态")
public class EmailStatusVO {

    @ApiModelProperty("是否已绑定邮箱")
    private Boolean bound;

    @ApiModelProperty("脱敏邮箱（未绑定为空，如 a***@example.com）")
    private String emailMasked;

    @ApiModelProperty("验证码有效期（秒），用于前端倒计时展示")
    private Integer codeTtlSeconds;
}
