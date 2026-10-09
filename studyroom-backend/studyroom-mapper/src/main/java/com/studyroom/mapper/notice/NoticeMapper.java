package com.studyroom.mapper.notice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.notice.Notice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告 Mapper —— FR-UC-041~043
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
