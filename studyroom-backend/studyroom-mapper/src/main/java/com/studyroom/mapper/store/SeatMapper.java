package com.studyroom.mapper.store;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.store.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 座位 Mapper（FR-UC-017 座位图 / FR-UC-018 锁座 / FR-AD-006/007）
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * CAS 更新座位状态（乐观锁 version 由 MyBatis-Plus @Version 插件处理，
     * 此处提供显式 CAS 供"离座转打扫"等场景使用，FR-UC-034）
     *
     * @return 影响行数（0 表示并发冲突或状态已变）
     */
    @Update("UPDATE seat SET status = #{targetStatus}, version = version + 1 " +
            "WHERE id = #{seatId} AND status = #{expectStatus} AND deleted = 0")
    int casUpdateStatus(@Param("seatId") Long seatId,
                        @Param("expectStatus") Integer expectStatus,
                        @Param("targetStatus") Integer targetStatus);
}
