package com.lee.graphic_reasoning_server.controller;

import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.dto.AdminLoginDTO;
import com.lee.graphic_reasoning_server.service.AdminService;
import com.lee.graphic_reasoning_server.service.AdminServiceImpl;
import com.lee.graphic_reasoning_server.vo.AdminLoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {
    private final StringRedisTemplate redis;
    private final AdminService adminService;

    @PostMapping("/login")
    public R<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok(adminService.login(dto));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            redis.delete(AdminServiceImpl.ADMIN_TOKEN_PREFIX + token);
        }
        return R.ok();
    }
}
