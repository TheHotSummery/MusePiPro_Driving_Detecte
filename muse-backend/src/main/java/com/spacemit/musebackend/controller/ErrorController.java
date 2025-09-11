package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object method = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        
        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
        }
        
        String errorMessage = "未知错误";
        if (message != null && !message.toString().isEmpty()) {
            errorMessage = message.toString();
        } else {
            switch (statusCode) {
                case 400:
                    errorMessage = "请求参数错误";
                    break;
                case 401:
                    errorMessage = "未授权访问";
                    break;
                case 403:
                    errorMessage = "访问被拒绝";
                    break;
                case 404:
                    errorMessage = "请求的资源不存在";
                    break;
                case 405:
                    errorMessage = "请求方法不被允许";
                    break;
                case 500:
                    errorMessage = "服务器内部错误";
                    break;
                default:
                    errorMessage = "HTTP " + statusCode + " 错误";
                    break;
            }
        }
        
        log.warn("错误请求: {} {} - 状态码: {}, 错误: {}", 
                method != null ? method.toString() : "UNKNOWN", 
                path != null ? path.toString() : "UNKNOWN", 
                statusCode, errorMessage);
        
        ApiResponse<Object> response = new ApiResponse<>();
        response.setCode(statusCode);
        response.setMessage(errorMessage);
        response.setData(null);
        response.setTimestamp(System.currentTimeMillis());
        
        return ResponseEntity.status(statusCode).body(response);
    }
}
