package com.studyroom.entity.review;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价实体（review 表）
 * <p>FR-UC-039 评价提交（一单一评，uk_order 唯一键）；
 * FR-UC-040 评价列表（status=1 显示，FR-AD-018 管理端可隐藏）。</p>
 */
@Data
@TableName("review")
@ApiModel("评价")
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：0隐藏 1显示（FR-AD-018） */
    public static final int STATUS_HIDDEN = 0;
    public static final int STATUS_VISIBLE = 1;

    @TableId
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("订单ID（一单一评）")
    private Long orderId;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("门店ID")
    private Long storeId;

    @ApiModelProperty("座位ID（可空，按门店维度评价）")
    private Long seatId;

    @ApiModelProperty("评分1-5，FR-UC-039")
    private Integer score;

    @ApiModelProperty("预设标签JSON数组，如 [\"安静\",\"环境好\"]，FR-UC-039")
    private String tags;

    @ApiModelProperty("评价内容 10-500 字，FR-UC-039")
    private String content;

    @ApiModelProperty("0不匿名 1匿名，FR-UC-040 昵称脱敏")
    private Integer anonymous;

    @ApiModelProperty("0隐藏 1显示，FR-AD-018")
    private Integer status;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
