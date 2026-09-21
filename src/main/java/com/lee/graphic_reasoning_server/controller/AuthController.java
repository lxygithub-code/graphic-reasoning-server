package com.lee.graphic_reasoning_server.controller;

import cn.hutool.core.util.StrUtil;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.R;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.dto.LoginDTO;
import com.lee.graphic_reasoning_server.dto.UserUpdateDTO;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.service.AuthService;
import com.lee.graphic_reasoning_server.vo.LoginVO;
import com.lee.graphic_reasoning_server.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    /**
     * 微信登录：code 换 token
     */
    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    /**
     * 获取当前用户
     */
    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(authService.currentUser());
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /** 更新昵称/头像 */
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody UserUpdateDTO dto) {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");

        User user = new User();
        user.setId(userId);
        if (StrUtil.isNotBlank(dto.getNickname())) {
            user.setNickname(dto.getNickname().trim());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        userMapper.updateById(user);
        return R.ok();
    }

}