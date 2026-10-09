package com.studyroom.service.sign;

import com.studyroom.entity.dto.order.AppealCreateDTO;
import com.studyroom.entity.dto.order.CheckInDTO;
import com.studyroom.entity.vo.order.CheckInResultVO;

/**
 * 自助签到服务（FR-UC-029 ~ FR-UC-032）
 *
 * <h3>签到二维码格式（与 FR-AD-008 管理端生成约定）</h3>
 * <pre>
 * 二维码全文：SR-SIGN:{AES密文Base64}:{HMAC签名Base64}
 *
 * AES 密文明文（JSON）：
 * {"t":1,"sid":1001,"nonce":"a1b2c3d4","exp":1760000000}
 *   t    固定 1，表示签到码类型
 *   sid  门店 ID（③ 校验订单归属门店）
 *   nonce 随机串（防重放审计）
 *   exp  过期时间戳（秒，管理端生成时默认 +24h，可刷新）
 * </pre>
 *
 * <h3>五重校验（FR-UC-030）</h3>
 * <ol>
 *   <li>① AES-256-CBC 解密（失败 → 二维码无效）</li>
 *   <li>② HMAC-SHA256 验签（常量时间比较，防篡改）+ 二维码有效期 exp</li>
 *   <li>③ 查有效订单：当前用户 + 该门店 + 已支付未签到（status=1）</li>
 *   <li>④ 时间窗口：[开始-15min, 开始+15min]</li>
 *   <li>⑤ GPS 距离：Haversine ≤ 100 米</li>
 * </ol>
 *
 * @author studyroom
 */
public interface CheckInService {

    /**
     * 扫码签到（FR-UC-029/030/031/032）
     * <p>成功：订单 1→2 进行中、记录签到时间/GPS、座位转使用中；
     * 失败：落 order_sign 失败留痕并返回原因（appealable=true）。</p>
     */
    CheckInResultVO checkIn(Long userId, CheckInDTO dto);

    /**
     * 提交签到申诉（FR-UC-032：原因 + 文字说明 + 凭证图片）
     * <p>同一订单存在待审核申诉时拒绝重复提交；orders.appeal_status 0→1。</p>
     *
     * @return 申诉ID
     */
    Long submitAppeal(Long userId, AppealCreateDTO dto);

    /**
     * 生成门店签到二维码内容（FR-AD-008 管理端复用：生成/刷新/下载）
     *
     * @param storeId     门店 ID
     * @param validHours  有效期（小时）
     * @return 二维码全文 SR-SIGN:密文:签名
     */
    String generateStoreQr(Long storeId, int validHours);
}
