package com.studyroom.entity.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的信用 VO（FR-UC-053/054 用户端信用页）
 *
 * @author studyroom
 */
@Data
@ApiModel("我的信用")
public class CreditVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("信用分（0~100）")
    private Integer creditScore;

    /** 0正常 1限制中（FR-UC-054） */
    @ApiModelProperty("信用状态：0正常 1限制中")
    private Integer creditStatus;

    @ApiModelProperty("信用限制结束时间（限制中时有值，到期自动恢复）")
    private LocalDateTime creditLimitEnd;

    @ApiModelProperty("限制剩余天数（限制中时有值）")
    private Integer limitRemainingDays;

    @ApiModelProperty("30 天内爽约次数（FR-UC-053）")
    private Integer noShowCount30d;

    @ApiModelProperty("周期内爽约次数上限（=3，展示用）")
    private Integer noShowLimit;

    @ApiModelProperty("距离爽约次数统计窗口重置还剩天数")
    private Integer periodRemainingDays;

    @ApiModelProperty("近期爽约订单列表（最近 10 条）")
    private List<NoShowItem> noShows;

    @ApiModelProperty("信用变更流水（最近 20 条）")
    private List<CreditLogItem> logs;

    /**
     * 爽约订单项（FR-UC-053 爽约记录）
     */
    @Data
    @ApiModel("爽约记录项")
    public static class NoShowItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty("订单ID")
        private Long orderId;

        @ApiModelProperty("订单号")
        private String orderNo;

        @ApiModelProperty("预约开始时间（未按时签到的时间点）")
        private LocalDateTime startTime;

        @ApiModelProperty("爽约判定时间（订单创建+开始后 30 分钟自动取消）")
        private LocalDateTime noShowTime;
    }

    /**
     * 信用流水项（FR-UC-053/054 变更留痕）
     */
    @Data
    @ApiModel("信用流水项")
    public static class CreditLogItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty("流水类型：1爽约扣分 2恢复信用 3限制预约 4解除限制")
        private Integer type;

        @ApiModelProperty("信用分变化（正加负减）")
        private Integer scoreDelta;

        @ApiModelProperty("原因说明")
        private String reason;

        @ApiModelProperty("发生时间")
        private LocalDateTime time;
    }
}
