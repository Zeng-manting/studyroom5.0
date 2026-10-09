package com.studyroom.entity.store;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 座位类型实体（seat_type 表）
 */
@Data
@TableName("seat_type")
@ApiModel("座位类型")
public class SeatType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("类型ID")
    private Long id;

    @ApiModelProperty("类型名称：普通区/静音区/包间/VIP")
    private String name;

    @ApiModelProperty("类型编码：NORMAL/SILENT/ROOM/VIP")
    private String code;

    @ApiModelProperty("描述")
    private String description;

    @ApiModelProperty("图标")
    private String icon;

    @ApiModelProperty("排序")
    private Integer sort;

    @ApiModelProperty("0下架 1上架")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
