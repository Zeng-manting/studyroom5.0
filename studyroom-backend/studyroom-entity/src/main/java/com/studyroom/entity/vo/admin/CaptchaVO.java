package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 图形验证码返回 VO —— FR-AD-001
 * <p>captchaId 为 Redis 键 studyroom:captcha:{uuid} 的 uuid 部分；
 * image 为 data:image/png;base64 前缀的 Base64 图片，前端直接 &lt;img :src&gt; 渲染。
 * 验证码 5 分钟有效、一次性消费（校验后立即删除，无论对错）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("图形验证码")
public class CaptchaVO {

    @ApiModelProperty("验证码 ID（登录时随 AdminLoginDTO 提交）")
    private String captchaId;

    @ApiModelProperty("Base64 图片（含 data:image/png;base64 前缀）")
    private String image;

    public static CaptchaVO of(String captchaId, String image) {
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setImage(image);
        return vo;
    }
}
