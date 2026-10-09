package com.studyroom.web.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * <p>技术选型 1.3：MyBatis-Plus 3.5.5，启用分页插件、乐观锁插件、逻辑删除（全局配置于 application.yml）。</p>
 */
@Configuration
@MapperScan("com.studyroom.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：管理端列表分页（FR-AD-010 等，默认 20 条/页）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页上限，防止恶意大分页拖垮数据库
        paginationInterceptor.setMaxLimit(100L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        // 乐观锁插件：订单状态流转、座位状态变更等并发场景（配合 @Version 字段）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
