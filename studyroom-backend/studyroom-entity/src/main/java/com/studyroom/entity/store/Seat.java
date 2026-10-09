package com.studyroom.entity.store;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 座位实体（seat 表）
 * <p>FR-UC-017 座位平面图六色状态；FR-UC-018 乐观锁防并发；FR-AD-006/007 座位管理</p>
 */
@Data
@TableName("seat")
@ApiModel("座位")
public class Seat implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty("座位ID")
    private Long id;

    @ApiModelProperty("门店ID")
    private Long storeId;

    @ApiModelProperty("座位类型ID")
    private Long seatTypeId;

    @ApiModelProperty("座位编号（如 A-12）")
    private String seatNo;

    @ApiModelProperty("区域编码：window/corner/center")
    private String areaCode;

    @ApiModelProperty("画布X坐标")
    private Integer xCoord;

    @ApiModelProperty("画布Y坐标")
    private Integer yCoord;

    @ApiModelProperty("楼层")
    private String floor;

    @ApiModelProperty("备注")
    private String description;

    /** 0空闲 1使用中 2维护 3打扫中 4锁定，FR-UC-017 六色状态 */
    @ApiModelProperty("0空闲 1使用中 2维护 3打扫中 4锁定")
    private Integer status;

    /** 乐观锁，FR-UC-018 锁座并发 */
    @Version
    private Integer version;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
