package com.spacemit.musebackend.controller;

import com.spacemit.musebackend.dto.ApiResponse;
import com.spacemit.musebackend.dto.CreateUserRequest;
import com.spacemit.musebackend.dto.UpdateUserRequest;
import com.spacemit.musebackend.dto.UserResponse;
import com.spacemit.musebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * 获取用户列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            // 将用户传入的page（从1开始）转换为Spring Data JPA的page（从0开始）
            int jpaPage = Math.max(0, page - 1);
            log.info("获取用户列表: page={}, size={}, keyword={}, status={}", page, size, keyword, status);
            Map<String, Object> result = userService.getUsers(jpaPage, size, keyword, status);
            return ApiResponse.success("用户列表获取成功", result);
        } catch (Exception e) {
            log.error("获取用户列表失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Integer userId) {
        try {
            log.info("获取用户详情: userId={}", userId);
            UserResponse user = userService.getUserById(userId);
            return ApiResponse.success("用户详情获取成功", user);
        } catch (Exception e) {
            log.error("获取用户详情失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取用户详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            log.info("创建用户: username={}, email={}", request.getUsername(), request.getEmail());
            UserResponse user = userService.createUser(request);
            return ApiResponse.success("用户创建成功", user);
        } catch (Exception e) {
            log.error("创建用户失败: {}", e.getMessage(), e);
            return ApiResponse.error("创建用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Integer userId, 
                                               @Valid @RequestBody UpdateUserRequest request) {
        try {
            log.info("更新用户: userId={}", userId);
            UserResponse user = userService.updateUser(userId, request);
            return ApiResponse.success("用户更新成功", user);
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage(), e);
            return ApiResponse.error("更新用户失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @PostMapping("/{userId}/delete")
    public ApiResponse<String> deleteUser(@PathVariable Integer userId) {
        try {
            log.info("删除用户: userId={}", userId);
            userService.deleteUser(userId);
            return ApiResponse.success("用户删除成功", "用户已删除");
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage(), e);
            return ApiResponse.error("删除用户失败: " + e.getMessage());
        }
    }
}
