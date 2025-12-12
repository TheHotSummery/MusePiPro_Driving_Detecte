package com.spacemit.musebackendv2.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Token处理工具类
 * 支持从多种方式获取Token：Authorization Header、token Header、URL参数
 * 所有硬件上报接口都支持query和header任选传参token
 */
@Slf4j
public class TokenUtil {

    /**
     * 获取Token
     * 优先级：Authorization Header > token Header > URL参数token
     * 支持多种传参方式，任选其一即可
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
            
            // 1. 优先从Authorization Header获取 (Bearer TOKEN 格式)
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.trim().isEmpty()) {
                log.debug("从Authorization Header获取Token");
                return authHeader;
            }
            
            // 2. 从token Header获取 (直接传token值)
            String tokenHeader = request.getHeader("token");
            if (tokenHeader != null && !tokenHeader.trim().isEmpty()) {
                log.debug("从token Header获取Token");
                return tokenHeader;
            }
            
            // 3. 从URL参数获取 (?token=YOUR_TOKEN)
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.trim().isEmpty()) {
                log.debug("从URL参数获取Token");
                return tokenParam;
            }
            
            log.warn("未找到Token（既不在Header中，也不在URL参数中）");
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
            
            // 检查token Header
            String tokenHeader = request.getHeader("token");
            if (tokenHeader != null && !tokenHeader.trim().isEmpty()) {
                return "token Header";
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










