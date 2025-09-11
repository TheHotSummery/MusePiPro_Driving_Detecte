package com.spacemit.musebackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 内存缓存作为Redis的降级方案
    private final ConcurrentHashMap<String, Object> memoryCache = new ConcurrentHashMap<>();
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, value, timeout, unit);
                log.debug("Redis缓存设置成功: key={}", key);
            } else {
                // 使用内存缓存
                memoryCache.put(key, value);
                log.debug("内存缓存设置成功: key={}", key);
            }
        } catch (Exception e) {
            log.warn("缓存设置失败，使用内存缓存: key={}, error={}", key, e.getMessage());
            memoryCache.put(key, value);
        }
    }
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            if (redisTemplate != null) {
                Object value = redisTemplate.opsForValue().get(key);
                log.debug("Redis缓存获取成功: key={}", key);
                return value;
            } else {
                // 使用内存缓存
                Object value = memoryCache.get(key);
                log.debug("内存缓存获取成功: key={}", key);
                return value;
            }
        } catch (Exception e) {
            log.warn("缓存获取失败，使用内存缓存: key={}, error={}", key, e.getMessage());
            return memoryCache.get(key);
        }
    }
    
    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
                log.debug("Redis缓存删除成功: key={}", key);
            } else {
                memoryCache.remove(key);
                log.debug("内存缓存删除成功: key={}", key);
            }
        } catch (Exception e) {
            log.warn("缓存删除失败，使用内存缓存: key={}, error={}", key, e.getMessage());
            memoryCache.remove(key);
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        try {
            if (redisTemplate != null) {
                Boolean exists = redisTemplate.hasKey(key);
                return exists != null && exists;
            } else {
                return memoryCache.containsKey(key);
            }
        } catch (Exception e) {
            log.warn("缓存检查失败，使用内存缓存: key={}, error={}", key, e.getMessage());
            return memoryCache.containsKey(key);
        }
    }
    
    /**
     * 设置设备状态缓存
     */
    public void setDeviceStatus(String deviceId, String status) {
        String key = "device:status:" + deviceId;
        set(key, status, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 获取设备状态缓存
     */
    public String getDeviceStatus(String deviceId) {
        String key = "device:status:" + deviceId;
        Object status = get(key);
        return status != null ? status.toString() : null;
    }
    
    /**
     * 设置实时数据缓存
     */
    public void setRealtimeData(String deviceId, Object data) {
        String key = "realtime:data:" + deviceId;
        set(key, data, 1, TimeUnit.MINUTES);
    }
    
    /**
     * 获取实时数据缓存
     */
    public Object getRealtimeData(String deviceId) {
        String key = "realtime:data:" + deviceId;
        return get(key);
    }
}




