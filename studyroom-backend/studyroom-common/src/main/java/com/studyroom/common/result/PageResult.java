package com.studyroom.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应结构：{ list, total, page, size }
 * <p>接口规范见《技术选型与功能汇总.md》1.3 节：
 * 分页接口的 data 统一为本结构。</p>
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据列表 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    /** 当前页码（从 1 开始） */
    private Long page;

    /** 每页条数 */
    private Long size;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Long page, Long size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 由 MyBatis-Plus 的 IPage 构建分页结果
     *
     * @param page MyBatis-Plus 分页对象
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 空分页结果 */
    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(java.util.Collections.emptyList(), 0L, page, size);
    }
}
