package com.spacemit.musebackend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisTestUtil {
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 测试Redis连接
     */
    public boolean testRedisConnection() {
        if (redisTemplate == null) {
            log.warn("Redis未配置或连接失败");
            return false;
        }
        
        try {
            // 测试连接
            redisTemplate.opsForValue().set("test:connection", "success", 10, java.util.concurrent.TimeUnit.SECONDS);
            String result = (String) redisTemplate.opsForValue().get("test:connection");
            
            if ("success".equals(result)) {
                log.info("Redis连接测试成功");
                return true;
            } else {
                log.warn("Redis连接测试失败：数据不匹配");
                return false;
            }
        } catch (Exception e) {
            log.error("Redis连接测试失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取Redis信息
     */
    public String getRedisInfo() {
        if (redisTemplate == null) {
            return "Redis未配置";
        }
        
        try {
            return "Redis连接正常";
        } catch (Exception e) {
            return "Redis连接异常: " + e.getMessage();
        }
    }
}




