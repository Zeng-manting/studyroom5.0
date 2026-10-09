package com.studyroom.mapper.notice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.notice.NoticeAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告附件 Mapper —— FR-UC-043 附件下载
 */
@Mapper
public interface NoticeAttachmentMapper extends BaseMapper<NoticeAttachment> {
}
