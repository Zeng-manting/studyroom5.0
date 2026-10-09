package com.studyroom.service.notice;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.vo.notice.NoticeDetailVO;
import com.studyroom.entity.vo.notice.NoticeListVO;

/**
 * 公告服务接口
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-UC-041 公告分类：全部/门店规则/停业通知/活动公告</li>
 *   <li>FR-UC-042 公告列表：已读/未读标识 + 重要公告弹窗</li>
 *   <li>FR-UC-043 公告详情：富文本正文 + 附件下载</li>
 * </ul>
 */
public interface NoticeService {

    /**
     * 已发布公告分页（FR-UC-041/042）
     *
     * @param userId 用户ID（计算已读标识）
     * @param type   类型筛选：0/null=全部，1门店规则 2停业通知 3活动公告 4其他
     */
    PageResult<NoticeListVO> listNotices(Long userId, Integer type, long page, long size);

    /**
     * 公告详情（FR-UC-043）
     * <p>首次访问落 notice_read 已读记录 + read_count 自增（幂等：重复访问不重复计数）。</p>
     */
    NoticeDetailVO getNoticeDetail(Long userId, Long noticeId);

    /**
     * 重要公告弹窗（FR-UC-042）
     * <p>返回最新一条"重要且未读"的已发布公告；无则返回 null。</p>
     */
    NoticeDetailVO getImportantPopup(Long userId);
}
