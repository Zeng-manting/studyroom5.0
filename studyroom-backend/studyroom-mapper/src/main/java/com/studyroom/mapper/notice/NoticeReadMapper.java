package com.studyroom.mapper.notice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.notice.NoticeRead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告已读 Mapper —— FR-UC-042 已读/未读标识
 */
@Mapper
public interface NoticeReadMapper extends BaseMapper<NoticeRead> {
}
