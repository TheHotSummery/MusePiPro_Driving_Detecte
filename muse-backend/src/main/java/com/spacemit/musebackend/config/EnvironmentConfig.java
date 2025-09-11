package com.spacemit.musebackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

/**
 * 环境配置类
 * 根据运行环境自动调整配置
 */
@Configuration
@Slf4j
public class EnvironmentConfig {

    @PostConstruct
    public void init() {
        // 检测运行环境
        String javaCommand = System.getProperty("sun.java.command", "");
        boolean isJarMode = javaCommand.contains(".jar");
        
        if (isJarMode) {
            log.info("🚀 检测到JAR包运行模式，使用生产环境配置");
            log.info("📊 数据库: localhost:3306/spacemit");
            log.info("🔴 Redis: localhost:6379 (无密码)");
            log.info("📁 静态资源: ./static/ 目录");
        } else {
            log.info("🔧 检测到开发模式，使用开发环境配置");
        }
    }

    /**
     * 生产环境配置
     */
    @Configuration
    @Profile("prod")
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
    public static class ProductionConfig {
        
        @PostConstruct
        public void init() {
            log.info("🏭 生产环境配置已激活");
        }
    }
}




