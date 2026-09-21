package com.lee.graphic_reasoning_server.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.dto.AdminLoginDTO;
import com.lee.graphic_reasoning_server.mapper.AdminMapper;
import com.lee.graphic_reasoning_server.po.Admin;
import com.lee.graphic_reasoning_server.util.JwtUtil;
import com.lee.graphic_reasoning_server.vo.AdminLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final StringRedisTemplate redis;

    /**
     * 管理员 token 前缀，与用户 token 区分
     */
    public static final String ADMIN_TOKEN_PREFIX = "login:admin:token:";

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getUsername, dto.getUsername())
                        .select(Admin::getId, Admin::getUsername, Admin::getPassword,
                                Admin::getNickname, Admin::getAvatar, Admin::getStatus));

        if (admin == null) throw new BizException("账号或密码错误");
        if (admin.getStatus() == null || admin.getStatus() != 1) throw new BizException("账号已被禁用");

        if (!BCrypt.checkpw(dto.getPassword(), admin.getPassword())) {
            throw new BizException("账号或密码错误");
        }

        // 更新最近登录时间
        Admin update = new Admin();
        update.setId(admin.getId());
        update.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(update);

        // 生成 token
        String token = JwtUtil.create(admin.getId());
        redis.opsForValue().set(ADMIN_TOKEN_PREFIX + token,
                String.valueOf(admin.getId()), 7, TimeUnit.DAYS);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setAdminId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setAvatar(admin.getAvatar());
        return vo;
    }

    @Override
    public void logout() {
        // 简单做法：前端清 token，服务端也可根据 header 里的 token 主动删除
    }
}
