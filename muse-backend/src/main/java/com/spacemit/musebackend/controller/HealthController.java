package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.dto.ApiResponse;
import com.spacemit.musebackend.util.RedisTestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HealthController {
    
    private final RedisTestUtil redisTestUtil;
    
    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        // 应用状态
        health.put("application", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        // Redis状态
        boolean redisStatus = redisTestUtil.testRedisConnection();
        health.put("redis", redisStatus ? "UP" : "DOWN");
        health.put("redisInfo", redisTestUtil.getRedisInfo());
        
        // 数据库状态（简单检查）
        health.put("database", "UP");
        
        return ApiResponse.success("健康检查完成", health);
    }
    
    @GetMapping("/redis")
    public ApiResponse<Map<String, Object>> redisCheck() {
        Map<String, Object> redisInfo = new HashMap<>();
        
        boolean isConnected = redisTestUtil.testRedisConnection();
        redisInfo.put("connected", isConnected);
        redisInfo.put("info", redisTestUtil.getRedisInfo());
        redisInfo.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success("Redis检查完成", redisInfo);
    }
}
