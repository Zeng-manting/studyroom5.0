package com.studyroom.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import com.studyroom.entity.config.SystemConfig;
import com.studyroom.mapper.config.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端系统配置服务 —— FR-AD-026
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>FR-AD-026 退款规则配置：免费取消时限 / 各时段退款比例（refund 分组）</li>
 *   <li>顺带覆盖 order/sign/pause/credit 等其他配置组的读写</li>
 * </ul>
 *
 * <h3>重要说明</h3>
 * <p>当前版本业务侧退款规则仍读取编译期常量（CommonConstants）；
 * system_config 为运营动态化预留 —— 后续 PricingService/退款链路可切换为
 * 读取本表（带本地缓存）。保存即生效范围以此为准。</p>
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminConfigService {

    private final SystemConfigMapper systemConfigMapper;

    /**
     * 按分组读取配置（group 空 = 全部），返回 分组 → [key, value, description] 列表
     */
    public Map<String, List<SystemConfig>> listByGroup(String group) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        if (group != null && !group.isEmpty()) {
            wrapper.eq(SystemConfig::getConfigGroup, group);
        }
        wrapper.orderByAsc(SystemConfig::getConfigGroup).orderByAsc(SystemConfig::getId);
        Map<String, List<SystemConfig>> result = new LinkedHashMap<>();
        for (SystemConfig config : systemConfigMapper.selectList(wrapper)) {
            result.computeIfAbsent(config.getConfigGroup(), k -> new java.util.ArrayList<>())
                    .add(config);
        }
        return result;
    }

    /**
     * 批量保存配置值（FR-AD-026）
     *
     * <p>只允许更新已存在的键（防止误建垃圾键）；值合法性由前端 + 业务侧二次确认。</p>
     *
     * @param values key → value 映射
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveValues(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "没有需要保存的配置");
        }
        int updated = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            SystemConfig existing = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, entry.getKey()));
            if (existing == null) {
                log.warn("[AdminConfig] 跳过不存在的配置键: {}", entry.getKey());
                continue;
            }
            if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "配置「" + existing.getDescription() + "」的值不能为空");
            }
            SystemConfig patch = new SystemConfig();
            patch.setId(existing.getId());
            patch.setConfigValue(entry.getValue().trim());
            systemConfigMapper.updateById(patch);
            updated++;
        }
        if (updated == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "没有匹配到任何有效配置键");
        }
        log.info("[AdminConfig] FR-AD-026 批量保存配置: {} 项", updated);
    }
}
