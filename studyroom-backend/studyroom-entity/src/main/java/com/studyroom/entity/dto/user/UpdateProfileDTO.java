package com.studyroom.entity.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 用户资料更新请求 DTO —— FR-UC-003 用户信息授权
 *
 * <p>微信头像昵称填写能力：button open-type="chooseAvatar" 得到头像临时文件 →
 * 上传 /api/file/upload 得到 URL；input type="nickname" 得到昵称。
 * 两个字段均可为空，仅更新传入项。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("用户资料更新请求")
public class UpdateProfileDTO {

    /** 头像 URL（先经 /api/file/upload 上传获取） */
    @Size(max = 512, message = "头像 URL 过长")
    @ApiModelProperty("头像 URL（先经 /api/file/upload 上传获取）")
    private String avatar;

    /** 昵称（微信 nickname 输入框回填） */
    @Size(min = 1, max = 64, message = "昵称长度需在 1-64 字符之间")
    @ApiModelProperty("昵称")
    private String nickname;

    /** 性别：0未知 1男 2女（可选） */
    @ApiModelProperty("性别：0未知 1男 2女")
    private Integer gender;
}
