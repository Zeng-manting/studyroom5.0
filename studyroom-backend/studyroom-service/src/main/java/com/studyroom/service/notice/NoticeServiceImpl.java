package com.studyroom.service.notice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.entity.notice.Notice;
import com.studyroom.entity.notice.NoticeAttachment;
import com.studyroom.entity.notice.NoticeRead;
import com.studyroom.entity.vo.notice.NoticeDetailVO;
import com.studyroom.entity.vo.notice.NoticeListVO;
import com.studyroom.mapper.notice.NoticeAttachmentMapper;
import com.studyroom.mapper.notice.NoticeMapper;
import com.studyroom.mapper.notice.NoticeReadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公告服务实现（FR-UC-041~043）
 * <p>用户端仅展示 status=1 已发布且 publish_time &le; now 的公告；
 * 管理端维护（FR-AD-015/016）在模块9实现。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeAttachmentMapper attachmentMapper;
    private final NoticeReadMapper noticeReadMapper;

    @Override
    public PageResult<NoticeListVO> listNotices(Long userId, Integer type, long page, long size) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, Notice.STATUS_PUBLISHED)
                .le(Notice::getPublishTime, LocalDateTime.now())
                .orderByDesc(Notice::getIsImportant)
                .orderByDesc(Notice::getPublishTime);
        // FR-UC-041 分类筛选
        if (type != null && type > 0) {
            wrapper.eq(Notice::getType, type);
        }
        Page<Notice> p = noticeMapper.selectPage(new Page<>(page, size), wrapper);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }

        // FR-UC-042 已读标识：批量查 notice_read
        Set<Long> readIds = selectReadIds(userId,
                p.getRecords().stream().map(Notice::getId).collect(Collectors.toList()));

        List<NoticeListVO> vos = p.getRecords().stream().map(n -> {
            NoticeListVO vo = new NoticeListVO();
            vo.setId(n.getId());
            vo.setTitle(n.getTitle());
            vo.setType(n.getType());
            vo.setTypeDesc(typeDesc(n.getType()));
            vo.setImportant(n.getIsImportant() != null && n.getIsImportant() == 1);
            vo.setHasRead(readIds.contains(n.getId()));
            vo.setPublishTime(n.getPublishTime());
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public NoticeDetailVO getNoticeDetail(Long userId, Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null || notice.getStatus() != Notice.STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "公告不存在或已下架");
        }

        // FR-UC-043：首次访问落已读 + 阅读量自增（uk_user_notice 幂等）
        boolean firstRead = markRead(userId, noticeId);
        if (firstRead) {
            notice.setReadCount((notice.getReadCount() == null ? 0 : notice.getReadCount()) + 1);
            noticeMapper.updateById(notice);
        }

        NoticeDetailVO vo = new NoticeDetailVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setType(notice.getType());
        vo.setTypeDesc(typeDesc(notice.getType()));
        vo.setImportant(notice.getIsImportant() != null && notice.getIsImportant() == 1);
        vo.setHasRead(true);
        vo.setPublishTime(notice.getPublishTime());
        vo.setContent(notice.getContent());
        vo.setReadCount(notice.getReadCount());

        // 附件（FR-UC-043 附件下载）
        List<NoticeAttachment> atts = attachmentMapper.selectList(
                new LambdaQueryWrapper<NoticeAttachment>()
                        .eq(NoticeAttachment::getNoticeId, noticeId)
                        .orderByAsc(NoticeAttachment::getSort));
        vo.setAttachments(atts.stream().map(a -> {
            NoticeDetailVO.AttachmentVO av = new NoticeDetailVO.AttachmentVO();
            av.setFileName(a.getFileName());
            av.setFileUrl(a.getFileUrl());
            av.setFileSize(a.getFileSize());
            return av;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public NoticeDetailVO getImportantPopup(Long userId) {
        // FR-UC-042：最新一条重要且未读的已发布公告
        Notice latest = noticeMapper.selectOne(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, Notice.STATUS_PUBLISHED)
                .eq(Notice::getIsImportant, 1)
                .le(Notice::getPublishTime, LocalDateTime.now())
                .orderByDesc(Notice::getPublishTime)
                .last("LIMIT 1"));
        if (latest == null) {
            return null;
        }
        Set<Long> readIds = selectReadIds(userId, Collections.singletonList(latest.getId()));
        if (readIds.contains(latest.getId())) {
            return null; // 已读过，不再弹窗
        }
        NoticeDetailVO vo = new NoticeDetailVO();
        vo.setId(latest.getId());
        vo.setTitle(latest.getTitle());
        vo.setType(latest.getType());
        vo.setTypeDesc(typeDesc(latest.getType()));
        vo.setImportant(true);
        vo.setHasRead(false);
        vo.setPublishTime(latest.getPublishTime());
        vo.setContent(latest.getContent());
        vo.setReadCount(latest.getReadCount());
        return vo;
    }

    /* ==================== 私有方法 ==================== */

    /** 批量查询用户已读公告 ID 集合 */
    private Set<Long> selectReadIds(Long userId, List<Long> noticeIds) {
        if (userId == null || noticeIds == null || noticeIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(noticeReadMapper.selectList(new LambdaQueryWrapper<NoticeRead>()
                        .eq(NoticeRead::getUserId, userId)
                        .in(NoticeRead::getNoticeId, noticeIds))
                .stream().map(NoticeRead::getNoticeId).collect(Collectors.toSet()));
    }

    /**
     * 落已读记录（uk_user_scene 唯一键冲突 = 已读过）
     *
     * @return true=首次阅读
     */
    private boolean markRead(Long userId, Long noticeId) {
        if (userId == null) {
            return false;
        }
        NoticeRead read = new NoticeRead();
        read.setId(IdGenerator.nextId());
        read.setUserId(userId);
        read.setNoticeId(noticeId);
        read.setReadTime(LocalDateTime.now());
        try {
            noticeReadMapper.insert(read);
            return true;
        } catch (DuplicateKeyException e) {
            return false; // 已读过
        }
    }

    private String typeDesc(Integer type) {
        if (type == null) {
            return "其他";
        }
        switch (type) {
            case Notice.TYPE_RULE: return "门店规则";
            case Notice.TYPE_CLOSURE: return "停业通知";
            case Notice.TYPE_ACTIVITY: return "活动公告";
            default: return "其他";
        }
    }
}
