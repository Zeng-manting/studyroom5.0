-- ============================================================
-- 基于 Spring Boot 的无人自习室管理系统 - 数据库建表脚本
-- 数据库：MySQL 8.0（InnoDB / utf8mb4）
-- 设计依据：《技术选型与功能汇总.md》全部 FR 编号
-- 命名约定：表名 snake_case + 下划线；字段 snake_case；
--          id 统一 BIGINT（雪花算法，Java 端由 MyBatis-Plus assign_id 填充）；
--          create_time / update_time / deleted / version 全表统一。
-- ============================================================

-- ----------------------------------------------------------------
-- 0. 创建数据库
-- ----------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `studyroom`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `studyroom`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ===============================================================
-- 一、用户域（FR-UC-001 ~ 006 / FR-UC-053 / FR-UC-054）
-- ===============================================================

-- 1.1 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`                  BIGINT          NOT NULL                    COMMENT '雪花ID',
    `openid`              VARCHAR(64)     NOT NULL                    COMMENT '微信openid，FR-UC-001 静默登录唯一标识',
    `unionid`             VARCHAR(64)     DEFAULT NULL                COMMENT '微信unionid（多公众号/小程序统一身份）',
    `nickname`            VARCHAR(64)     DEFAULT '自习用户'           COMMENT '昵称，FR-UC-002 默认昵称',
    `avatar`              VARCHAR(512)    DEFAULT NULL                COMMENT '头像URL，FR-UC-003 用户信息授权',
    `email_encrypted`     VARBINARY(255)  DEFAULT NULL                COMMENT '邮箱 AES-256-CBC 密文，FR-UC-004 加密存储',
    `phone_encrypted`     VARBINARY(255)  DEFAULT NULL                COMMENT '手机号 AES 密文（预留）',
    `gender`              TINYINT         DEFAULT 0                   COMMENT '0未知 1男 2女',
    `balance`             DECIMAL(10,2)   DEFAULT 0.00                COMMENT '钱包余额（分单位精度），FR-UC-023/048',
    `frozen_balance`      DECIMAL(10,2)   DEFAULT 0.00                COMMENT '冻结余额（下单预扣）',
    `credit_score`        INT             DEFAULT 100                 COMMENT '信用分',
    `credit_status`       TINYINT         DEFAULT 0                   COMMENT '信用状态：0正常 1限制中，FR-UC-054',
    `credit_limit_end`    DATETIME        DEFAULT NULL                COMMENT '信用限制结束时间，FR-UC-054 到期自动恢复',
    `no_show_count_30d`   INT             DEFAULT 0                   COMMENT '30天内爽约次数，FR-UC-053/054',
    `no_show_records`     JSON            DEFAULT NULL                COMMENT '爽约明细（最近N条）',
    `status`              TINYINT         DEFAULT 1                   COMMENT '账号状态：0禁用 1正常',
    `last_login_time`     DATETIME        DEFAULT NULL                COMMENT '最近登录时间',
    `last_login_ip`       VARCHAR(64)     DEFAULT NULL                COMMENT '最近登录IP',
    `privacy_agreed`      TINYINT         DEFAULT 0                   COMMENT '是否已同意隐私协议，FR-UC-050',
    `privacy_agree_time`  DATETIME        DEFAULT NULL                COMMENT '协议同意时间',
    `version`             INT             DEFAULT 0                   COMMENT '乐观锁',
    `deleted`             TINYINT         DEFAULT 0                   COMMENT '逻辑删除：0否 1是',
    `create_time`         DATETIME        DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_phone` (`phone_encrypted`(32)),
    KEY `idx_status_credit` (`status`, `credit_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 1.2 信用/爽约记录
DROP TABLE IF EXISTS `user_credit_log`;
CREATE TABLE `user_credit_log` (
    `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '用户ID',
    `order_id`     BIGINT       NOT NULL                COMMENT '关联订单ID',
    `type`         TINYINT      NOT NULL                COMMENT '类型：1爽约扣分 2恢复信用 3限制预约 4解除限制',
    `score_delta`  INT          DEFAULT 0               COMMENT '信用分变化',
    `reason`       VARCHAR(255) DEFAULT NULL            COMMENT '原因说明',
    `operator_id`  BIGINT       DEFAULT NULL            COMMENT '操作人（管理员/系统），系统为 NULL',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用变更流水（FR-UC-053/054）';

-- 1.3 余额流水
DROP TABLE IF EXISTS `user_balance_log`;
CREATE TABLE `user_balance_log` (
    `id`              BIGINT        NOT NULL                COMMENT '雪花ID',
    `user_id`         BIGINT        NOT NULL                COMMENT '用户ID',
    `type`            TINYINT       NOT NULL                COMMENT '1充值 2消费 3退款 4冻结 5解冻 6管理员调整',
    `amount`          DECIMAL(10,2) NOT NULL                COMMENT '变动金额（正负）',
    `balance_before`  DECIMAL(10,2) NOT NULL                COMMENT '变动前余额',
    `balance_after`   DECIMAL(10,2) NOT NULL                COMMENT '变动后余额',
    `biz_type`        VARCHAR(32)   DEFAULT NULL            COMMENT '业务类型：order/refund/recharge',
    `biz_id`          BIGINT        DEFAULT NULL            COMMENT '业务ID',
    `remark`          VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='余额流水（FR-UC-048）';

-- 1.4 反馈意见
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
    `id`          BIGINT        NOT NULL                COMMENT '雪花ID',
    `user_id`     BIGINT        DEFAULT NULL            COMMENT '用户ID（匿名反馈时为NULL）',
    `type`        TINYINT       DEFAULT 1               COMMENT '1功能建议 2问题反馈 3投诉 4其他',
    `content`     VARCHAR(1000) NOT NULL                COMMENT '反馈内容',
    `images`      JSON          DEFAULT NULL            COMMENT '图片URL数组',
    `contact`     VARCHAR(128)  DEFAULT NULL            COMMENT '联系方式（手机/邮箱/微信号）',
    `status`      TINYINT       DEFAULT 0               COMMENT '0待处理 1已处理 2已关闭',
    `reply`       VARCHAR(500)  DEFAULT NULL            COMMENT '管理员回复',
    `handler_id`  BIGINT        DEFAULT NULL            COMMENT '处理人',
    `handle_time` DATETIME      DEFAULT NULL            COMMENT '处理时间',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意见反馈（FR-UC-052）';

-- 1.5 订阅消息授权
DROP TABLE IF EXISTS `user_subscribe_message`;
CREATE TABLE `user_subscribe_message` (
    `id`           BIGINT     NOT NULL                COMMENT '雪花ID',
    `user_id`      BIGINT     NOT NULL                COMMENT '用户ID',
    `template_id`  VARCHAR(64) NOT NULL               COMMENT '微信订阅消息模板ID',
    `scene`        VARCHAR(32) NOT NULL               COMMENT '业务场景：orderSuccess/startSoon/endSoon/cancel/refund/timeout',
    `accept_flag`  TINYINT    DEFAULT 0               COMMENT '0未授权 1已授权 2已拒绝',
    `accept_time`  DATETIME   DEFAULT NULL            COMMENT '最近授权时间',
    `create_time`  DATETIME   DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_scene` (`user_id`, `scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅消息授权（FR-UC-044）';

-- 1.6 消息中心
DROP TABLE IF EXISTS `user_message`;
CREATE TABLE `user_message` (
    `id`         BIGINT       NOT NULL                COMMENT '雪花ID',
    `user_id`    BIGINT       NOT NULL                COMMENT '用户ID',
    `type`       TINYINT      NOT NULL                COMMENT '1系统通知 2订单消息 3活动消息',
    `title`      VARCHAR(128) NOT NULL                COMMENT '标题',
    `content`    VARCHAR(500) NOT NULL                COMMENT '内容',
    `biz_type`   VARCHAR(32)  DEFAULT NULL            COMMENT '业务类型',
    `biz_id`     BIGINT       DEFAULT NULL            COMMENT '业务ID',
    `is_read`    TINYINT      DEFAULT 0               COMMENT '0未读 1已读',
    `read_time`  DATETIME     DEFAULT NULL            COMMENT '已读时间',
    `deleted`    TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    `create_time`DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read_time` (`user_id`, `is_read`, `create_time`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息中心（FR-UC-047）';

-- ===============================================================
-- 二、门店与座位域（FR-UC-007 ~ 011 / FR-AD-004 ~ 008 / FR-UC-017 / FR-UC-020）
-- ===============================================================

-- 2.1 门店（单店固定，但仍保留多店扩展）
DROP TABLE IF EXISTS `store`;
CREATE TABLE `store` (
    `id`             BIGINT        NOT NULL                COMMENT '雪花ID',
    `name`           VARCHAR(64)   NOT NULL                COMMENT '门店名称',
    `address`        VARCHAR(255)  NOT NULL                COMMENT '门店地址',
    `phone`          VARCHAR(32)   DEFAULT NULL            COMMENT '联系电话',
    `business_hours` VARCHAR(64)   DEFAULT '08:00-23:00'   COMMENT '营业时间，如 08:00-23:00',
    `latitude`       DECIMAL(10,6) DEFAULT NULL            COMMENT 'GPS纬度',
    `longitude`      DECIMAL(10,6) DEFAULT NULL            COMMENT 'GPS经度',
    `description`    TEXT          DEFAULT NULL            COMMENT '门店简介',
    `facilities`     JSON          DEFAULT NULL            COMMENT '设施标签JSON数组',
    `images`         JSON          DEFAULT NULL            COMMENT '环境图片JSON数组',
    `rating`         DECIMAL(3,1)  DEFAULT 5.0             COMMENT '综合评分',
    `status`         TINYINT       DEFAULT 1               COMMENT '0休息中 1营业中 2维护中，FR-UC-008',
    `sort`           INT           DEFAULT 0               COMMENT '排序',
    `deleted`        TINYINT       DEFAULT 0,
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店表（FR-UC-007~011 / FR-AD-004）';

-- 2.2 首页轮播
DROP TABLE IF EXISTS `store_banner`;
CREATE TABLE `store_banner` (
    `id`         BIGINT        NOT NULL                COMMENT '雪花ID',
    `store_id`   BIGINT        NOT NULL                COMMENT '门店ID',
    `image_url`  VARCHAR(512)  NOT NULL                COMMENT '图片URL',
    `link_type`  VARCHAR(32)   DEFAULT NULL            COMMENT '跳转类型：notice/web/miniprogram',
    `link_value` VARCHAR(255)  DEFAULT NULL            COMMENT '跳转值',
    `sort`       INT           DEFAULT 0               COMMENT '排序',
    `status`     TINYINT       DEFAULT 1               COMMENT '0下架 1上架',
    `create_time`DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_store_status` (`store_id`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页Banner（FR-UC-009）';

-- 2.3 时段黑名单
DROP TABLE IF EXISTS `store_blacklist`;
CREATE TABLE `store_blacklist` (
    `id`          BIGINT      NOT NULL                COMMENT '雪花ID',
    `store_id`    BIGINT      NOT NULL                COMMENT '门店ID',
    `type`        TINYINT     NOT NULL                COMMENT '1周期性 2单次',
    `week_days`   VARCHAR(32) DEFAULT NULL            COMMENT '周期性：周几，逗号分隔，1=周一 7=周日',
    `single_date` DATE        DEFAULT NULL            COMMENT '单次：具体日期',
    `start_time`  TIME        DEFAULT NULL            COMMENT '开始时间',
    `end_time`    TIME        DEFAULT NULL            COMMENT '结束时间',
    `reason`      VARCHAR(255) DEFAULT NULL           COMMENT '原因（停业维护等）',
    `status`      TINYINT     DEFAULT 1               COMMENT '0禁用 1启用',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_store_type_date` (`store_id`, `type`, `single_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='时段黑名单（FR-UC-020 / FR-AD-005）';

-- 2.4 座位类型
DROP TABLE IF EXISTS `seat_type`;
CREATE TABLE `seat_type` (
    `id`          BIGINT       NOT NULL                COMMENT '雪花ID',
    `name`        VARCHAR(32)  NOT NULL                COMMENT '类型名称：普通区/静音区/包间/VIP',
    `code`        VARCHAR(32)  NOT NULL                COMMENT '类型编码：NORMAL/SILENT/ROOM/VIP',
    `description` VARCHAR(255) DEFAULT NULL            COMMENT '描述',
    `icon`        VARCHAR(255) DEFAULT NULL            COMMENT '图标',
    `sort`        INT          DEFAULT 0               COMMENT '排序',
    `status`      TINYINT      DEFAULT 1               COMMENT '0下架 1上架',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位类型';

-- 2.5 座位
DROP TABLE IF EXISTS `seat`;
CREATE TABLE `seat` (
    `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
    `store_id`     BIGINT       NOT NULL                COMMENT '门店ID',
    `seat_type_id` BIGINT       NOT NULL                COMMENT '座位类型ID',
    `seat_no`      VARCHAR(32)  NOT NULL                COMMENT '座位编号（如 A-12）',
    `area_code`    VARCHAR(32)  DEFAULT NULL            COMMENT '区域编码：window/corner/center',
    `x_coord`      INT          DEFAULT 0               COMMENT '画布X坐标',
    `y_coord`      INT          DEFAULT 0               COMMENT '画布Y坐标',
    `floor`        VARCHAR(16)  DEFAULT '1F'            COMMENT '楼层',
    `description`  VARCHAR(255) DEFAULT NULL            COMMENT '备注',
    `status`       TINYINT      DEFAULT 0               COMMENT '0空闲 1使用中 2维护 3打扫中 4锁定，FR-UC-017 六色状态',
    `version`      INT          DEFAULT 0               COMMENT '乐观锁，FR-UC-018 锁座并发',
    `deleted`      TINYINT      DEFAULT 0,
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_seat_no` (`store_id`, `seat_no`),
    KEY `idx_store_type_status` (`store_id`, `seat_type_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位表（FR-UC-017 / FR-AD-006/007）';

-- ===============================================================
-- 三、价格与套餐（FR-UC-019 / FR-AD-025）
-- ===============================================================

-- 3.1 套餐
DROP TABLE IF EXISTS `package`;
CREATE TABLE `package` (
    `id`             BIGINT        NOT NULL                COMMENT '雪花ID',
    `name`           VARCHAR(64)   NOT NULL                COMMENT '套餐名称',
    `type`           TINYINT       NOT NULL                COMMENT '1按小时 2日卡 3周卡 4月卡',
    `duration_value` INT           NOT NULL                COMMENT '时长值：小时/天/周/月',
    `unit`           VARCHAR(16)   NOT NULL                COMMENT '单位：hour/day/week/month',
    `price`          DECIMAL(10,2) NOT NULL                COMMENT '实际售价',
    `original_price` DECIMAL(10,2) DEFAULT NULL            COMMENT '划线原价',
    `description`    VARCHAR(255)  DEFAULT NULL            COMMENT '描述',
    `sort`           INT           DEFAULT 0               COMMENT '排序',
    `status`         TINYINT       DEFAULT 1               COMMENT '0下架 1上架',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐表（FR-UC-019 / FR-AD-025）';

-- 3.2 价格配置（座位类型 × 时段加价规则）
DROP TABLE IF EXISTS `price_rule`;
CREATE TABLE `price_rule` (
    `id`              BIGINT        NOT NULL                COMMENT '雪花ID',
    `seat_type_id`    BIGINT        DEFAULT NULL            COMMENT '座位类型ID（NULL=全部）',
    `package_id`      BIGINT        DEFAULT NULL            COMMENT '套餐ID（NULL=全部）',
    `time_slot_start` TIME          DEFAULT NULL            COMMENT '时段起点（如 18:00）',
    `time_slot_end`   TIME          DEFAULT NULL            COMMENT '时段终点（如 22:00）',
    `extra_price`     DECIMAL(10,2) DEFAULT 0.00            COMMENT '加价金额',
    `extra_ratio`     DECIMAL(3,2)  DEFAULT 1.00            COMMENT '加价倍数',
    `priority`        INT           DEFAULT 0               COMMENT '优先级（数字越大越优先）',
    `status`          TINYINT       DEFAULT 1               COMMENT '0禁用 1启用',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_pkg_time` (`seat_type_id`, `package_id`, `time_slot_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格加价规则（FR-AD-025）';

-- ===============================================================
-- 四、订单域（FR-UC-015 ~ 038 / FR-UC-053 ~ 055 / FR-AD-010 ~ 013）
-- ===============================================================

-- 4.1 订单主表
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`                     BIGINT        NOT NULL                COMMENT '雪花ID',
    `order_no`               VARCHAR(32)   NOT NULL                COMMENT '业务订单号（年月日+6位序号）',
    `user_id`                BIGINT        NOT NULL                COMMENT '用户ID',
    `store_id`               BIGINT        NOT NULL                COMMENT '门店ID',
    `seat_id`                BIGINT        NOT NULL                COMMENT '座位ID',
    `seat_type_id`           BIGINT        NOT NULL                COMMENT '座位类型ID（冗余便于统计）',
    `package_id`             BIGINT        NOT NULL                COMMENT '套餐ID',
    `start_time`             DATETIME      NOT NULL                COMMENT '预约开始时间',
    `end_time`               DATETIME      NOT NULL                COMMENT '预约结束时间',
    `original_price`         DECIMAL(10,2) NOT NULL                COMMENT '原价',
    `discount_amount`        DECIMAL(10,2) DEFAULT 0.00            COMMENT '优惠金额',
    `coupon_id`              BIGINT        DEFAULT NULL            COMMENT '优惠券ID，FR-UC-024',
    `coupon_amount`          DECIMAL(10,2) DEFAULT 0.00            COMMENT '券抵扣金额',
    `pay_amount`             DECIMAL(10,2) NOT NULL                COMMENT '实付金额',
    `status`                 TINYINT       NOT NULL                COMMENT '0待支付 1已预约 2进行中 3已完成 4已取消 5退款中 6已退款 7申诉中',
    `pay_type`               TINYINT       DEFAULT NULL            COMMENT '1余额 2微信 3混合',
    `pay_time`               DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `cancel_time`            DATETIME      DEFAULT NULL            COMMENT '取消时间',
    `cancel_reason`          VARCHAR(255)  DEFAULT NULL            COMMENT '取消原因',
    `sign_time`              DATETIME      DEFAULT NULL            COMMENT '签到时间，FR-UC-030',
    `sign_latitude`          DECIMAL(10,6) DEFAULT NULL            COMMENT '签到纬度，FR-UC-030 GPS',
    `sign_longitude`         DECIMAL(10,6) DEFAULT NULL            COMMENT '签到经度',
    `sign_gps_distance`      INT           DEFAULT NULL            COMMENT 'GPS距离门店米数',
    `pause_start_time`       DATETIME      DEFAULT NULL            COMMENT '最近一次暂停开始时间',
    `pause_end_time`         DATETIME      DEFAULT NULL            COMMENT '暂停预计恢复时间，FR-UC-033 默认+30min',
    `pause_count`            INT           DEFAULT 0               COMMENT '累计暂停次数',
    `actual_end_time`        DATETIME      DEFAULT NULL            COMMENT '实际结束时间，FR-UC-034',
    `actual_duration_minutes`INT           DEFAULT NULL            COMMENT '实际使用分钟数',
    `is_no_show`             TINYINT       DEFAULT 0               COMMENT '是否爽约：0否 1是，FR-UC-026',
    `appeal_status`          TINYINT       DEFAULT 0               COMMENT '0无 1待审 2通过 3驳回',
    `refund_amount`          DECIMAL(10,2) DEFAULT NULL            COMMENT '退款金额，FR-UC-027',
    `refund_time`            DATETIME      DEFAULT NULL            COMMENT '退款时间',
    `refund_reason`          VARCHAR(255)  DEFAULT NULL            COMMENT '退款原因',
    `extend_count`           INT           DEFAULT 0               COMMENT '续时次数，FR-UC-055',
    `idempotent_key`         VARCHAR(64)   DEFAULT NULL            COMMENT '接口幂等键，FR-SY-001',
    `remark`                 VARCHAR(255)  DEFAULT NULL            COMMENT '用户备注',
    `version`                INT           DEFAULT 0               COMMENT '乐观锁',
    `deleted`                TINYINT       DEFAULT 0,
    `create_time`            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_idempotent` (`idempotent_key`),
    KEY `idx_user_status` (`user_id`, `status`, `create_time`),
    KEY `idx_seat_time` (`seat_id`, `start_time`, `end_time`),
    KEY `idx_status_start` (`status`, `start_time`),
    KEY `idx_no_show` (`is_no_show`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表（FR-UC-015~038）';

-- 4.2 支付记录
DROP TABLE IF EXISTS `order_payment`;
CREATE TABLE `order_payment` (
    `id`             BIGINT        NOT NULL                COMMENT '雪花ID',
    `order_id`       BIGINT        NOT NULL                COMMENT '订单ID',
    `order_no`       VARCHAR(32)   NOT NULL                COMMENT '订单号（冗余）',
    `pay_type`       TINYINT       NOT NULL                COMMENT '1余额 2微信',
    `pay_amount`     DECIMAL(10,2) NOT NULL                COMMENT '支付金额',
    `transaction_id` VARCHAR(64)   DEFAULT NULL            COMMENT '微信交易号',
    `pay_status`     TINYINT       DEFAULT 1               COMMENT '0失败 1成功 2已退款',
    `pay_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `remark`         VARCHAR(255)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单支付记录';

-- 4.3 退款记录
DROP TABLE IF EXISTS `order_refund`;
CREATE TABLE `order_refund` (
    `id`              BIGINT        NOT NULL                COMMENT '雪花ID',
    `order_id`        BIGINT        NOT NULL                COMMENT '订单ID',
    `refund_amount`   DECIMAL(10,2) NOT NULL                COMMENT '退款金额',
    `refund_type`     TINYINT       NOT NULL                COMMENT '1阶梯退款 2未签到退款 3申诉通过 4管理手动',
    `step_type`       VARCHAR(16)   DEFAULT NULL            COMMENT '阶梯类型：GT_60/FIFTEEN_TO_60/LT_15，FR-UC-027',
    `ratio`           DECIMAL(3,2)  DEFAULT NULL            COMMENT '退款比例',
    `refund_reason`   VARCHAR(255)  DEFAULT NULL            COMMENT '退款原因',
    `operator_id`     BIGINT        DEFAULT NULL            COMMENT '操作人（系统为NULL）',
    `operator_remark` VARCHAR(255)  DEFAULT NULL            COMMENT '操作备注',
    `status`          TINYINT       DEFAULT 1               COMMENT '0失败 1成功 2处理中',
    `refund_time`     DATETIME      DEFAULT NULL            COMMENT '退款成功时间',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录（FR-UC-027 / FR-AD-012）';

-- 4.4 续时记录
DROP TABLE IF EXISTS `order_extension`;
CREATE TABLE `order_extension` (
    `id`           BIGINT        NOT NULL                COMMENT '雪花ID',
    `order_id`     BIGINT        NOT NULL                COMMENT '订单ID',
    `hours`        INT           NOT NULL                COMMENT '续时时长（小时）',
    `amount`       DECIMAL(10,2) NOT NULL                COMMENT '续时金额',
    `pay_type`     TINYINT       DEFAULT NULL            COMMENT '支付方式：1余额 2微信',
    `pay_status`   TINYINT       DEFAULT 0               COMMENT '0待支付 1已支付 2已退款',
    `pay_time`     DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `new_end_time` DATETIME      NOT NULL                COMMENT '续时后结束时间',
    `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单续时记录（FR-UC-055）';

-- 4.5 签到记录
DROP TABLE IF EXISTS `order_sign`;
CREATE TABLE `order_sign` (
    `id`               BIGINT        NOT NULL                COMMENT '雪花ID',
    `order_id`         BIGINT        NOT NULL                COMMENT '订单ID',
    `qrcode_plain`     VARCHAR(128)  DEFAULT NULL            COMMENT '扫码内容明文（仅签名比对，不存敏感字段）',
    `qrcode_encrypted` VARBINARY(512) DEFAULT NULL           COMMENT 'AES-256-CBC 加密内容，FR-UC-030 ①',
    `qrcode_sign`      VARCHAR(128)  DEFAULT NULL            COMMENT 'HMAC-SHA256 签名，FR-UC-030 ②',
    `sign_time`        DATETIME      NOT NULL                COMMENT '签到时间',
    `sign_latitude`    DECIMAL(10,6) DEFAULT NULL            COMMENT '签到纬度',
    `sign_longitude`   DECIMAL(10,6) DEFAULT NULL            COMMENT '签到经度',
    `gps_distance`     INT           DEFAULT NULL            COMMENT 'GPS距离门店米数',
    `check_result`     TINYINT       NOT NULL                COMMENT '五重校验结果：0失败 1成功',
    `fail_reason`      VARCHAR(255)  DEFAULT NULL            COMMENT '失败原因',
    `user_agent`       VARCHAR(255)  DEFAULT NULL            COMMENT '客户端UA',
    `client_ip`        VARCHAR(64)   DEFAULT NULL            COMMENT '客户端IP',
    PRIMARY KEY (`id`),
    KEY `idx_order_time` (`order_id`, `sign_time`),
    KEY `idx_result` (`check_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到记录（FR-UC-029/030/031）';

-- 4.6 签到申诉
DROP TABLE IF EXISTS `order_appeal`;
CREATE TABLE `order_appeal` (
    `id`            BIGINT        NOT NULL                COMMENT '雪花ID',
    `order_id`      BIGINT        NOT NULL                COMMENT '订单ID',
    `user_id`       BIGINT        NOT NULL                COMMENT '申诉人',
    `reason_type`   VARCHAR(32)   DEFAULT NULL            COMMENT '申诉原因枚举',
    `description`   VARCHAR(500)  NOT NULL                COMMENT '申诉说明',
    `images`        JSON          DEFAULT NULL            COMMENT '凭证图片',
    `status`        TINYINT       DEFAULT 0               COMMENT '0待审 1通过 2驳回，FR-AD-011',
    `handler_id`    BIGINT        DEFAULT NULL            COMMENT '处理人',
    `handle_remark` VARCHAR(255)  DEFAULT NULL            COMMENT '处理意见',
    `handle_time`   DATETIME      DEFAULT NULL            COMMENT '处理时间',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到申诉（FR-UC-032 / FR-AD-011）';

-- 4.7 暂停记录
DROP TABLE IF EXISTS `order_pause`;
CREATE TABLE `order_pause` (
    `id`           BIGINT     NOT NULL                COMMENT '雪花ID',
    `order_id`     BIGINT     NOT NULL                COMMENT '订单ID',
    `pause_start`  DATETIME   NOT NULL                COMMENT '暂停开始',
    `pause_end`    DATETIME   DEFAULT NULL            COMMENT '暂停结束（恢复时填）',
    `status`       TINYINT    DEFAULT 0               COMMENT '0暂停中 1已恢复 2超时自动结算，FR-UC-033',
    `auto_end`     TINYINT    DEFAULT 0               COMMENT '是否系统自动结束：0否 1是',
    `create_time`  DATETIME   DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_status` (`order_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单暂停记录（FR-UC-033）';

-- ===============================================================
-- 五、评价域（FR-UC-039 / 040 / FR-AD-017 / 018）
-- ===============================================================

-- 5.1 评价主表
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id`         BIGINT       NOT NULL                COMMENT '雪花ID',
    `order_id`   BIGINT       NOT NULL                COMMENT '订单ID（一单一评）',
    `user_id`    BIGINT       NOT NULL                COMMENT '用户ID',
    `store_id`   BIGINT       NOT NULL                COMMENT '门店ID',
    `seat_id`    BIGINT       DEFAULT NULL            COMMENT '座位ID（可空，按门店维度）',
    `score`      TINYINT      NOT NULL                COMMENT '评分1-5',
    `tags`       JSON         DEFAULT NULL            COMMENT '预设标签JSON数组',
    `content`    VARCHAR(500) DEFAULT NULL            COMMENT '评价内容',
    `anonymous`  TINYINT      DEFAULT 0               COMMENT '0不匿名 1匿名',
    `status`     TINYINT      DEFAULT 1               COMMENT '0隐藏 1显示，FR-AD-018',
    `deleted`    TINYINT      DEFAULT 0,
    `create_time`DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_store_score_time` (`store_id`, `score`, `create_time`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表（FR-UC-039/040）';

-- 5.2 评价图片
DROP TABLE IF EXISTS `review_image`;
CREATE TABLE `review_image` (
    `id`         BIGINT       NOT NULL                COMMENT '雪花ID',
    `review_id`  BIGINT       NOT NULL                COMMENT '评价ID',
    `image_url`  VARCHAR(512) NOT NULL                COMMENT '图片URL',
    `sort`       INT          DEFAULT 0               COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_review_sort` (`review_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价图片';

-- 5.3 评价回复
DROP TABLE IF EXISTS `review_reply`;
CREATE TABLE `review_reply` (
    `id`         BIGINT       NOT NULL                COMMENT '雪花ID',
    `review_id`  BIGINT       NOT NULL                COMMENT '评价ID',
    `content`    VARCHAR(200) NOT NULL                COMMENT '回复内容',
    `admin_id`   BIGINT       NOT NULL                COMMENT '回复人',
    `create_time`DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价回复（FR-AD-018）';

-- ===============================================================
-- 六、公告域（FR-UC-041 ~ 043 / FR-AD-015 / 016）
-- ===============================================================

-- 6.1 公告主表
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
    `store_id`     BIGINT       DEFAULT NULL            COMMENT '门店ID（NULL=全局）',
    `title`        VARCHAR(128) NOT NULL                COMMENT '标题',
    `type`         TINYINT      DEFAULT 1               COMMENT '1门店规则 2停业通知 3活动公告 4其他',
    `content`      LONGTEXT     NOT NULL                COMMENT '富文本正文',
    `is_important` TINYINT      DEFAULT 0               COMMENT '是否重要公告',
    `status`       TINYINT      DEFAULT 0               COMMENT '0草稿 1已发布 2已下架',
    `publish_time` DATETIME     DEFAULT NULL            COMMENT '发布时间',
    `publisher_id` BIGINT       DEFAULT NULL            COMMENT '发布人',
    `read_count`   INT          DEFAULT 0               COMMENT '阅读量统计',
    `deleted`      TINYINT      DEFAULT 0,
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `is_important`, `publish_time`),
    KEY `idx_store_type` (`store_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表（FR-UC-041~043 / FR-AD-015/016）';

-- 6.2 公告附件
DROP TABLE IF EXISTS `notice_attachment`;
CREATE TABLE `notice_attachment` (
    `id`         BIGINT       NOT NULL                COMMENT '雪花ID',
    `notice_id`  BIGINT       NOT NULL                COMMENT '公告ID',
    `file_name`  VARCHAR(128) NOT NULL                COMMENT '文件名',
    `file_url`   VARCHAR(512) NOT NULL                COMMENT '文件URL',
    `file_size`  BIGINT       DEFAULT 0               COMMENT '文件大小（字节）',
    `sort`       INT          DEFAULT 0               COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_notice` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告附件';

-- 6.3 公告已读
DROP TABLE IF EXISTS `notice_read`;
CREATE TABLE `notice_read` (
    `id`         BIGINT   NOT NULL                COMMENT '雪花ID',
    `user_id`    BIGINT   NOT NULL                COMMENT '用户ID',
    `notice_id`  BIGINT   NOT NULL                COMMENT '公告ID',
    `read_time`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_notice` (`user_id`, `notice_id`),
    KEY `idx_notice` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录（FR-UC-042）';

-- ===============================================================
-- 七、优惠券域（FR-UC-024 / FR-AD-027）
-- ===============================================================

-- 7.1 优惠券
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
    `id`              BIGINT        NOT NULL                COMMENT '雪花ID',
    `name`            VARCHAR(64)   NOT NULL                COMMENT '券名称',
    `type`            TINYINT       NOT NULL                COMMENT '1新人券 2时长抵扣券 3满减券',
    `discount_type`   TINYINT       NOT NULL                COMMENT '1定额抵扣 2折扣 3满减',
    `threshold_amount`DECIMAL(10,2) DEFAULT 0.00            COMMENT '满减门槛',
    `discount_amount` DECIMAL(10,2) DEFAULT 0.00            COMMENT '定额抵扣金额',
    `discount_ratio`  DECIMAL(3,2)  DEFAULT 1.00            COMMENT '折扣比例（如0.85）',
    `valid_days`      INT           DEFAULT 30               COMMENT '领取后有效天数',
    `valid_start`     DATETIME      DEFAULT NULL            COMMENT '固定起期',
    `valid_end`       DATETIME      DEFAULT NULL            COMMENT '固定止期',
    `total_quota`     INT           DEFAULT 0               COMMENT '总发行量（0不限）',
    `used_count`      INT           DEFAULT 0               COMMENT '已使用量',
    `claim_count`     INT           DEFAULT 0               COMMENT '已领取量',
    `per_user_limit`  INT           DEFAULT 1               COMMENT '每人限领',
    `description`     VARCHAR(255)  DEFAULT NULL            COMMENT '使用说明',
    `status`          TINYINT       DEFAULT 1               COMMENT '0下架 1上架',
    `creator_id`      BIGINT        DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `valid_start`, `valid_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券（FR-UC-024 / FR-AD-027）';

-- 7.2 用户领取
DROP TABLE IF EXISTS `coupon_claim`;
CREATE TABLE `coupon_claim` (
    `id`         BIGINT     NOT NULL                COMMENT '雪花ID',
    `coupon_id`  BIGINT     NOT NULL                COMMENT '券ID',
    `user_id`    BIGINT     NOT NULL                COMMENT '用户ID',
    `status`     TINYINT    DEFAULT 0               COMMENT '0未使用 1已使用 2已过期',
    `claim_time` DATETIME   DEFAULT CURRENT_TIMESTAMP,
    `use_time`   DATETIME   DEFAULT NULL            COMMENT '使用时间',
    `order_id`   BIGINT     DEFAULT NULL            COMMENT '使用订单',
    `expire_time`DATETIME   NOT NULL                COMMENT '过期时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status_expire` (`user_id`, `status`, `expire_time`),
    KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户领取记录';

-- ===============================================================
-- 八、财务流水（FR-AD-024）
-- ===============================================================

DROP TABLE IF EXISTS `finance_record`;
CREATE TABLE `finance_record` (
    `id`              BIGINT        NOT NULL                COMMENT '雪花ID',
    `type`            TINYINT       NOT NULL                COMMENT '1收入 2退款',
    `biz_type`        VARCHAR(32)   NOT NULL                COMMENT '业务类型：order/extend/refund',
    `biz_id`          BIGINT        NOT NULL                COMMENT '业务ID',
    `order_no`        VARCHAR(32)   DEFAULT NULL            COMMENT '订单号',
    `amount`          DECIMAL(10,2) NOT NULL                COMMENT '金额',
    `user_id`         BIGINT        DEFAULT NULL            COMMENT '用户ID',
    `operator_id`     BIGINT        DEFAULT NULL            COMMENT '操作人',
    `payment_method`  TINYINT       DEFAULT NULL            COMMENT '支付方式：1余额 2微信 3系统',
    `remark`          VARCHAR(255)  DEFAULT NULL,
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_type_time` (`type`, `create_time`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务流水（FR-AD-024）';

-- ===============================================================
-- 九、管理端（FR-AD-001 / 014 / 028 / 预留 RBAC）
-- ===============================================================

-- 9.1 角色（预留 RBAC）
DROP TABLE IF EXISTS `admin_role`;
CREATE TABLE `admin_role` (
    `id`          BIGINT       NOT NULL                COMMENT '雪花ID',
    `code`        VARCHAR(32)  NOT NULL                COMMENT '角色编码：SUPER_ADMIN/MANAGER/OPERATOR',
    `name`        VARCHAR(32)  NOT NULL                COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL,
    `status`      TINYINT      DEFAULT 1               COMMENT '0禁用 1启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理端角色（预留RBAC）';

-- 9.2 权限
DROP TABLE IF EXISTS `admin_permission`;
CREATE TABLE `admin_permission` (
    `id`          BIGINT       NOT NULL                COMMENT '雪花ID',
    `parent_id`   BIGINT       DEFAULT 0               COMMENT '父权限ID',
    `code`        VARCHAR(64)  NOT NULL                COMMENT '权限编码，如 order:list',
    `name`        VARCHAR(64)  NOT NULL                COMMENT '权限名称',
    `type`        TINYINT      DEFAULT 2               COMMENT '1菜单 2按钮 3接口',
    `path`        VARCHAR(128) DEFAULT NULL            COMMENT '前端路由或接口路径',
    `sort`        INT          DEFAULT 0,
    `status`      TINYINT      DEFAULT 1               COMMENT '0禁用 1启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理端权限';

-- 9.3 角色权限
DROP TABLE IF EXISTS `admin_role_permission`;
CREATE TABLE `admin_role_permission` (
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY `idx_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联';

-- 9.4 管理员
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id`              BIGINT       NOT NULL                COMMENT '雪花ID',
    `username`        VARCHAR(32)  NOT NULL                COMMENT '登录账号',
    `password`        VARCHAR(128) NOT NULL                COMMENT 'BCrypt 加密密码，FR-AD-001',
    `real_name`       VARCHAR(32)  DEFAULT NULL            COMMENT '真实姓名',
    `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像',
    `email`           VARCHAR(64)  DEFAULT NULL,
    `phone`           VARCHAR(32)  DEFAULT NULL,
    `role_id`         BIGINT       DEFAULT NULL            COMMENT '角色ID（预留RBAC）',
    `status`          TINYINT      DEFAULT 1               COMMENT '0禁用 1正常',
    `last_login_time` DATETIME     DEFAULT NULL,
    `last_login_ip`   VARCHAR(64)  DEFAULT NULL,
    `version`         INT          DEFAULT 0,
    `deleted`         TINYINT      DEFAULT 0,
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员（FR-AD-001）';

-- 9.5 操作日志（FR-AD-028，保留 90 天，定时清理）
DROP TABLE IF EXISTS `admin_log`;
CREATE TABLE `admin_log` (
    `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
    `admin_id`     BIGINT       DEFAULT NULL            COMMENT '管理员ID',
    `username`     VARCHAR(32)  DEFAULT NULL            COMMENT '冗余账号',
    `operation`    VARCHAR(64)  NOT NULL                COMMENT '操作描述',
    `module`       VARCHAR(32)  DEFAULT NULL            COMMENT '模块',
    `method`       VARCHAR(255) DEFAULT NULL            COMMENT '方法',
    `request_url`  VARCHAR(255) DEFAULT NULL            COMMENT '请求URL',
    `params`       TEXT         DEFAULT NULL            COMMENT '请求参数（脱敏后）',
    `ip`           VARCHAR(64)  DEFAULT NULL            COMMENT 'IP',
    `location`     VARCHAR(64)  DEFAULT NULL            COMMENT 'IP归属地',
    `user_agent`   VARCHAR(255) DEFAULT NULL,
    `status`       TINYINT      DEFAULT 1               COMMENT '0失败 1成功',
    `error_msg`    TEXT         DEFAULT NULL            COMMENT '错误信息',
    `cost_ms`      BIGINT       DEFAULT 0               COMMENT '耗时（毫秒）',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_admin_time` (`admin_id`, `create_time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_module_status` (`module`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志（FR-AD-028）';

-- ===============================================================
-- 十、系统配置（FR-AD-025 ~ 027 / FR-UC-027 / FR-UC-026 等阈值）
-- ===============================================================

DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
    `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
    `config_key`   VARCHAR(64)  NOT NULL                COMMENT '配置键',
    `config_value` TEXT         NOT NULL                COMMENT '配置值',
    `config_group` VARCHAR(32)  DEFAULT 'default'        COMMENT '分组',
    `description`  VARCHAR(255) DEFAULT NULL            COMMENT '说明',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`config_key`),
    KEY `idx_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置（FR-AD-025/026/027）';

-- ===============================================================
-- 十一、签到二维码配置（FR-AD-008）
-- 二维码主体由 Redis 滚动存储；此处存储密钥与轮换策略
-- ===============================================================

DROP TABLE IF EXISTS `sign_qrcode_config`;
CREATE TABLE `sign_qrcode_config` (
    `id`             BIGINT       NOT NULL                COMMENT '雪花ID',
    `store_id`       BIGINT       NOT NULL                COMMENT '门店ID',
    `aes_key`        VARCHAR(64)  NOT NULL                COMMENT 'AES-256 密钥（Base64）',
    `hmac_key`       VARCHAR(64)  NOT NULL                COMMENT 'HMAC-SHA256 密钥',
    `rotate_seconds` INT          DEFAULT 60              COMMENT '二维码轮换秒数',
    `status`         TINYINT      DEFAULT 1               COMMENT '0禁用 1启用',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到二维码配置（FR-AD-008）';

SET FOREIGN_KEY_CHECKS = 1;

-- ===============================================================
-- 十二、初始化数据
-- ===============================================================

-- 12.1 角色
INSERT INTO `admin_role` (`id`, `code`, `name`, `description`, `status`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '系统最高权限', 1),
(2, 'MANAGER',     '店长',       '门店日常运营', 1),
(3, 'OPERATOR',    '运营',       '日常运营操作', 1);

-- 12.2 权限种子（示例：核心接口编码；菜单项按需扩展）
INSERT INTO `admin_permission` (`id`, `parent_id`, `code`, `name`, `type`, `sort`, `status`) VALUES
(1,  0, 'dashboard',     '仪表盘',     1, 1, 1),
(2,  0, 'store',         '门店管理',   1, 2, 1),
(3,  0, 'seat',          '座位管理',   1, 3, 1),
(4,  0, 'order',         '订单管理',   1, 4, 1),
(5,  0, 'user',          '用户管理',   1, 5, 1),
(6,  0, 'notice',        '公告管理',   1, 6, 1),
(7,  0, 'review',        '评价管理',   1, 7, 1),
(8,  0, 'stats',         '数据统计',   1, 8, 1),
(9,  0, 'finance',       '财务管理',   1, 9, 1),
(10, 0, 'system',        '系统设置',   1, 10, 1),
-- 按钮/接口级权限
(101, 4, 'order:list',    '订单列表',   2, 1, 1),
(102, 4, 'order:export',  '订单导出',   2, 2, 1),
(103, 4, 'refund:audit',  '退款审核',   2, 3, 1),
(104, 7, 'review:reply',  '评价回复',   2, 1, 1);

-- 12.3 超级管理员拥有所有权限
INSERT INTO `admin_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `admin_permission`;

-- 12.4 管理员默认账号（密码：123456 的 BCrypt 哈希，正式上线请替换）
INSERT INTO `admin` (`id`, `username`, `password`, `real_name`, `role_id`, `status`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1, 1);

-- 12.5 座位类型
INSERT INTO `seat_type` (`id`, `name`, `code`, `description`, `sort`, `status`) VALUES
(1, '普通区', 'NORMAL', '标准自习位',                1, 1),
(2, '静音区', 'SILENT', '全程保持安静',              2, 1),
(3, '包间',   'ROOM',   '独立隔间，适合长时间学习',  3, 1),
(4, 'VIP',    'VIP',    '高端配置，含台灯/插座',     4, 1);

-- 12.6 套餐（FR-UC-019）
INSERT INTO `package` (`id`, `name`, `type`, `duration_value`, `unit`, `price`, `original_price`, `sort`, `status`) VALUES
(1, '4小时体验', 1, 4,    'hour',   15.00, 19.00, 1, 1),
(2, '8小时全天', 1, 8,    'hour',   28.00, 35.00, 2, 1),
(3, '日卡',     2, 1,    'day',    35.00, 45.00, 3, 1),
(4, '周卡',     3, 1,    'week',  180.00, 220.00, 4, 1),
(5, '月卡',     4, 1,    'month', 599.00, 780.00, 5, 1);

-- 12.7 系统配置（阈值/规则由 service 层读取）
-- 退款规则 FR-UC-027
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_group`, `description`) VALUES
(1,  'refund.free_minutes',         '60',   'refund',  '免费取消时限（分钟）>60全退'),
(2,  'refund.ratio_15_to_60',       '0.90', 'refund',  '15~60分钟取消退款比例'),
(3,  'refund.ratio_0_to_15',        '0.70', 'refund',  '<15分钟取消退款比例'),
-- 下单 FR-UC-018 / FR-UC-025
(4,  'order.pay_timeout_minutes',   '15',   'order',   '支付超时自动取消（分钟）'),
(5,  'order.no_show_minutes',       '30',   'order',   '开始后未签到自动取消（分钟）'),
-- 签到 FR-UC-030
(6,  'sign.window_minutes',         '15',   'sign',    '签到时间窗口±分钟'),
(7,  'sign.gps_max_meters',         '100',  'sign',    '签到GPS距离上限（米）'),
-- 暂停 FR-UC-033
(8,  'pause.max_minutes',           '30',   'pause',   '暂停保留座位最长（分钟）'),
-- 信用 FR-UC-054
(9,  'credit.no_show_limit',        '3',    'credit',  '30天内爽约次数上限'),
(10, 'credit.limit_days',           '7',    'credit',  '触发限制预约天数'),
(11, 'credit.period_days',          '30',   'credit',  '爽约统计周期（天）'),
-- 限流 FR-SY-001（QPS 阈值）
(12, 'limiter.login.qps',           '5',    'limiter', '登录限流 QPS'),
(13, 'limiter.order.qps',           '10',   'limiter', '下单限流 QPS'),
(14, 'limiter.sign.qps',            '5',    'limiter', '签到限流 QPS'),
(15, 'limiter.review.qps',          '3',    'limiter', '评价限流 QPS'),
-- JWT
(16, 'jwt.user.expire_seconds',     '604800', 'jwt',   '用户Token有效期（秒）=7天，FR-UC-005'),
(17, 'jwt.admin.expire_seconds',    '7200',   'jwt',   '管理员Token有效期（秒）=2小时，FR-AD-001');

-- ===============================================================
-- 表清单（合计 26 张业务表）
-- ===============================================================
-- 用户域：user / user_credit_log / user_balance_log / user_subscribe_message / user_message / feedback
-- 门店域：store / store_banner / store_blacklist / seat_type / seat
-- 价格域：package / price_rule
-- 订单域：orders / order_payment / order_refund / order_extension / order_sign / order_appeal / order_pause
-- 评价域：review / review_image / review_reply
-- 公告域：notice / notice_attachment / notice_read
-- 营销域：coupon / coupon_claim
-- 财务域：finance_record
-- 管理域：admin_role / admin_permission / admin_role_permission / admin / admin_log
-- 系统域：system_config / sign_qrcode_config
-- ===============================================================