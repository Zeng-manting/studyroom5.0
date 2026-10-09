package com.studyroom.common.constants;

import java.math.BigDecimal;

/**
 * 系统级公共常量
 * <p>包括鉴权请求头、Redis Key 前缀、分页默认值等。
 * Redis Key 前缀统一规划，避免模块间冲突：</p>
 * <pre>
 * studyroom:seat:lock:{seatId}:{date}:{slot}   座位时段锁（FR-UC-018）
 * studyroom:order:pay:{orderId}                支付超时延迟队列（FR-UC-025）
 * studyroom:order:checkin:{orderId}            未签到超时（FR-UC-026）
 * studyroom:pause:{orderId}                    暂停计时器（FR-UC-033）
 * studyroom:token:checkin:{code}               签到二维码（FR-UC-030）
 * studyroom:idempotent:{key}                   接口幂等 Token
 * studyroom:limiter:{key}                      RRateLimiter 限流器（FR-SY-001）
 * studyroom:captcha:{uuid}                     图形验证码（FR-AD-001）
 * studyroom:email:code:{email}                 邮箱验证码（FR-UC-004）
 * </pre>
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /* ==================== 鉴权 ==================== */

    /** JWT 请求头 */
    public static final String AUTH_HEADER = "Authorization";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /* ==================== Redis Key 前缀 ==================== */

    /** 全局业务前缀 */
    public static final String REDIS_PREFIX = "studyroom:";

    /** 座位时段锁前缀（FR-UC-018 下单原子锁座） */
    public static final String SEAT_LOCK_KEY = REDIS_PREFIX + "seat:lock:";

    /** 支付超时前缀（FR-UC-025 15 分钟支付超时） */
    public static final String ORDER_PAY_TIMEOUT_KEY = REDIS_PREFIX + "order:pay:";

    /** 未签到超时前缀（FR-UC-026 开始后 30 分钟未签到取消） */
    public static final String ORDER_CHECKIN_TIMEOUT_KEY = REDIS_PREFIX + "order:checkin:";

    /** 暂停计时器前缀（FR-UC-033 中途离座 30 分钟保留） */
    public static final String SEAT_PAUSE_KEY = REDIS_PREFIX + "pause:";

    /** 签到二维码前缀（FR-UC-030 五重校验） */
    public static final String CHECKIN_CODE_KEY = REDIS_PREFIX + "token:checkin:";

    /** 接口幂等前缀 */
    public static final String IDEMPOTENT_KEY = REDIS_PREFIX + "idempotent:";

    /** 接口限流前缀（FR-SY-001 RRateLimiter） */
    public static final String RATE_LIMITER_KEY = REDIS_PREFIX + "limiter:";

    /** 图形验证码前缀（FR-AD-001） */
    public static final String CAPTCHA_KEY = REDIS_PREFIX + "captcha:";

    /** 管理端登录失败计数前缀（FR-AD-001 防爆破）：admin:login:fail:{username} */
    public static final String ADMIN_LOGIN_FAIL_KEY = REDIS_PREFIX + "admin:login:fail:";

    /** 管理端连续失败锁定阈值：5 次（FR-AD-001） */
    public static final int ADMIN_LOGIN_MAX_FAIL = 5;

    /** 管理端登录锁定时长：10 分钟（FR-AD-001） */
    public static final int ADMIN_LOGIN_LOCK_MINUTES = 10;

    /** 图形验证码有效期：5 分钟（FR-AD-001） */
    public static final int CAPTCHA_TTL_MINUTES = 5;

    /** 邮箱验证码前缀（FR-UC-004 邮箱绑定） */
    public static final String EMAIL_CODE_KEY = REDIS_PREFIX + "email:code:";

    /** 邮箱验证码每日发送次数前缀（FR-UC-004 防刷：每用户每天最多 5 次） */
    public static final String EMAIL_CODE_DAILY_KEY = REDIS_PREFIX + "email:limit:";

    /** 微信 sessionKey 前缀（FR-UC-001 登录后缓存，供手机号解密等使用） */
    public static final String WX_SESSION_KEY = REDIS_PREFIX + "wx:session:";

    /** 订单延迟队列名（FR-UC-025 支付超时 / FR-UC-026 未签到取消，Redisson RDelayedQueue） */
    public static final String ORDER_DELAY_QUEUE = REDIS_PREFIX + "order:delay:queue";

    /** 订单号每日自增序列前缀（FR-UC-018 订单号 = yyyyMMdd + 6 位序号） */
    public static final String ORDER_NO_SEQ_KEY = REDIS_PREFIX + "order:no:seq:";

    /* ==================== 订单业务常量 ==================== */

    /** 支付超时时间：15 分钟（FR-UC-025） */
    public static final int PAY_TIMEOUT_MINUTES = 15;

    /** 未签到自动取消：开始后 30 分钟（FR-UC-026） */
    public static final int NO_SHOW_CANCEL_MINUTES = 30;

    /** 阶梯退款：距开始 >60 分钟全退（FR-UC-027） */
    public static final int REFUND_FULL_MINUTES = 60;

    /** 阶梯退款：距开始 15~60 分钟退 90%（FR-UC-027） */
    public static final BigDecimal REFUND_RATIO_90 = new BigDecimal("0.90");

    /** 阶梯退款：距开始 <15 分钟退 70%（FR-UC-027） */
    public static final BigDecimal REFUND_RATIO_70 = new BigDecimal("0.70");

    /* ==================== 签到与暂停业务常量（FR-UC-029~034） ==================== */

    /** 签到时间窗口：可提前签到分钟数（FR-UC-030 ④，开始前 15 分钟起可签到） */
    public static final int SIGN_EARLY_MINUTES = 15;

    /** 签到时间窗口：开始后仍可签到的分钟数（FR-UC-030 ④，超过则校验失败走申诉） */
    public static final int SIGN_LATE_WINDOW_MINUTES = 15;

    /** 签到 GPS 校验最大距离：米（FR-UC-030 ⑤） */
    public static final int SIGN_GPS_MAX_DISTANCE = 100;

    /** 中途离座座位保留时长：分钟（FR-UC-033，超时自动结算） */
    public static final int PAUSE_KEEP_MINUTES = 30;

    /* ==================== 信用与续时业务常量（FR-UC-053~055，模块8） ==================== */

    /** 信用：爽约统计周期：天（FR-UC-053/054，30 天滚动窗口） */
    public static final int CREDIT_PERIOD_DAYS = 30;

    /** 信用：周期内爽约次数上限，达到即限制预约（FR-UC-054） */
    public static final int CREDIT_NO_SHOW_LIMIT = 3;

    /** 信用：限制预约天数（FR-UC-054，到期自动恢复） */
    public static final int CREDIT_LIMIT_DAYS = 7;

    /** 信用：单次爽约信用分扣减（FR-UC-053） */
    public static final int CREDIT_NO_SHOW_SCORE_DELTA = 10;

    /** 续时分布式锁前缀（FR-UC-055，防并发续时/与结算竞争） */
    public static final String ORDER_EXTEND_LOCK_KEY = REDIS_PREFIX + "order:extend:lock:";

    /* ==================== 分页 ==================== */

    /** 默认页码 */
    public static final long DEFAULT_PAGE = 1L;

    /** 默认每页条数（管理端列表分页 20 条，FR-AD-010） */
    public static final long DEFAULT_SIZE = 20L;

    /** 每页最大条数 */
    public static final long MAX_SIZE = 100L;

    /* ==================== 逻辑删除 ==================== */

    /** 逻辑未删除 */
    public static final int NOT_DELETED = 0;

    /** 逻辑已删除 */
    public static final int DELETED = 1;

    /* ==================== 用户角色（Spring Security hasRole） ==================== */

    /** 普通用户角色 key（FR-UC-001） */
    public static final String ROLE_USER = "USER";

    /** 管理员角色 key（FR-AD-001） */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 超管角色 key（FR-AD-001） */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
}
