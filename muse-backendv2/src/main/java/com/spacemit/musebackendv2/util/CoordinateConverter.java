package com.spacemit.musebackendv2.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 坐标转换工具类
 * WGS84（GPS原始坐标） -> GCJ02（高德地图坐标）
 * 
 * 说明：
 * - 硬件设备传过来的GPS坐标是WGS84格式（国际标准）
 * - 中国大陆地区需要使用GCJ02坐标系（火星坐标系）
 * - 高德地图API使用GCJ02坐标系
 */
@Component
@Slf4j
public class CoordinateConverter {

    private static final double PI = 3.1415926535897932384626;
    private static final double A = 6378245.0; // 长半轴
    private static final double EE = 0.00669342162296594323; // 偏心率平方

    /**
     * WGS84转GCJ02
     * 
     * @param wgsLat WGS84纬度
     * @param wgsLng WGS84经度
     * @return double数组 [GCJ02纬度, GCJ02经度]
     */
    public double[] wgs84ToGcj02(double wgsLat, double wgsLng) {
        if (outOfChina(wgsLat, wgsLng)) {
            // 不在中国范围内，不需要转换
            return new double[]{wgsLat, wgsLng};
        }
        
        double dLat = transformLat(wgsLng - 105.0, wgsLat - 35.0);
        double dLng = transformLng(wgsLng - 105.0, wgsLat - 35.0);
        double radLat = wgsLat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        double gcjLat = wgsLat + dLat;
        double gcjLng = wgsLng + dLng;
        
        return new double[]{gcjLat, gcjLng};
    }

    /**
     * WGS84转GCJ02（BigDecimal版本）
     * 
     * @param wgsLat WGS84纬度
     * @param wgsLng WGS84经度
     * @return BigDecimal数组 [GCJ02纬度, GCJ02经度]
     */
    public BigDecimal[] wgs84ToGcj02(BigDecimal wgsLat, BigDecimal wgsLng) {
        if (wgsLat == null || wgsLng == null) {
            return new BigDecimal[]{wgsLat, wgsLng};
        }
        
        double[] gcj = wgs84ToGcj02(wgsLat.doubleValue(), wgsLng.doubleValue());
        return new BigDecimal[]{
            BigDecimal.valueOf(gcj[0]).setScale(6, RoundingMode.HALF_UP),
            BigDecimal.valueOf(gcj[1]).setScale(6, RoundingMode.HALF_UP)
        };
    }

    /**
     * 判断是否在中国范围外
     */
    private boolean outOfChina(double lat, double lng) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    /**
     * 纬度转换
     */
    private double transformLat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 经度转换
     */
    private double transformLng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
}
















