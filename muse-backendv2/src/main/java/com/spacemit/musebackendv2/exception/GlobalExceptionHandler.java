package com.spacemit.musebackendv2.exception;

import com.spacemit.musebackendv2.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理异常，避免返回敏感信息（如堆栈跟踪）
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * HTTP方法不支持异常（405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String method = e.getMethod();
        String[] supportedMethods = e.getSupportedMethods();
        String supportedMethodsStr = supportedMethods != null ? String.join(", ", supportedMethods) : "未知";
        
        log.warn("HTTP方法不支持: {} {}, 支持的方法: {}", method, request.getRequestURI(), supportedMethodsStr);
        
        ApiResponse<Object> response = ApiResponse.error(405, 
            String.format("请求方法 '%s' 不被支持，该接口支持的方法: %s", method, supportedMethodsStr));
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 404 - 资源不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFound(
            NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("资源不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        
        ApiResponse<Object> response = ApiResponse.error(404, 
            String.format("请求的资源不存在: %s", e.getRequestURL()));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 400 - 参数验证失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        log.warn("参数验证失败: {}", message);
        
        ApiResponse<Object> response = ApiResponse.error(400, "请求参数验证失败: " + message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 400 - 绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        log.warn("参数绑定失败: {}", message);
        
        ApiResponse<Object> response = ApiResponse.error(400, "请求参数绑定失败: " + message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 400 - 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));
        
        log.warn("约束违反: {}", message);
        
        ApiResponse<Object> response = ApiResponse.error(400, "请求参数验证失败: " + message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 400 - 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(
            MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        
        ApiResponse<Object> response = ApiResponse.error(400, 
            String.format("缺少必需的请求参数: %s", e.getParameterName()));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 400 - 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {} = {}", e.getName(), e.getValue());
        
        ApiResponse<Object> response = ApiResponse.error(400, 
            String.format("参数 '%s' 类型不匹配，期望类型: %s", e.getName(), 
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知"));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 400 - 请求体不可读（JSON格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(
            HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(400, "请求体格式错误，请检查JSON格式");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 400 - 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(400, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 500 - 其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e, HttpServletRequest request) {
        log.error("未处理的异常: {} {}", request.getMethod(), request.getRequestURI(), e);
        
        // 生产环境不返回详细错误信息
        String message = "服务器内部错误，请稍后重试";
        
        ApiResponse<Object> response = ApiResponse.error(500, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
















