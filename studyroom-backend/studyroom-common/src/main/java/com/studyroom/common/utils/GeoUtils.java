package com.studyroom.common.utils;

import java.math.BigDecimal;

/**
 * 地理位置工具类
 * <p>FR-UC-030 五重校验之⑤：GPS 距离计算（Haversine 球面距离公式，地球半径 6371km）。</p>
 *
 * @author studyroom
 */
public final class GeoUtils {

    /** 地球平均半径（千米） */
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * 计算两个 GPS 坐标之间的球面距离（米）
     *
     * @param lat1 纬度1
     * @param lng1 经度1
     * @param lat2 纬度2
     * @param lng2 经度2
     * @return 距离（米，四舍五入取整）
     */
    public static int distanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return -1;
        }
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double meters = EARTH_RADIUS_KM * c * 1000;
        return (int) Math.round(meters);
    }
}
