package com.studyroom.web.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.login.AuthContextHolder;
import com.studyroom.common.login.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充器
 *
 * <h3>填充规则</h3>
 * <ul>
 *   <li>{@code insert}: createTime / updateTime = now；createBy / updateBy = 当前登录用户 ID（未登录填 0）</li>
 *   <li>{@code update}: updateTime = now；updateBy = 当前登录用户 ID</li>
 *   <li>字段未在实体中声明则跳过填充，零侵入</li>
 * </ul>
 *
 * <h3>实体字段建议</h3>
 * <pre>
 * &#64;TableField(fill = FieldFill.INSERT)
 * private LocalDateTime createTime;
 *
 * &#64;TableField(fill = FieldFill.INSERT_UPDATE)
 * private LocalDateTime updateTime;
 *
 * &#64;TableField(fill = FieldFill.INSERT)
 * private Long createBy;
 *
 * &#64;TableField(fill = FieldFill.INSERT_UPDATE)
 * private Long updateBy;
 * </pre>
 *
 * @author studyroom
 */
@Slf4j
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("[MybatisMetaObjectHandler] 插入填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());
        LocalDateTime now = LocalDateTime.now();
        Long uid = currentUid();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createBy", Long.class, uid);
        strictInsertFill(metaObject, "updateBy", Long.class, uid);
        // 逻辑删除默认值
        strictInsertFill(metaObject, "deleted", Integer.class, CommonConstants.NOT_DELETED);
        // 版本号（乐观锁）
        strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("[MybatisMetaObjectHandler] 更新填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updateBy", Long.class, currentUid());
    }

    private Long currentUid() {
        LoginUser u = AuthContextHolder.get();
        return u == null || u.getId() == null ? 0L : u.getId();
    }
}