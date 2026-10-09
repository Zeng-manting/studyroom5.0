package com.studyroom.service.store;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.store.Store;
import com.studyroom.entity.store.StoreBlacklist;
import com.studyroom.entity.vo.order.StoreBlacklistVO;
import com.studyroom.entity.vo.order.StoreDetailVO;
import com.studyroom.entity.vo.order.StoreListVO;
import com.studyroom.mapper.store.StoreBlacklistMapper;
import com.studyroom.mapper.store.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 门店服务实现（FR-UC-007~015 / FR-UC-020）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreMapper storeMapper;
    private final StoreBlacklistMapper blacklistMapper;

    @Override
    public List<StoreListVO> listStores(String keyword) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        // FR-UC-008：过滤维护中门店；休息中仍展示（用户可见"休息中"标签）
        wrapper.ne(Store::getStatus, 2)
                .eq(Store::getDeleted, 0)
                .orderByAsc(Store::getSort)
                .orderByDesc(Store::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Store::getName, keyword).or().like(Store::getAddress, keyword));
        }
        List<Store> stores = storeMapper.selectList(wrapper);
        return stores.stream().map(this::toListVO).collect(Collectors.toList());
    }

    @Override
    public StoreDetailVO getDetail(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null || store.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "门店不存在");
        }
        StoreDetailVO vo = new StoreDetailVO();
        vo.setId(store.getId());
        vo.setName(store.getName());
        vo.setAddress(store.getAddress());
        vo.setPhone(store.getPhone());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setLatitude(store.getLatitude());
        vo.setLongitude(store.getLongitude());
        vo.setDescription(store.getDescription());
        vo.setRating(store.getRating());
        vo.setStatus(store.getStatus());
        vo.setStatusDesc(statusDesc(store.getStatus()));
        vo.setImageList(parseJsonArray(store.getImages()));
        vo.setFacilityList(parseJsonArray(store.getFacilities()));
        return vo;
    }

    @Override
    public StoreBlacklistVO getBlacklist(Long storeId, LocalDate date) {
        StoreBlacklistVO vo = new StoreBlacklistVO();
        vo.setDate(date);
        vo.setSlots(new ArrayList<>());
        vo.setAllDayClosed(false);

        List<StoreBlacklist> rules = listEffectiveRules(storeId, date);
        for (StoreBlacklist rule : rules) {
            // 全天闭店：00:00-23:59 视为全天
            if (rule.getStartTime() == null || rule.getEndTime() == null
                    || (rule.getStartTime().equals(LocalTime.MIN)
                        && rule.getEndTime().isAfter(LocalTime.of(23, 58)))) {
                vo.setAllDayClosed(true);
                continue;
            }
            StoreBlacklistVO.Slot slot = new StoreBlacklistVO.Slot();
            slot.setStartTime(rule.getStartTime());
            slot.setEndTime(rule.getEndTime());
            slot.setReason(rule.getReason());
            vo.getSlots().add(slot);
        }
        return vo;
    }

    @Override
    public boolean isBlocked(Long storeId, LocalDate date, LocalTime start, LocalTime end) {
        List<StoreBlacklist> rules = listEffectiveRules(storeId, date);
        for (StoreBlacklist rule : rules) {
            if (rule.getStartTime() == null || rule.getEndTime() == null) {
                return true;
            }
            // 时段重叠判定
            if (rule.getStartTime().isBefore(end) && rule.getEndTime().isAfter(start)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询某日生效的黑名单规则（FR-UC-020：周期性按周几匹配 + 单次按日期匹配）
     */
    private List<StoreBlacklist> listEffectiveRules(Long storeId, LocalDate date) {
        LambdaQueryWrapper<StoreBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreBlacklist::getStoreId, storeId)
                .eq(StoreBlacklist::getStatus, 1)
                .and(w -> w
                        // 单次：具体日期命中
                        .eq(StoreBlacklist::getType, 2).eq(StoreBlacklist::getSingleDate, date)
                        // 周期性：周几命中
                        .or(b -> b.eq(StoreBlacklist::getType, 1)
                                .apply("FIND_IN_SET({0}, week_days)", dayOfWeekValue(date))));
        return blacklistMapper.selectList(wrapper);
    }

    /**
     * MySQL 周几：1=周一 7=周日（与 week_days 字段约定一致）
     */
    private int dayOfWeekValue(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow.getValue();
    }

    private StoreListVO toListVO(Store store) {
        StoreListVO vo = new StoreListVO();
        vo.setId(store.getId());
        vo.setName(store.getName());
        vo.setAddress(store.getAddress());
        vo.setRating(store.getRating());
        vo.setStatus(store.getStatus());
        vo.setStatusDesc(statusDesc(store.getStatus()));
        vo.setBusinessHours(store.getBusinessHours());
        List<String> images = parseJsonArray(store.getImages());
        vo.setCoverImage(images.isEmpty() ? null : images.get(0));
        vo.setFacilityList(parseJsonArray(store.getFacilities()));
        return vo;
    }

    private String statusDesc(Integer status) {
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

    /**
     * 解析 JSON 数组字符串为 List（null 安全）
     */
    private List<String> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = JSON.parseArray(json);
            return array.toJavaList(String.class);
        } catch (Exception e) {
            log.warn("[StoreService] JSON 数组解析失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
