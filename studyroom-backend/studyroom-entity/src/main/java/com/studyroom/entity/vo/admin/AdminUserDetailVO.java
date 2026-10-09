package com.studyroom.entity.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户详情 VO —— FR-AD-014
 *
 * <p>基本信息 + 学习记录（最近订单）+ 评价记录三段组装。</p>
 */
@Data
@ApiModel("管理端-用户详情")
public class AdminUserDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("基本信息（列表字段全集）")
    private AdminUserListVO baseInfo;

    @ApiModelProperty("学习记录（最近 20 笔订单）")
    private List<StudyRecordItem> studyRecords;

    @ApiModelProperty("评价记录")
    private List<ReviewRecordItem> reviewRecords;

    /** 学习记录条目 */
    @Data
    @ApiModel("管理端-用户学习记录条目")
    public static class StudyRecordItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("订单ID")
        private Long orderId;

        @ApiModelProperty("订单号")
        private String orderNo;

        @ApiModelProperty("门店名")
        private String storeName;

        @ApiModelProperty("座位号")
        private String seatNo;

        @ApiModelProperty("套餐名")
        private String packageName;

        @ApiModelProperty("预约开始时间")
        private LocalDateTime startTime;

        @ApiModelProperty("预约结束时间")
        private LocalDateTime endTime;

        @ApiModelProperty("状态：0待支付 1已预约 2进行中 3已完成 4已取消 5退款中 6已退款 7申诉中")
        private Integer status;

        @ApiModelProperty("状态描述")
        private String statusDesc;

        @ApiModelProperty("实付金额")
        private BigDecimal payAmount;

        @ApiModelProperty("是否爽约")
        private Boolean noShow;

        @ApiModelProperty("实际使用时长（分钟，未结算为空）")
        private Integer actualDurationMinutes;

        @ApiModelProperty("下单时间")
        private LocalDateTime createTime;
    }

    /** 评价记录条目 */
    @Data
    @ApiModel("管理端-用户评价记录条目")
    public static class ReviewRecordItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("评价ID")
        private Long reviewId;

        @ApiModelProperty("门店名")
        private String storeName;

        @ApiModelProperty("评分 1-5")
        private Integer score;

        @ApiModelProperty("评价内容")
        private String content;

        @ApiModelProperty("0隐藏 1显示")
        private Integer status;

        @ApiModelProperty("管理员回复内容（无回复为空）")
        private String replyContent;

        @ApiModelProperty("评价时间")
        private LocalDateTime createTime;
    }
}
