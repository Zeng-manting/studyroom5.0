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
import java.time.LocalDateTime;

/**
 * 签到申诉实体（order_appeal 表）
 * <p>FR-UC-032 校验失败与申诉：用户提交原因+文字说明+凭证图片；
 * 处理（标记签到成功/驳回）属管理端 FR-AD-011，模块9实现。</p>
 */
@Data
@TableName("order_appeal")
@ApiModel("签到申诉")
public class OrderAppeal implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ==================== 申诉状态常量 ==================== */
    /** 待审核 */
    public static final int STATUS_PENDING = 0;
    /** 已通过（FR-AD-011 标记签到成功） */
    public static final int STATUS_APPROVED = 1;
    /** 已驳回 */
    public static final int STATUS_REJECTED = 2;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty("申诉ID（雪花ID）")
    private Long id;

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("申诉人用户ID")
    private Long userId;

    @ApiModelProperty("申诉原因枚举：QR_EXPIRED/GPS_FAIL/TIME_WINDOW/ORDER_INVALID/OTHER")
    private String reasonType;

    @ApiModelProperty("申诉说明（≤500字）")
    private String description;

    /** JSON 数组字符串，如 ["/api/file/xxx.jpg"]，FR-CC-003 上传后回填 URL */
    @ApiModelProperty("凭证图片URL JSON数组")
    private String images;

    /** 0待审 1通过 2驳回，FR-AD-011 */
    @ApiModelProperty("0待审 1通过 2驳回")
    private Integer status;

    @ApiModelProperty("处理人（管理端）")
    private Long handlerId;

    @ApiModelProperty("处理意见")
    private String handleRemark;

    @ApiModelProperty("处理时间")
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
