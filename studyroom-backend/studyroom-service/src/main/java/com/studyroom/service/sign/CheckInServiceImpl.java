package com.studyroom.service.sign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.constants.CommonConstants;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.properties.CryptoProperties;
import com.studyroom.common.utils.CryptoUtils;
import com.studyroom.common.utils.GeoUtils;
import com.studyroom.common.utils.IdGenerator;
import com.studyroom.common.utils.JacksonUtils;
import com.studyroom.common.utils.WebUtils;
import com.studyroom.entity.dto.order.AppealCreateDTO;
import com.studyroom.entity.dto.order.CheckInDTO;
import com.studyroom.entity.order.OrderAppeal;
import com.studyroom.entity.order.OrderSign;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.vo.order.CheckInResultVO;
import com.studyroom.mapper.order.OrderAppealMapper;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.order.OrderSignMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 自助签到服务实现（FR-UC-029 ~ FR-UC-032）
 *
 * <p>五重校验按序执行，任一环失败即终止并留痕（order_sign.check_result=0）；
 * 签到成功走 CAS（status 1→2）保证幂等，重复扫码/与超时取消并发安全。</p>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    /** 二维码前缀（与 FR-AD-008 管理端生成约定） */
    public static final String QR_PREFIX = "SR-SIGN:";
    /** 签到码类型标识 */
    private static final int QR_TYPE_SIGN = 1;

    private final OrderMapper orderMapper;
    private final OrderSignMapper orderSignMapper;
    private final OrderAppealMapper orderAppealMapper;
    private final StoreMapper storeMapper;
    private final SeatMapper seatMapper;
    private final CryptoProperties cryptoProperties;
    private final TransactionTemplate transactionTemplate;

    private static final ObjectMapper MAPPER = JacksonUtils.defaultMapper();

    /* ==================== FR-UC-029/030：扫码签到五重校验 ==================== */

    @Override
    public CheckInResultVO checkIn(Long userId, CheckInDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        String plain = null;
        String encrypted = null;
        String sign = null;

        // ---------- ① 格式解析 + AES 解密（FR-UC-030 ①） ----------
        if (!StringUtils.hasText(dto.getQrcode()) || !dto.getQrcode().startsWith(QR_PREFIX)) {
            return fail(userId, null, null, null, null, "二维码格式不正确，请扫描门店签到码", null);
        }
        String[] parts = dto.getQrcode().substring(QR_PREFIX.length()).split(":");
        if (parts.length != 2) {
            return fail(userId, null, null, null, null, "二维码格式不正确，请扫描门店签到码", null);
        }
        encrypted = parts[0];
        sign = parts[1];
        try {
            plain = CryptoUtils.aesDecrypt(encrypted, cryptoProperties.getAesKey());
        } catch (Exception e) {
            // FR-UC-030 ① 失败：非本系统签到码或密钥不符
            return fail(userId, null, encrypted, sign, null, "签到码无效（解密失败），请扫描门店张贴的签到码", null);
        }

        // ---------- ② HMAC 验签 + 有效期（FR-UC-030 ②） ----------
        JsonNode payload;
        try {
            payload = MAPPER.readTree(plain);
        } catch (Exception e) {
            return fail(userId, null, encrypted, sign, plain, "签到码内容无效", null);
        }
        if (!CryptoUtils.hmacVerify(encrypted, cryptoProperties.getHmacKey(), sign)) {
            // 常量时间比较，防 timing attack；失败即内容被篡改
            return fail(userId, null, encrypted, sign, plain, "签到码签名校验失败，内容可能被篡改", null);
        }
        if (payload.path("t").asInt(-1) != QR_TYPE_SIGN) {
            return fail(userId, null, encrypted, sign, plain, "该二维码不是签到码", null);
        }
        long exp = payload.path("exp").asLong(0);
        if (exp > 0 && Instant.now().getEpochSecond() > exp) {
            return fail(userId, null, encrypted, sign, plain, "签到码已过期，请联系店员刷新", null);
        }

        // ---------- ③ 查有效订单（FR-UC-030 ③） ----------
        long storeId = payload.path("sid").asLong(0);
        if (storeId <= 0) {
            return fail(userId, null, encrypted, sign, plain, "签到码无效（门店信息缺失）", null);
        }
        List<Orders> candidates = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .eq(Orders::getStoreId, storeId)
                .eq(Orders::getStatus, Orders.STATUS_RESERVED)
                .isNull(Orders::getSignTime)
                .between(Orders::getStartTime, now.minusHours(24), now.plusHours(24))
                .orderByAsc(Orders::getStartTime));
        if (candidates.isEmpty()) {
            // status=0 待支付 / 已被爽约取消(6) / 已签到(2) 均不构成"有效订单"
            return fail(userId, null, encrypted, sign, plain,
                    "未找到该门店的有效预约订单（请确认订单已支付且尚未签到）", null);
        }

        // ---------- ④ 时间窗口 ±15 分钟（FR-UC-030 ④） ----------
        Orders order = null;
        for (Orders candidate : candidates) {
            LocalDateTime windowStart = candidate.getStartTime()
                    .minusMinutes(CommonConstants.SIGN_EARLY_MINUTES);
            LocalDateTime windowEnd = candidate.getStartTime()
                    .plusMinutes(CommonConstants.SIGN_LATE_WINDOW_MINUTES);
            if (!now.isBefore(windowStart) && !now.isAfter(windowEnd)) {
                order = candidate;
                break;
            }
        }
        if (order == null) {
            Orders nearest = candidates.get(0);
            return fail(userId, nearest.getId(), encrypted, sign, plain,
                    String.format("不在签到时间窗口内（开始前%d分钟至开始后%d分钟），最近订单开始于 %s",
                            CommonConstants.SIGN_EARLY_MINUTES,
                            CommonConstants.SIGN_LATE_WINDOW_MINUTES,
                            nearest.getStartTime().toLocalTime()), null);
        }

        // ---------- ⑤ GPS 距离 ≤100 米（FR-UC-030 ⑤） ----------
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            return fail(userId, order.getId(), encrypted, sign, plain,
                    "未获取到定位信息，请授权定位后重试", order);
        }
        Store store = storeMapper.selectById(order.getStoreId());
        Integer distance = null;
        if (store != null && store.getLatitude() != null && store.getLongitude() != null) {
            distance = GeoUtils.distanceMeters(dto.getLatitude(), dto.getLongitude(),
                    store.getLatitude(), store.getLongitude());
            if (distance > CommonConstants.SIGN_GPS_MAX_DISTANCE) {
                return fail(userId, order.getId(), encrypted, sign, plain,
                        String.format("距离门店过远（%d米 > %d米），请到达门店后再签到",
                                distance, CommonConstants.SIGN_GPS_MAX_DISTANCE), order);
            }
        } else {
            // 门店未配置坐标（演示数据）：跳过距离校验，仅记录定位
            log.warn("[CheckInService] 门店未配置 GPS 坐标，跳过距离校验: storeId={}", order.getStoreId());
        }

        // ---------- 五重校验全部通过：FR-UC-031 签到成功处理 ----------
        // 注意：此处需通过 TransactionTemplate 显式开启事务（checkIn 内部调用，@Transactional 自调用不生效）
        final Orders finalOrder = order;
        final String finalEncrypted = encrypted;
        final String finalSign = sign;
        final String finalPlain = plain;
        final Integer finalDistance = distance;
        return transactionTemplate.execute(status -> doSign(finalOrder, finalEncrypted, finalSign, finalPlain,
                now, dto.getLatitude(), dto.getLongitude(), finalDistance));
    }

    /**
     * 签到成功处理（FR-UC-031）：事务内 CAS 订单 1→2 + 留痕 + 座位转使用中
     * <p>由 {@link #checkIn} 通过 TransactionTemplate 调用（避免 @Transactional 自调用失效）。</p>
     */
    private CheckInResultVO doSign(Orders order, String encrypted, String sign, String plain,
                                     LocalDateTime now,
                                     java.math.BigDecimal latitude, java.math.BigDecimal longitude,
                                     Integer distance) {
        // CAS：status 1→2（防重复签到 / 与爽约取消并发）
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, order.getId())
                .eq(Orders::getStatus, Orders.STATUS_RESERVED)
                .isNull(Orders::getSignTime)
                .set(Orders::getStatus, Orders.STATUS_IN_USE)
                .set(Orders::getSignTime, now)
                .set(Orders::getSignLatitude, latitude)
                .set(Orders::getSignLongitude, longitude)
                .set(Orders::getSignGpsDistance, distance));
        if (rows == 0) {
            throw new BusinessException("订单状态已变更（可能已签到或已取消），请刷新订单列表");
        }

        // 留痕：order_sign 成功记录（FR-SY-003 审计）
        HttpServletRequest req = WebUtils.currentRequest();
        OrderSign orderSign = new OrderSign();
        orderSign.setId(IdGenerator.nextId());
        orderSign.setOrderId(order.getId());
        orderSign.setQrcodePlain(plain);
        orderSign.setQrcodeEncrypted(encrypted);
        orderSign.setQrcodeSign(sign);
        orderSign.setSignTime(now);
        orderSign.setSignLatitude(latitude);
        orderSign.setSignLongitude(longitude);
        orderSign.setGpsDistance(distance);
        orderSign.setCheckResult(OrderSign.RESULT_SUCCESS);
        orderSign.setUserAgent(req != null ? req.getHeader("User-Agent") : null);
        orderSign.setClientIp(WebUtils.getClientIp());
        orderSignMapper.insert(orderSign);

        // 座位 0空闲 → 1使用中（best-effort：座位被管理端置维护等场景不阻塞签到）
        int seatRows = seatMapper.casUpdateStatus(order.getSeatId(), 0, 1);
        if (seatRows == 0) {
            log.warn("[CheckInService] 座位状态 CAS(0→1) 未命中，座位转使用中失败: seatId={}", order.getSeatId());
        }

        // 迟到判定（FR-UC-031）：签到时间晚于预约开始时间
        boolean late = now.isAfter(order.getStartTime());
        int lateMinutes = late ? (int) java.time.Duration.between(order.getStartTime(), now).toMinutes() : 0;

        CheckInResultVO vo = buildBaseVO(order);
        vo.setSuccess(true);
        vo.setAppealable(false);
        vo.setLate(late);
        vo.setLateMinutes(lateMinutes);
        vo.setGpsDistance(distance);
        vo.setSignTime(now);
        log.info("[CheckInService] FR-UC-031 签到成功: orderNo={}, late={}, distance={}m",
                order.getOrderNo(), late, distance);
        return vo;
    }

    /* ==================== FR-UC-032：申诉 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitAppeal(Long userId, AppealCreateDTO dto) {
        // 订单归属校验
        Orders order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        // 防重复提交：已有待审核申诉直接拒绝
        if (orderAppealMapper.countPendingByOrderId(dto.getOrderId()) > 0) {
            throw new BusinessException("该订单已有待审核的申诉，请耐心等待处理");
        }
        // orders.appeal_status 0无 → 1待审（CAS 防并发重复）
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getId, dto.getOrderId())
                .eq(Orders::getAppealStatus, 0)
                .set(Orders::getAppealStatus, 1));
        if (rows == 0) {
            throw new BusinessException("该订单已有待审核的申诉，请耐心等待处理");
        }

        OrderAppeal appeal = new OrderAppeal();
        appeal.setId(IdGenerator.nextId());
        appeal.setOrderId(dto.getOrderId());
        appeal.setUserId(userId);
        appeal.setReasonType(dto.getReasonType());
        appeal.setDescription(dto.getDescription());
        // 图片 URL 列表 → JSON 数组字符串（order_appeal.images JSON 列）
        appeal.setImages(dto.getImages() == null || dto.getImages().isEmpty()
                ? null : toJsonArray(dto.getImages()));
        appeal.setStatus(OrderAppeal.STATUS_PENDING);
        orderAppealMapper.insert(appeal);
        log.info("[CheckInService] FR-UC-032 申诉已提交: orderId={}, type={}", dto.getOrderId(), dto.getReasonType());
        return appeal.getId();
    }

    /* ==================== FR-AD-008：签到码生成（管理端复用） ==================== */

    @Override
    public String generateStoreQr(Long storeId, int validHours) {
        try {
            long exp = Instant.now().plusSeconds(validHours * 3600L).getEpochSecond();
            String plain = String.format("{\"t\":%d,\"sid\":%d,\"nonce\":\"%s\",\"exp\":%d}",
                    QR_TYPE_SIGN, storeId, UUID.randomUUID().toString().replace("-", "").substring(0, 12), exp);
            String encrypted = CryptoUtils.aesEncrypt(plain, cryptoProperties.getAesKey());
            String sign = CryptoUtils.hmacSign(encrypted, cryptoProperties.getHmacKey());
            return QR_PREFIX + encrypted + ":" + sign;
        } catch (Exception e) {
            throw new BusinessException("签到码生成失败");
        }
    }

    /* ==================== 私有辅助 ==================== */

    /**
     * 校验失败统一出口（FR-UC-032）：留痕 + 返回失败原因（appealable=true）
     */
    private CheckInResultVO fail(Long userId, Long orderId, String encrypted, String sign,
                                 String plain, String reason, Orders order) {
        // 失败留痕（best-effort：留痕异常不影响用户拿到失败原因）
        try {
            HttpServletRequest req = WebUtils.currentRequest();
            OrderSign orderSign = new OrderSign();
            orderSign.setId(IdGenerator.nextId());
            orderSign.setOrderId(orderId);
            orderSign.setQrcodePlain(plain);
            orderSign.setQrcodeEncrypted(encrypted);
            orderSign.setQrcodeSign(sign);
            orderSign.setSignTime(LocalDateTime.now());
            orderSign.setCheckResult(OrderSign.RESULT_FAIL);
            orderSign.setFailReason(reason);
            orderSign.setUserAgent(req != null ? req.getHeader("User-Agent") : null);
            orderSign.setClientIp(WebUtils.getClientIp());
            orderSignMapper.insert(orderSign);
        } catch (Exception e) {
            log.error("[CheckInService] 签到失败留痕异常: userId={}, reason={}", userId, reason, e);
        }
        log.warn("[CheckInService] FR-UC-030 校验失败: userId={}, orderId={}, reason={}", userId, orderId, reason);

        CheckInResultVO vo = order != null ? buildBaseVO(order) : new CheckInResultVO();
        if (order == null) {
            vo.setOrderId(orderId);
        }
        vo.setSuccess(false);
        vo.setAppealable(true);
        vo.setFailReason(reason);
        return vo;
    }

    private CheckInResultVO buildBaseVO(Orders order) {
        CheckInResultVO vo = new CheckInResultVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        Store store = storeMapper.selectById(order.getStoreId());
        vo.setStoreName(store != null ? store.getName() : null);
        Seat seat = seatMapper.selectById(order.getSeatId());
        vo.setSeatNo(seat != null ? seat.getSeatNo() : null);
        return vo;
    }

    private String toJsonArray(List<String> urls) {
        try {
            return MAPPER.writeValueAsString(urls);
        } catch (Exception e) {
            return null;
        }
    }
}
