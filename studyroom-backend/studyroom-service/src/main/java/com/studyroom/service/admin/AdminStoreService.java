package com.studyroom.service.admin;

import com.studyroom.entity.dto.admin.BlacklistSaveDTO;
import com.studyroom.entity.dto.admin.SeatLayoutSaveDTO;
import com.studyroom.entity.dto.admin.StoreSaveDTO;
import com.studyroom.entity.vo.admin.AdminBlacklistVO;
import com.studyroom.entity.vo.admin.AdminQrCodeVO;
import com.studyroom.entity.vo.admin.AdminSeatVO;
import com.studyroom.entity.vo.admin.AdminStoreVO;
import com.studyroom.entity.vo.order.SeatMapVO;

import java.time.YearMonth;
import java.util.List;

/**
 * 管理端门店服务 —— FR-AD-004 ~ FR-AD-008
 *
 * <h3>功能映射</h3>
 * <ul>
 *   <li>FR-AD-004 编辑门店信息：名称、地址、营业时间、GPS、设施标签、环境图片（含新增门店）</li>
 *   <li>FR-AD-005 时段黑名单设置：周期性/单次规则 CRUD + 月历视图</li>
 *   <li>FR-AD-006 座位布局可视化编辑：画布座位全量同步保存（新增/更新/删除）</li>
 *   <li>FR-AD-007 座位状态管理：维护/恢复空闲/打扫完成手动切换（CAS 防并发）</li>
 *   <li>FR-AD-008 签到二维码管理：AES+HMAC 签到码生成（渲染/下载/打印由前端完成）</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminStoreService {

    /* ==================== FR-AD-004：门店信息 ==================== */

    /** 门店列表（含维护中，管理端视角） */
    List<AdminStoreVO> listStores();

    /** 门店详情（含设施/图片 JSON 解析与座位数） */
    AdminStoreVO getStore(Long storeId);

    /** 新增门店 */
    Long createStore(StoreSaveDTO dto);

    /** 编辑门店（FR-AD-004） */
    void updateStore(Long storeId, StoreSaveDTO dto);

    /* ==================== FR-AD-005：时段黑名单 ==================== */

    /** 黑名单规则列表（全部，含禁用） */
    List<AdminBlacklistVO> listBlacklistRules(Long storeId);

    /** 新增黑名单规则 */
    Long addBlacklistRule(Long storeId, BlacklistSaveDTO dto);

    /** 编辑黑名单规则 */
    void updateBlacklistRule(Long ruleId, BlacklistSaveDTO dto);

    /** 删除黑名单规则 */
    void deleteBlacklistRule(Long ruleId);

    /**
     * 月历视图（FR-AD-005 日历视图展示）
     *
     * @param month 查询月份（yyyy-MM），空 = 当月
     * @return 当月每日命中情况（周期性按周几匹配 + 单次按日期匹配）
     */
    List<AdminBlacklistVO.CalendarDay> getBlacklistCalendar(Long storeId, YearMonth month);

    /* ==================== FR-AD-006：座位布局 ==================== */

    /** 座位列表（管理端画布数据：真实状态 + 座位类型 + 是否有进行中订单） */
    List<AdminSeatVO> listSeats(Long storeId);

    /** 座位类型列表（画布图例/编辑选项；复用用户端 SeatMapVO.SeatTypeVO） */
    List<SeatMapVO.SeatTypeVO> listSeatTypes();

    /**
     * 座位布局全量同步保存（FR-AD-006 拖拽编辑画布落库）
     * <p>payload 带 id 更新、不带 id 插入；缺失座位软删除——
     * 但存在关联进行中订单（status IN 0,1,2）的座位不允许删除，整体拒绝保证一致性。</p>
     */
    void saveSeatLayout(Long storeId, SeatLayoutSaveDTO dto);

    /* ==================== FR-AD-007：座位状态管理 ==================== */

    /**
     * 手动切换座位状态（FR-AD-007）
     * <p>允许：空闲→维护/锁定、维护→空闲、打扫中→空闲、锁定→空闲；
     * 使用中(1)由订单驱动不可手动切换；空闲→维护/锁定时若有未完成订单则拒绝。</p>
     *
     * @param seatId       座位ID
     * @param targetStatus 目标状态（0/2/4）
     */
    void changeSeatStatus(Long seatId, Integer targetStatus);

    /* ==================== FR-AD-008：签到二维码 ==================== */

    /**
     * 生成门店签到码（FR-AD-008：AES加密 + HMAC签名，格式 SR-SIGN:{密文}:{签名}）
     * <p>返回 QR 内容字符串 + 有效期至时间；前端渲染成图、下载、打印。</p>
     *
     * @param storeId    门店ID
     * @param validHours 有效小时数（1~72）
     */
    AdminQrCodeVO generateQrCode(Long storeId, int validHours);
}
