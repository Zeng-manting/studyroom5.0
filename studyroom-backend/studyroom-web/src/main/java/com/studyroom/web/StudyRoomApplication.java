package com.studyroom.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 无人自习室管理系统 - 启动类
 *
 * <p>三层架构（业务层）：Spring Boot 2.7.18 + JDK 11，
 * 数据层 MySQL 8 + Redis 6，统一 JWT 无状态鉴权。</p>
 *
 * <p>{@link EnableScheduling}：FR-SY-002 后台调度
 * （支付超时取消 FR-UC-025、未签到取消 FR-UC-026、暂停超时结算 FR-UC-033）。</p>
 *
 * <p>{@link EnableAsync}：邮件通知（FR-UC-004/046）等异步任务走线程池，
 * SMTP 抖动不阻塞主流程。</p>
 *
 * <p>组件扫描范围覆盖 com.studyroom 全部子包：
 * common（工具/切面/属性）、service（业务实现）、web（Controller/配置）。
 * Mapper 接口扫描见 {@code MybatisPlusConfig#@MapperScan}。</p>
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.studyroom")
public class StudyRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyRoomApplication.class, args);
    }
}
