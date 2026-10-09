package com.studyroom.entity.vo.statistics;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 座位状态总览单项 VO —— FR-AD-003（座位状态总览网格）
 *
 * <p>状态字典（FR-UC-017 六色）：0空闲(绿) 1使用中(蓝) 2维护(橙) 3打扫中(黄) 4锁定(红)；
 * 名称与颜色映射由前端维护，后端只回传状态码与数量。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("座位状态总览单项")
public class SeatStatusStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("座位状态：0空闲 1使用中 2维护 3打扫中 4锁定")
    private Integer status;

    @ApiModelProperty("该状态座位数")
    private Long count;
}
