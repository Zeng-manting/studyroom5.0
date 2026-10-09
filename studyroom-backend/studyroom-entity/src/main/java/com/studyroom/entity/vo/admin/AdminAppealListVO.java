package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端申诉列表/详情 VO —— FR-AD-011
 *
 * <p>列表与详情共用：详情即单条列表项（images 凭证图片 URL 已由 JSON 数组解析为 List）。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("管理端-签到申诉项")
public class AdminAppealListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("申诉ID")
    private Long appealId;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("业务订单号")
    private String orderNo;

    @ApiModelProperty("订单状态（用于判断处理语义：1已预约可补签到 / 6已退款可撤销爽约）")
    private Integer orderStatus;

    @ApiModelProperty("订单状态描述")
    private String orderStatusDesc;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("门店名称")
    private String storeName;

    @ApiModelProperty("座位号")
    private String seatNo;

    @ApiModelProperty("预约开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("申诉原因枚举：QR_EXPIRED/GPS_FAIL/TIME_WINDOW/ORDER_INVALID/OTHER")
    private String reasonType;

    @ApiModelProperty("申诉原因描述")
    private String reasonTypeDesc;

    @ApiModelProperty("申诉说明（≤500字）")
    private String description;

    @ApiModelProperty("凭证图片URL列表")
    private List<String> images;

    @ApiModelProperty("申诉状态：0待审 1通过 2驳回")
    private Integer status;

    @ApiModelProperty("状态描述")
    private String statusDesc;

    @ApiModelProperty("处理人管理员ID")
    private Long handlerId;

    @ApiModelProperty("处理意见")
    private String handleRemark;

    @ApiModelProperty("处理时间")
    private LocalDateTime handleTime;

    @ApiModelProperty("申诉提交时间")
    private LocalDateTime createTime;
}
