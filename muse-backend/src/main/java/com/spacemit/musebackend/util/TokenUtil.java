package com.spacemit.musebackend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Token处理工具类
 * 优先从Authorization Header获取Token，如果获取不到再从URL参数获取
 */
@Slf4j
public class TokenUtil {

    /**
     * 获取Token
     * 优先级：Authorization Header > URL参数token
     * 
     * @return Token字符串，如果都获取不到则返回null
     */
    public static String getToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("无法获取请求上下文");
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // 1. 优先从Authorization Header获取
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.trim().isEmpty()) {
                log.debug("从Authorization Header获取Token");
                return authHeader;
            }
            
            // 2. 从URL参数获取
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.trim().isEmpty()) {
                log.debug("从URL参数获取Token");
                return tokenParam;
            }
            
            log.warn("未找到Token（既不在Authorization Header中，也不在URL参数中）");
            return null;
            
        } catch (Exception e) {
            log.error("获取Token时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 清理Token（移除Bearer前缀）
     * 
     * @param token 原始Token
     * @return 清理后的Token
     */
    public static String cleanToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        
        return token;
    }

    /**
     * 验证Token是否有效（非空且不为空字符串）
     * 
     * @param token Token字符串
     * @return 是否有效
     */
    public static boolean isValidToken(String token) {
        return token != null && !token.trim().isEmpty();
    }

    /**
     * 获取Token来源描述（用于日志）
     * 
     * @return Token来源描述
     */
    public static String getTokenSource() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "未知";
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // 检查Authorization Header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.trim().isEmpty()) {
                return "Authorization Header";
            }
            
            // 检查URL参数
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.trim().isEmpty()) {
                return "URL参数";
            }
            
            return "未找到";
            
        } catch (Exception e) {
            log.error("获取Token来源时发生异常: {}", e.getMessage(), e);
            return "异常";
        }
    }
}




