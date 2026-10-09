package com.studyroom.service.admin;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.dto.admin.NoticeSaveDTO;
import com.studyroom.entity.vo.admin.AdminNoticeVO;

/**
 * 管理端公告管理服务 —— FR-AD-015/016
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-AD-015 新建/编辑公告：标题/类型/富文本正文/重要性/附件，支持存草稿或直接发布</li>
 *   <li>FR-AD-016 公告管理列表：状态/类型筛选 + 发布/下架/删除 + 阅读量统计</li>
 * </ul>
 *
 * <p>状态机：草稿(0) ⇄ 已发布(1) ⇄ 已下架(2)；已发布可下架，下架后可重新发布（刷新发布时间）。</p>
 *
 * @author studyroom
 */
public interface AdminNoticeService {

    /**
     * 公告管理分页（FR-AD-016）
     *
     * @param type   类型：1门店规则 2停业通知 3活动公告 4其他（空=全部）
     * @param status 状态：0草稿 1已发布 2已下架（空=全部）
     */
    PageResult<AdminNoticeVO> pageNotices(Integer type, Integer status, long page, long size);

    /**
     * 公告详情（FR-AD-015 编辑回显）：含富文本正文与附件列表
     */
    AdminNoticeVO getNotice(Long noticeId);

    /**
     * 新建/编辑公告（FR-AD-015）
     *
     * <p>id 为空=新建；附件全量覆盖式保存；publishNow=true 时直接发布。</p>
     *
     * @return 公告ID
     */
    Long saveNotice(NoticeSaveDTO dto, Long operatorId);

    /**
     * 发布公告（FR-AD-016）：草稿/已下架 → 已发布，记录发布时间与发布人
     */
    void publish(Long noticeId, Long operatorId);

    /**
     * 下架公告（FR-AD-016）：已发布 → 已下架（用户端列表即刻不可见）
     */
    void offline(Long noticeId);

    /**
     * 删除公告（FR-AD-016）：逻辑删除（@TableLogic）
     */
    void deleteNotice(Long noticeId);
}
