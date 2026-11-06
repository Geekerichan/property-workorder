package com.prop.wo.common.util;

import java.math.BigDecimal;

/**
 * 经纬度距离计算（米），用于定位打卡半径校验
 */
public final class GeoUtil {

    private static final double EARTH_RADIUS = 6371000d;

    private GeoUtil() {
    }

    public static Integer distanceMeter(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2) {
        if (lon1 == null || lat1 == null || lon2 == null || lat2 == null) {
            return null;
        }
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double deltaLat = radLat1 - radLat2;
        double deltaLon = Math.toRadians(lon1.doubleValue()) - Math.toRadians(lon2.doubleValue());
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.asin(Math.min(1, Math.sqrt(a)));
        return (int) Math.round(EARTH_RADIUS * c);
    }
}
