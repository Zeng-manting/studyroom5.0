package com.studyroom.common.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

/**
 * 雪花 ID 生成工具
 * <p>封装 MyBatis-Plus 的雪花 ID，业务代码需要主动生成 ID 时使用。
 * 大多数场景下由 {@code @TableId(type = IdType.ASSIGN_ID)} 自动填充。</p>
 *
 * @author studyroom
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static long nextId() {
        return IdWorker.getId();
    }

    public static String nextIdStr() {
        return String.valueOf(IdWorker.getId());
    }
}