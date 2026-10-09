package com.studyroom.service.admin;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.entity.dto.admin.BlacklistSaveDTO;
import com.studyroom.entity.dto.admin.SeatLayoutSaveDTO;
import com.studyroom.entity.dto.admin.StoreSaveDTO;
import com.studyroom.entity.order.Orders;
import com.studyroom.entity.store.Seat;
import com.studyroom.entity.store.SeatType;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.store.StoreBlacklist;
import com.studyroom.entity.vo.admin.AdminBlacklistVO;
import com.studyroom.entity.vo.admin.AdminQrCodeVO;
import com.studyroom.entity.vo.admin.AdminSeatVO;
import com.studyroom.entity.vo.admin.AdminStoreVO;
import com.studyroom.entity.vo.order.SeatMapVO;
import com.studyroom.mapper.order.OrderMapper;
import com.studyroom.mapper.store.SeatMapper;
import com.studyroom.mapper.store.SeatTypeMapper;
import com.studyroom.mapper.store.StoreBlacklistMapper;
import com.studyroom.mapper.store.StoreMapper;
import com.studyroom.service.sign.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端门店服务实现 —— FR-AD-004 ~ FR-AD-008
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>门店 facilities / images 为 JSON 列：保存时 List → JSON 字符串，读取时反向解析</li>
 *   <li>座位布局保存为全量同步：事务内 upsert + 缺失软删；座位号唯一冲突（uk_store_seat_no）
 *       转友好提示；删除前校验无进行中订单</li>
 *   <li>座位状态切换走 SeatMapper.casUpdateStatus 显式 CAS（与 FR-UC-031/034 同一通道，
 *       防止与签到/离场结算并发覆盖）</li>
 *   <li>黑名单月历：一次性加载门店全部启用规则，Java 内按日匹配（周期性按周几 + 单次按日期），
 *       避免逐日查询 N+1</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStoreServiceImpl implements AdminStoreService {

    /** 全天闭店判定：00:00 开始且结束 ≥23:59（与用户端 StoreServiceImpl 口径一致） */
    private static final LocalTime ALL_DAY_END = LocalTime.of(23, 58);

    private final StoreMapper storeMapper;
    private final StoreBlacklistMapper blacklistMapper;
    private final SeatMapper seatMapper;
    private final SeatTypeMapper seatTypeMapper;
    private final OrderMapper orderMapper;
    private final CheckInService checkInService;

    /* ==================== FR-AD-004：门店信息 ==================== */

    @Override
    public List<AdminStoreVO> listStores() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .orderByAsc(Store::getSort).orderByDesc(Store::getId));
        if (stores.isEmpty()) {
            return Collections.emptyList();
        }
        // 座位数按门店聚合（一次查询避免 N+1）
        Map<Long, Long> seatCountMap = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                        .select(Seat::getId, Seat::getStoreId))
                .stream().collect(Collectors.groupingBy(Seat::getStoreId, Collectors.counting()));
        return stores.stream().map(s -> toStoreVO(s, seatCountMap.getOrDefault(s.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public AdminStoreVO getStore(Long storeId) {
        Store store = loadStore(storeId);
        Long seatCount = seatMapper.selectCount(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getStoreId, storeId));
        return toStoreVO(store, seatCount);
    }

    @Override
    public Long createStore(StoreSaveDTO dto) {
        Store store = new Store();
        applyDto(store, dto);
        store.setRating(new java.math.BigDecimal("5.0"));
        storeMapper.insert(store);
        log.info("[AdminStore] FR-AD-004 新增门店: id={}, name={}", store.getId(), store.getName());
        return store.getId();
    }

    @Override
    public void updateStore(Long storeId, StoreSaveDTO dto) {
        Store store = loadStore(storeId);
        applyDto(store, dto);
        storeMapper.updateById(store);
        log.info("[AdminStore] FR-AD-004 编辑门店: id={}, name={}", storeId, store.getName());
    }

    /**
     * DTO → 实体字段拷贝（facilities/images List → JSON 字符串）
     */
    private void applyDto(Store store, StoreSaveDTO dto) {
        store.setName(dto.getName());
        store.setAddress(dto.getAddress());
        store.setPhone(dto.getPhone());
        store.setBusinessHours(StringUtils.hasText(dto.getBusinessHours())
                ? dto.getBusinessHours() : "08:00-23:00");
        store.setLatitude(dto.getLatitude());
        store.setLongitude(dto.getLongitude());
        store.setDescription(dto.getDescription());
        store.setFacilities(toJsonArray(dto.getFacilities()));
        store.setImages(toJsonArray(dto.getImages()));
        store.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        store.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }

    /* ==================== FR-AD-005：时段黑名单 ==================== */

    @Override
    public List<AdminBlacklistVO> listBlacklistRules(Long storeId) {
        loadStore(storeId);
        List<StoreBlacklist> rules = blacklistMapper.selectList(
                new LambdaQueryWrapper<StoreBlacklist>()
                        .eq(StoreBlacklist::getStoreId, storeId)
                        .orderByDesc(StoreBlacklist::getCreateTime));
        return rules.stream().map(this::toBlacklistVO).collect(Collectors.toList());
    }

    @Override
    public Long addBlacklistRule(Long storeId, BlacklistSaveDTO dto) {
        loadStore(storeId);
        StoreBlacklist rule = new StoreBlacklist();
        applyRuleDto(rule, storeId, dto);
        blacklistMapper.insert(rule);
        log.info("[AdminStore] FR-AD-005 新增黑名单规则: storeId={}, id={}, type={}",
                storeId, rule.getId(), dto.getType());
        return rule.getId();
    }

    @Override
    public void updateBlacklistRule(Long ruleId, BlacklistSaveDTO dto) {
        StoreBlacklist rule = blacklistMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException("黑名单规则不存在");
        }
        applyRuleDto(rule, rule.getStoreId(), dto);
        blacklistMapper.updateById(rule);
        log.info("[AdminStore] FR-AD-005 编辑黑名单规则: id={}", ruleId);
    }

    @Override
    public void deleteBlacklistRule(Long ruleId) {
        int rows = blacklistMapper.delete(new LambdaQueryWrapper<StoreBlacklist>()
                .eq(StoreBlacklist::getId, ruleId));
        if (rows == 0) {
            throw new BusinessException("黑名单规则不存在");
        }
        log.info("[AdminStore] FR-AD-005 删除黑名单规则: id={}", ruleId);
    }

    @Override
    public List<AdminBlacklistVO.CalendarDay> getBlacklistCalendar(Long storeId, YearMonth month) {
        loadStore(storeId);
        YearMonth target = month == null ? YearMonth.now() : month;
        LocalDate today = LocalDate.now();

        // 一次性加载启用规则，Java 内按日匹配
        List<StoreBlacklist> rules = blacklistMapper.selectList(
                new LambdaQueryWrapper<StoreBlacklist>()
                        .eq(StoreBlacklist::getStoreId, storeId)
                        .eq(StoreBlacklist::getStatus, 1));

        List<AdminBlacklistVO.CalendarDay> days = new ArrayList<>();
        for (LocalDate date = target.atDay(1); date.getMonth() == target.getMonth();
             date = date.plusDays(1)) {
            AdminBlacklistVO.CalendarDay day = new AdminBlacklistVO.CalendarDay();
            day.setDate(date);
            day.setToday(date.equals(today));
            for (StoreBlacklist rule : rules) {
                if (!ruleMatchesDate(rule, date)) {
                    continue;
                }
                boolean allDay = rule.getStartTime() == null || rule.getEndTime() == null
                        || (rule.getStartTime().equals(LocalTime.MIN)
                            && rule.getEndTime().isAfter(ALL_DAY_END));
                if (allDay) {
                    day.setAllDayClosed(true);
                    continue;
                }
                AdminBlacklistVO.Slot slot = new AdminBlacklistVO.Slot();
                slot.setStartTime(rule.getStartTime());
                slot.setEndTime(rule.getEndTime());
                slot.setReason(rule.getReason());
                day.getSlots().add(slot);
            }
            if (!day.getAllDayClosed() && day.getSlots().isEmpty()) {
                day.setAllDayClosed(false);
            }
            days.add(day);
        }
        return days;
    }

    /**
     * 规则是否命中某日：单次按日期、周期性按周几（与用户端 FR-UC-020 判定一致）
     */
    private boolean ruleMatchesDate(StoreBlacklist rule, LocalDate date) {
        if (rule.getType() != null && rule.getType() == 2) {
            return date.equals(rule.getSingleDate());
        }
        if (rule.getType() != null && rule.getType() == 1 && StringUtils.hasText(rule.getWeekDays())) {
            int dow = date.getDayOfWeek().getValue(); // 1=周一 7=周日
            for (String s : rule.getWeekDays().split(",")) {
                if (Integer.parseInt(s.trim()) == dow) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * DTO → 规则实体（含 type 相关字段交叉校验）
     */
    private void applyRuleDto(StoreBlacklist rule, Long storeId, BlacklistSaveDTO dto) {
        // 交叉校验：周期性必须有周几、单次必须有日期
        if (dto.getType() == 1 && !StringUtils.hasText(dto.getWeekDays())) {
            throw new BusinessException("周期性规则必须选择生效周几");
        }
        if (dto.getType() == 2 && dto.getSingleDate() == null) {
            throw new BusinessException("单次规则必须选择日期");
        }
        // 时间区间校验（允许双空 = 全天闭店）
        if (dto.getStartTime() != null && dto.getEndTime() != null
                && !dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BusinessException("开始时间必须早于结束时间");
        }
        rule.setStoreId(storeId);
        rule.setType(dto.getType());
        rule.setWeekDays(dto.getType() == 1 ? dto.getWeekDays() : null);
        rule.setSingleDate(dto.getType() == 2 ? dto.getSingleDate() : null);
        rule.setStartTime(dto.getStartTime());
        rule.setEndTime(dto.getEndTime());
        rule.setReason(dto.getReason());
        rule.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    /* ==================== FR-AD-006：座位布局 ==================== */

    @Override
    public List<AdminSeatVO> listSeats(Long storeId) {
        loadStore(storeId);
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getStoreId, storeId)
                .orderByAsc(Seat::getFloor).orderByAsc(Seat::getSeatNo));
        if (seats.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> typeNames = seatTypeMapper.selectList(new LambdaQueryWrapper<SeatType>()
                        .eq(SeatType::getStatus, 1))
                .stream().collect(Collectors.toMap(SeatType::getId, SeatType::getName, (a, b) -> a));
        // 有进行中订单（含待支付锁座）的座位集合
        Set<Long> activeSeatIds = orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                        .select(Orders::getSeatId)
                        .eq(Orders::getStoreId, storeId)
                        .in(Orders::getStatus, Orders.OCCUPYING_STATUSES)
                        .gt(Orders::getEndTime, LocalDateTime.now()))
                .stream().map(Orders::getSeatId).collect(Collectors.toSet());

        return seats.stream().map(s -> {
            AdminSeatVO vo = new AdminSeatVO();
            vo.setId(s.getId());
            vo.setSeatNo(s.getSeatNo());
            vo.setSeatTypeId(s.getSeatTypeId());
            vo.setSeatTypeName(typeNames.getOrDefault(s.getSeatTypeId(), "未知类型"));
            vo.setAreaCode(s.getAreaCode());
            vo.setXCoord(s.getXCoord());
            vo.setYCoord(s.getYCoord());
            vo.setFloor(s.getFloor());
            vo.setDescription(s.getDescription());
            vo.setStatus(s.getStatus());
            vo.setStatusDesc(seatStatusDesc(s.getStatus()));
            vo.setHasActiveOrder(activeSeatIds.contains(s.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SeatMapVO.SeatTypeVO> listSeatTypes() {
        return seatTypeMapper.selectList(new LambdaQueryWrapper<SeatType>()
                        .eq(SeatType::getStatus, 1).orderByAsc(SeatType::getSort))
                .stream().map(t -> {
                    SeatMapVO.SeatTypeVO tv = new SeatMapVO.SeatTypeVO();
                    tv.setId(t.getId());
                    tv.setName(t.getName());
                    tv.setCode(t.getCode());
                    tv.setIcon(t.getIcon());
                    return tv;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSeatLayout(Long storeId, SeatLayoutSaveDTO dto) {
        loadStore(storeId);
        List<SeatLayoutSaveDTO.SeatItem> items = dto.getSeats() == null
                ? Collections.emptyList() : dto.getSeats();

        // 1. 画布内编号唯一性预校验（uk_store_seat_no 兜底）
        Set<String> nos = new HashSet<>();
        for (SeatLayoutSaveDTO.SeatItem item : items) {
            if (!nos.add(item.getSeatNo())) {
                throw new BusinessException("座位编号重复：" + item.getSeatNo());
            }
        }

        // 2. 座位类型存在性校验
        Set<Long> typeIds = items.stream().map(SeatLayoutSaveDTO.SeatItem::getSeatTypeId)
                .collect(Collectors.toSet());
        if (!typeIds.isEmpty()) {
            long validTypes = seatTypeMapper.selectCount(new LambdaQueryWrapper<SeatType>()
                    .in(SeatType::getId, typeIds).eq(SeatType::getStatus, 1));
            if (validTypes < typeIds.size()) {
                throw new BusinessException("存在无效或已下架的座位类型");
            }
        }

        // 3. 现有座位 → 计算删除集合（有进行中订单的座位不可删，整体拒绝）
        List<Seat> existing = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getStoreId, storeId));
        Map<Long, Seat> existingMap = existing.stream()
                .collect(Collectors.toMap(Seat::getId, s -> s));
        Set<Long> payloadIds = items.stream().map(SeatLayoutSaveDTO.SeatItem::getId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        List<Long> toDeleteIds = existing.stream().map(Seat::getId)
                .filter(id -> !payloadIds.contains(id)).collect(Collectors.toList());
        if (!toDeleteIds.isEmpty()) {
            Long blocked = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .in(Orders::getSeatId, toDeleteIds)
                    .in(Orders::getStatus, Orders.OCCUPYING_STATUSES)
                    .gt(Orders::getEndTime, LocalDateTime.now()));
            if (blocked != null && blocked > 0) {
                throw new BusinessException("有 " + blocked + " 个待删除座位存在未完成订单，"
                        + "请等订单结束或先处理订单再调整布局");
            }
        }

        try {
            // 4. upsert：带 id 更新、不带 id 插入
            for (SeatLayoutSaveDTO.SeatItem item : items) {
                Seat seat = item.getId() != null ? existingMap.get(item.getId()) : null;
                boolean isInsert = seat == null;
                if (isInsert) {
                    seat = new Seat();
                    seat.setId(item.getId()); // null 由 ASSIGN_ID 生成
                }
                seat.setStoreId(storeId);
                seat.setSeatNo(item.getSeatNo());
                seat.setSeatTypeId(item.getSeatTypeId());
                seat.setAreaCode(item.getAreaCode());
                seat.setXCoord(item.getXCoord());
                seat.setYCoord(item.getYCoord());
                seat.setFloor(StringUtils.hasText(item.getFloor()) ? item.getFloor() : "1F");
                seat.setDescription(item.getDescription());
                if (isInsert) {
                    seat.setStatus(0); // 新增默认空闲
                    seatMapper.insert(seat);
                } else {
                    seatMapper.updateById(seat);
                }
            }
            // 5. 缺失座位软删除（@TableLogic）
            if (!toDeleteIds.isEmpty()) {
                seatMapper.deleteBatchIds(toDeleteIds);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("座位编号已存在（门店内唯一），请检查编号后重试");
        }
        log.info("[AdminStore] FR-AD-006 座位布局保存: storeId={}, upsert={}, delete={}",
                storeId, items.size(), toDeleteIds.size());
    }

    /* ==================== FR-AD-007：座位状态管理 ==================== */

    @Override
    public void changeSeatStatus(Long seatId, Integer targetStatus) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException("座位不存在");
        }
        int current = seat.getStatus() == null ? 0 : seat.getStatus();
        // 允许的迁移：0→2/4、2→0、3→0、4→0；1使用中由订单驱动不可手动切
        boolean allowed = (current == 0 && (targetStatus == 2 || targetStatus == 4))
                || ((current == 2 || current == 3 || current == 4) && targetStatus == 0);
        if (current == 1) {
            throw new BusinessException("使用中座位状态由订单驱动（签到/离场结算自动流转），不可手动切换");
        }
        if (!allowed) {
            throw new BusinessException("不支持的状态切换：" + seatStatusDesc(current)
                    + " → " + seatStatusDesc(targetStatus));
        }
        // 空闲→维护/锁定：若存在未完成订单（待支付/已预约/进行中）则拒绝
        if (targetStatus == 2 || targetStatus == 4) {
            Long active = orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                    .eq(Orders::getSeatId, seatId)
                    .in(Orders::getStatus, Orders.OCCUPYING_STATUSES)
                    .gt(Orders::getEndTime, LocalDateTime.now()));
            if (active != null && active > 0) {
                throw new BusinessException("该座位有未完成订单（含待支付锁座），请先处理订单再置"
                        + (targetStatus == 2 ? "维护" : "锁定"));
            }
        }
        // CAS 防与签到(0→1)/离场(1→3)并发冲突
        int rows = seatMapper.casUpdateStatus(seatId, current, targetStatus);
        if (rows == 0) {
            throw new BusinessException("座位状态已变更，请刷新后重试");
        }
        log.info("[AdminStore] FR-AD-007 座位状态切换: seatId={}, {} → {}",
                seatId, seatStatusDesc(current), seatStatusDesc(targetStatus));
    }

    /* ==================== FR-AD-008：签到二维码 ==================== */

    @Override
    public AdminQrCodeVO generateQrCode(Long storeId, int validHours) {
        Store store = loadStore(storeId);
        if (validHours < 1 || validHours > 72) {
            throw new BusinessException("有效期仅支持 1~72 小时");
        }
        // 复用 FR-UC-030 签到码生成（AES加密 + HMAC签名 + 有效期）
        String code = checkInService.generateStoreQr(storeId, validHours);
        AdminQrCodeVO vo = new AdminQrCodeVO();
        vo.setCode(code);
        vo.setExpireAt(LocalDateTime.now().plusHours(validHours));
        vo.setStoreName(store.getName());
        vo.setStoreId(storeId);
        log.info("[AdminStore] FR-AD-008 生成签到码: storeId={}, validHours={}", storeId, validHours);
        return vo;
    }

    /* ==================== 私有辅助 ==================== */

    private Store loadStore(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null || store.getDeleted() == 1) {
            throw new BusinessException("门店不存在");
        }
        return store;
    }

    private AdminStoreVO toStoreVO(Store store, Long seatCount) {
        AdminStoreVO vo = new AdminStoreVO();
        vo.setId(store.getId());
        vo.setName(store.getName());
        vo.setAddress(store.getAddress());
        vo.setPhone(store.getPhone());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setLatitude(store.getLatitude());
        vo.setLongitude(store.getLongitude());
        vo.setDescription(store.getDescription());
        vo.setFacilities(parseJsonArray(store.getFacilities()));
        vo.setImages(parseJsonArray(store.getImages()));
        vo.setRating(store.getRating());
        vo.setStatus(store.getStatus());
        vo.setStatusDesc(storeStatusDesc(store.getStatus()));
        vo.setSort(store.getSort());
        vo.setSeatCount(seatCount);
        vo.setCreateTime(store.getCreateTime());
        return vo;
    }

    private AdminBlacklistVO toBlacklistVO(StoreBlacklist rule) {
        AdminBlacklistVO vo = new AdminBlacklistVO();
        vo.setId(rule.getId());
        vo.setStoreId(rule.getStoreId());
        vo.setType(rule.getType());
        vo.setTypeDesc(rule.getType() != null && rule.getType() == 1 ? "周期性" : "单次");
        vo.setWeekDays(rule.getWeekDays());
        vo.setWeekDaysDesc(weekDaysDesc(rule.getWeekDays()));
        vo.setSingleDate(rule.getSingleDate());
        vo.setStartTime(rule.getStartTime());
        vo.setEndTime(rule.getEndTime());
        vo.setReason(rule.getReason());
        vo.setStatus(rule.getStatus());
        vo.setStatusDesc(rule.getStatus() != null && rule.getStatus() == 1 ? "启用" : "禁用");
        vo.setCreateTime(rule.getCreateTime());
        return vo;
    }

    /**
     * "1,3,5" → "周一,周三,周五"
     */
    private String weekDaysDesc(String weekDays) {
        if (!StringUtils.hasText(weekDays)) {
            return "";
        }
        String[] names = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<String> result = new ArrayList<>();
        for (String s : weekDays.split(",")) {
            int v = Integer.parseInt(s.trim());
            if (v >= 1 && v <= 7) {
                result.add(names[v - 1]);
            }
        }
        return String.join("、", result);
    }

    private String storeStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "休息中";
            case 1: return "营业中";
            case 2: return "维护中";
            default: return "未知";
        }
    }

    private String seatStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "空闲";
            case 1: return "使用中";
            case 2: return "维护";
            case 3: return "打扫中";
            case 4: return "锁定";
            default: return "未知";
        }
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(list);
    }

    private List<String> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json).toJavaList(String.class);
        } catch (Exception e) {
            log.warn("[AdminStore] JSON 数组解析失败: {}", json);
            return Collections.emptyList();
        }
    }
}
