package com.studyroom.service.admin;

import com.studyroom.entity.dto.admin.PackageSaveDTO;
import com.studyroom.entity.dto.admin.PriceRuleSaveDTO;
import com.studyroom.entity.dto.admin.SeatTypeSaveDTO;
import com.studyroom.entity.vo.admin.AdminPriceVO;

import java.util.List;

/**
 * 管理端价格配置服务 —— FR-AD-025
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>套餐价格：套餐 CRUD + 上/下架（用户端 FR-UC-019 套餐列表数据源）</li>
 *   <li>时段加价规则：price_rule CRUD（PricingService 计价引擎数据源）</li>
 *   <li>座位类型：seat_type CRUD（补模块 12 遗留）</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminPricingService {

    /* ---------- 套餐 ---------- */

    /** 套餐列表（含下架） */
    List<AdminPriceVO> listPackages();

    /** 新建/编辑套餐（id 空=新建），返回套餐ID */
    Long savePackage(PackageSaveDTO dto);

    /** 套餐上/下架 */
    void togglePackageStatus(Long packageId, boolean online);

    /* ---------- 时段加价规则 ---------- */

    /** 加价规则列表（含禁用，带类型/套餐名补齐） */
    List<AdminPriceVO> listPriceRules();

    /** 新建/编辑加价规则，返回规则ID */
    Long savePriceRule(PriceRuleSaveDTO dto);

    /** 删除规则（物理删除） */
    void deletePriceRule(Long ruleId);

    /* ---------- 座位类型 ---------- */

    /** 座位类型列表（含下架，含各类型座位数） */
    List<AdminPriceVO> listSeatTypes();

    /** 新建/编辑座位类型，返回类型ID */
    Long saveSeatType(SeatTypeSaveDTO dto);
}
