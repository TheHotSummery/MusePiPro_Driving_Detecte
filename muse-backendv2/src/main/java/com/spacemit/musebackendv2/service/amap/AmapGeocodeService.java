package com.spacemit.musebackendv2.service.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 高德地图逆地理编码服务
 * 用于将GPS坐标转换为地址信息
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmapGeocodeService {

    @Value("${amap.api.key:}")
    private String apiKey;

    @Value("${amap.api.base-url:https://restapi.amap.com/v3}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 逆地理编码 - 将坐标转换为地址
     * 
     * @param lat 纬度（GCJ02）
     * @param lng 经度（GCJ02）
     * @return 地址信息，格式：{address: "详细地址", region: "省市区"}
     */
    @Async
    public Map<String, String> reverseGeocode(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return null;
        }

        // 检查API Key是否配置
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_AMAP_API_KEY")) {
            log.debug("高德地图API Key未配置，跳过逆地理编码");
            return null;
        }

        try {
            String url = String.format("%s/geocode/regeo?key=%s&location=%s,%s&output=json&radius=1000&extensions=base",
                    baseUrl, apiKey, lng, lat); // 注意：高德API是 经度,纬度

            log.debug("调用高德地图逆地理编码API: lat={}, lng={}", lat, lng);

            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                log.warn("高德地图API返回空响应");
                return null;
            }

            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 检查状态码
            String status = jsonNode.path("status").asText();
            if (!"1".equals(status)) {
                String info = jsonNode.path("info").asText();
                log.warn("高德地图API调用失败: status={}, info={}", status, info);
                return null;
            }

            JsonNode regeocode = jsonNode.path("regeocode");
            if (regeocode.isMissingNode()) {
                log.warn("高德地图API返回数据格式错误：缺少regeocode字段");
                return null;
            }

            // 获取详细地址
            String formattedAddress = regeocode.path("formatted_address").asText("");
            
            // 获取地址组件（省市区）
            JsonNode addressComponent = regeocode.path("addressComponent");
            String province = addressComponent.path("province").asText("");
            String city = addressComponent.path("city").asText("");
            String district = addressComponent.path("district").asText("");
            
            // 构建区域信息：省 市 区
            String region = "";
            if (!province.isEmpty()) {
                region = province;
            }
            if (!city.isEmpty() && !city.equals(province)) {
                region += (region.isEmpty() ? "" : " ") + city;
            }
            if (!district.isEmpty()) {
                region += (region.isEmpty() ? "" : " ") + district;
            }

            Map<String, String> result = new HashMap<>();
            result.put("address", formattedAddress);
            result.put("region", region);

            log.debug("逆地理编码成功: address={}, region={}", formattedAddress, region);

            return result;
        } catch (Exception e) {
            log.error("调用高德地图逆地理编码API失败: lat={}, lng={}", lat, lng, e);
            return null;
        }
    }

    /**
     * 同步版本的逆地理编码（用于需要立即获取结果的场景）
     * 注意：这个方法会阻塞，等待API响应
     */
    public Map<String, String> reverseGeocodeSync(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return null;
        }

        // 检查API Key是否配置
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_AMAP_API_KEY")) {
            log.debug("高德地图API Key未配置，跳过逆地理编码");
            return null;
        }

        try {
            String url = String.format("%s/geocode/regeo?key=%s&location=%s,%s&output=json&radius=1000&extensions=base",
                    baseUrl, apiKey, lng, lat); // 注意：高德API是 经度,纬度

            log.debug("调用高德地图逆地理编码API（同步）: lat={}, lng={}", lat, lng);

            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                log.warn("高德地图API返回空响应");
                return null;
            }

            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 检查状态码
            String status = jsonNode.path("status").asText();
            if (!"1".equals(status)) {
                String info = jsonNode.path("info").asText();
                log.warn("高德地图API调用失败: status={}, info={}", status, info);
                return null;
            }

            JsonNode regeocode = jsonNode.path("regeocode");
            if (regeocode.isMissingNode()) {
                log.warn("高德地图API返回数据格式错误：缺少regeocode字段");
                return null;
            }

            // 获取详细地址
            String formattedAddress = regeocode.path("formatted_address").asText("");
            
            // 获取地址组件（省市区）
            JsonNode addressComponent = regeocode.path("addressComponent");
            String province = addressComponent.path("province").asText("");
            String city = addressComponent.path("city").asText("");
            String district = addressComponent.path("district").asText("");
            
            // 构建区域信息：省 市 区
            String region = "";
            if (!province.isEmpty()) {
                region = province;
            }
            if (!city.isEmpty() && !city.equals(province)) {
                region += (region.isEmpty() ? "" : " ") + city;
            }
            if (!district.isEmpty()) {
                region += (region.isEmpty() ? "" : " ") + district;
            }

            Map<String, String> result = new HashMap<>();
            result.put("address", formattedAddress);
            result.put("region", region);

            log.debug("逆地理编码成功（同步）: address={}, region={}", formattedAddress, region);

            return result;
        } catch (Exception e) {
            log.error("调用高德地图逆地理编码API失败（同步）: lat={}, lng={}", lat, lng, e);
            return null;
        }
    }
}

