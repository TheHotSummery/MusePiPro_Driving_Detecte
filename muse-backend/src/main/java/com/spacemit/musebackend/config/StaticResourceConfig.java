package com.spacemit.musebackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 静态资源映射配置
 * 支持前端Vue打包文件的映射
 */
@Configuration
@Slf4j
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 检测运行环境
        String javaCommand = System.getProperty("sun.java.command", "");
        boolean isBaotaMode = javaCommand.contains("spacemit.topcoder.fun");
        
        String staticDir;
        if (isBaotaMode) {
            // 宝塔面板模式
            staticDir = "/www/wwwroot/spacemit.topcoder.fun/static";
            log.info("🏗️ 检测到宝塔面板模式，使用宝塔路径: {}", staticDir);
        } else {
            // 本地开发模式
            String currentDir = System.getProperty("user.dir");
            staticDir = currentDir + File.separator + "static";
            log.info("🔧 检测到本地开发模式，使用本地路径: {}", staticDir);
        }
        
        // 映射静态资源目录
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/", "file:" + staticDir + File.separator);
        
        // 映射根路径到index.html（支持Vue Router的history模式）
        registry.addResourceHandler("/")
                .addResourceLocations("classpath:/static/", "file:" + staticDir + File.separator)
                .resourceChain(true);
        
        // 映射所有前端路由到index.html（支持Vue Router的history模式）
        // 排除API和WebSocket路径
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "file:" + staticDir + File.separator)
                .resourceChain(true)
                .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                    @Override
                    protected org.springframework.core.io.Resource getResource(String resourcePath, 
                            org.springframework.core.io.Resource location) throws java.io.IOException {
                        org.springframework.core.io.Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的资源不存在，返回index.html（支持Vue Router）
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        } else {
                            // 排除API和WebSocket路径
                            if (!resourcePath.startsWith("api/") && !resourcePath.startsWith("websocket")) {
                                return location.createRelative("index.html");
                            }
                        }
                        return null;
                    }
                });
        
        log.info("✅ 静态资源映射配置完成");
        log.info("   - /static/** -> classpath:/static/ 和 file:{}/", staticDir);
        log.info("   - /** -> 前端Vue应用 (支持history模式)");
    }
}
