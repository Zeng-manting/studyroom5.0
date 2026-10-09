package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.PageResult;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.notice.Notice;
import com.studyroom.entity.notice.NoticeAttachment;
import com.studyroom.entity.dto.admin.NoticeSaveDTO;
import com.studyroom.entity.vo.admin.AdminNoticeVO;
import com.studyroom.entity.store.Store;
import com.studyroom.mapper.notice.NoticeAttachmentMapper;
import com.studyroom.mapper.notice.NoticeMapper;
import com.studyroom.mapper.store.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端公告管理服务实现 —— FR-AD-015/016
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>附件全量覆盖式保存（先删后插），与座位布局保存同思路</li>
 *   <li>已发布公告编辑不回退状态，仅刷新更新时间；重新发布会刷新发布时间</li>
 *   <li>阅读量 read_count 由用户端 NoticeService 首次访问已读时自增，此处只读</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeServiceImpl implements AdminNoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeAttachmentMapper noticeAttachmentMapper;
    private final StoreMapper storeMapper;

    /* ==================== 公告列表 ==================== */

    @Override
    public PageResult<AdminNoticeVO> pageNotices(Integer type, Integer status, long page, long size) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Notice::getType, type);
        }
        if (status != null) {
            wrapper.eq(Notice::getStatus, status);
        }
        wrapper.orderByDesc(Notice::getCreateTime);

        Page<Notice> p = noticeMapper.selectPage(new Page<>(page, size), wrapper);
        List<Notice> records = p.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), p.getTotal(), p.getCurrent(), p.getSize());
        }
        // 批量补门店名（列表不携带 content 大字段与附件）
        Map<Long, String> storeNames = loadStoreNames(records);
        return new PageResult<>(records.stream().map(n -> toListVO(n, storeNames))
                        .collect(Collectors.toList()),
                p.getTotal(), p.getCurrent(), p.getSize());
    }

    /* ==================== 公告详情（编辑回显） ==================== */

    @Override
    public AdminNoticeVO getNotice(Long noticeId) {
        Notice notice = requireNotice(noticeId);
        AdminNoticeVO vo = toListVO(notice, loadStoreNames(Collections.singletonList(notice)));
        vo.setContent(notice.getContent());
        List<NoticeAttachment> attachments = noticeAttachmentMapper.selectList(
                new LambdaQueryWrapper<NoticeAttachment>()
                        .eq(NoticeAttachment::getNoticeId, noticeId)
                        .orderByAsc(NoticeAttachment::getSort));
        vo.setAttachments(attachments.stream().map(a -> {
            AdminNoticeVO.AttachmentItem item = new AdminNoticeVO.AttachmentItem();
            item.setFileName(a.getFileName());
            item.setFileUrl(a.getFileUrl());
            item.setFileSize(a.getFileSize());
            item.setSort(a.getSort());
            return item;
        }).collect(Collectors.toList()));
        return vo;
    }

    /* ==================== 新建/编辑 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveNotice(NoticeSaveDTO dto, Long operatorId) {
        Notice notice;
        if (dto.getId() == null) {
            // 新建
            notice = new Notice();
            notice.setReadCount(0);
            notice.setStatus(Notice.STATUS_DRAFT);
        } else {
            notice = requireNotice(dto.getId());
        }

        notice.setStoreId(dto.getStoreId());
        notice.setTitle(dto.getTitle().trim());
        notice.setType(dto.getType());
        notice.setContent(dto.getContent());
        notice.setIsImportant(dto.getIsImportant());
        notice.setPublisherId(operatorId);

        if (notice.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }

        // 附件全量覆盖式保存
        syncAttachments(notice.getId(), dto.getAttachments());

        // 直接发布（FR-AD-015：保存并发布一步完成）
        if (Boolean.TRUE.equals(dto.getPublishNow())) {
            publish(notice.getId(), operatorId);
        }

        log.info("[AdminNotice] FR-AD-015 保存公告: id={}, title={}, publishNow={}",
                notice.getId(), notice.getTitle(), dto.getPublishNow());
        return notice.getId();
    }

    /* ==================== 发布/下架/删除 ==================== */

    @Override
    public void publish(Long noticeId, Long operatorId) {
        Notice notice = requireNotice(noticeId);
        if (notice.getStatus() == Notice.STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该公告已是发布状态");
        }
        Notice patch = new Notice();
        patch.setId(noticeId);
        patch.setStatus(Notice.STATUS_PUBLISHED);
        patch.setPublishTime(java.time.LocalDateTime.now());
        patch.setPublisherId(operatorId);
        noticeMapper.updateById(patch);
        log.info("[AdminNotice] FR-AD-016 发布公告: id={}, operator={}", noticeId, operatorId);
    }

    @Override
    public void offline(Long noticeId) {
        Notice notice = requireNotice(noticeId);
        if (notice.getStatus() != Notice.STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅已发布公告可下架");
        }
        Notice patch = new Notice();
        patch.setId(noticeId);
        patch.setStatus(Notice.STATUS_OFFLINE);
        noticeMapper.updateById(patch);
        log.info("[AdminNotice] FR-AD-016 下架公告: id={}", noticeId);
    }

    @Override
    public void deleteNotice(Long noticeId) {
        requireNotice(noticeId);
        noticeMapper.deleteById(noticeId);
        log.info("[AdminNotice] FR-AD-016 删除公告: id={}", noticeId);
    }

    /* ==================== 内部方法 ==================== */

    private Notice requireNotice(Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return notice;
    }

    /** 附件全量覆盖：先删旧再插新 */
    private void syncAttachments(Long noticeId, List<NoticeSaveDTO.AttachmentItem> attachments) {
        noticeAttachmentMapper.delete(new LambdaQueryWrapper<NoticeAttachment>()
                .eq(NoticeAttachment::getNoticeId, noticeId));
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        int sort = 0;
        for (NoticeSaveDTO.AttachmentItem a : attachments) {
            NoticeAttachment entity = new NoticeAttachment();
            entity.setNoticeId(noticeId);
            entity.setFileName(a.getFileName());
            entity.setFileUrl(a.getFileUrl());
            entity.setFileSize(a.getFileSize() == null ? 0L : a.getFileSize());
            entity.setSort(sort++);
            noticeAttachmentMapper.insert(entity);
        }
    }

    /** 批量加载门店名（storeId 为 NULL 的全局公告不参与查询） */
    private Map<Long, String> loadStoreNames(List<Notice> notices) {
        Set<Long> storeIds = notices.stream().map(Notice::getStoreId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return storeMapper.selectBatchIds(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName, (a, b) -> a));
    }

    private AdminNoticeVO toListVO(Notice n, Map<Long, String> storeNames) {
        AdminNoticeVO vo = new AdminNoticeVO();
        vo.setId(n.getId());
        vo.setStoreId(n.getStoreId());
        vo.setStoreName(n.getStoreId() == null ? "全部门店"
                : storeNames.getOrDefault(n.getStoreId(), "门店#" + n.getStoreId()));
        vo.setTitle(n.getTitle());
        vo.setType(n.getType());
        vo.setTypeDesc(typeDesc(n.getType()));
        vo.setIsImportant(n.getIsImportant());
        vo.setStatus(n.getStatus());
        vo.setStatusDesc(statusDesc(n.getStatus()));
        vo.setReadCount(n.getReadCount() == null ? 0 : n.getReadCount());
        vo.setPublishTime(n.getPublishTime());
        vo.setCreateTime(n.getCreateTime());
        vo.setUpdateTime(n.getUpdateTime());
        return vo;
    }

    private String typeDesc(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "门店规则";
            case 2: return "停业通知";
            case 3: return "活动公告";
            case 4: return "其他";
            default: return "未知";
        }
    }

    private String statusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已发布";
            case 2: return "已下架";
            default: return "未知";
        }
    }
}
