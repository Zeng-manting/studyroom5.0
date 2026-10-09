package com.studyroom.service.admin;

import com.studyroom.common.result.PageResult;
import com.studyroom.entity.dto.admin.CouponSaveDTO;
import com.studyroom.entity.dto.admin.CouponGrantDTO;
import com.studyroom.entity.vo.admin.AdminCouponVO;

import java.util.List;

/**
 * 管理端优惠券配置服务 —— FR-AD-027
 *
 * <h3>FR 映射</h3>
 * <ul>
 *   <li>新建优惠券：新人券/时长抵扣券/满减券，定额或折扣，固定/相对有效期</li>
 *   <li>发放：按用户ID列表定向发放（写 coupon_claim）</li>
 *   <li>下架/上架：下架后用户端不可再领取（已领取的继续可用）</li>
 * </ul>
 *
 * @author studyroom
 */
public interface AdminCouponService {

    /**
     * 优惠券分页列表（含下架）
     *
     * @param type   类型：1新人券 2时长抵扣券 3满减券（空=全部）
     * @param status 状态：0下架 1上架（空=全部）
     */
    PageResult<AdminCouponVO> pageCoupons(Integer type, Integer status, long page, long size);

    /**
     * 新建/编辑优惠券（id 空=新建）
     *
     * @return 优惠券ID
     */
    Long saveCoupon(CouponSaveDTO dto, Long operatorId);

    /**
     * 上架/下架（下架后不可再领取，已领取的继续可用）
     */
    void toggleStatus(Long couponId, boolean online);

    /**
     * 定向发放（FR-AD-027）：按用户ID列表写入 coupon_claim
     *
     * @return 实际发放成功人数（跳过不存在/超限用户）
     */
    int grantCoupon(CouponGrantDTO dto);

    /**
     * 某优惠券的领取明细（最近 100 条，发放效果跟踪）
     */
    List<AdminCouponVO.ClaimRecordItem> listClaims(Long couponId);
}
