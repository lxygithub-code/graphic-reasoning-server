package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.common.AdminContext;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.dto.AdminChangePwdDTO;
import com.lee.graphic_reasoning_server.dto.AdminLoginDTO;
import com.lee.graphic_reasoning_server.dto.AdminUpdateDTO;
import com.lee.graphic_reasoning_server.mapper.AdminMapper;
import com.lee.graphic_reasoning_server.po.Admin;
import com.lee.graphic_reasoning_server.util.JwtUtil;
import com.lee.graphic_reasoning_server.vo.AdminInfoVO;
import com.lee.graphic_reasoning_server.vo.AdminLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final StringRedisTemplate redis;

    /**
     * 管理员 token 前缀，与用户 token 区分
     */
    public static final String ADMIN_TOKEN_PREFIX = "login:admin:token:";
    /** ★ 每个管理员的所有 token 索引 */
    public static final String ADMIN_TOKENS_SET_PREFIX = "login:admin:tokens:";

    /** token 有效期（天） */
    private static final long TOKEN_EXPIRE_DAYS = 1;

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
                String.valueOf(admin.getId()),
                Duration.ofDays(TOKEN_EXPIRE_DAYS));

        String setKey = ADMIN_TOKENS_SET_PREFIX + admin.getId();
        redis.opsForSet().add(setKey, token);
        redis.expire(setKey, Duration.ofDays(TOKEN_EXPIRE_DAYS + 1));

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setAdminId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setAvatar(admin.getAvatar());
        return vo;
    }

    @Override
    public AdminInfoVO currentAdmin() {
        Long adminId = AdminContext.get();
        if (adminId == null) throw new BizException("管理员未登录");

        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) throw new BizException("管理员不存在");

        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setAvatar(admin.getAvatar());
        return vo;
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isBlank(token)) return;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (StrUtil.isBlank(token)) return;

        // 1. 查 adminId
        String adminIdStr = redis.opsForValue().get(ADMIN_TOKEN_PREFIX + token);
        if (StrUtil.isNotBlank(adminIdStr)) {
            // 2. 从 Set 中移除
            redis.opsForSet().remove(ADMIN_TOKENS_SET_PREFIX + adminIdStr, token);
        }

        // 3. 删除 token 本身
        redis.delete(ADMIN_TOKEN_PREFIX + token);
    }

    @Override
    @Transactional
    public void updateProfile(AdminUpdateDTO dto) {
        Long adminId = AdminContext.get();
        if (adminId == null) throw new BizException("管理员未登录");

        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) throw new BizException("管理员不存在");

        // 校验新账号是否已被占用
        if (StrUtil.isNotBlank(dto.getUsername())
                && !dto.getUsername().equals(admin.getUsername())) {
            Long count = adminMapper.selectCount(
                    new LambdaQueryWrapper<Admin>()
                            .eq(Admin::getUsername, dto.getUsername())
                            .ne(Admin::getId, adminId));
            if (count != null && count > 0) {
                throw new BizException("账号已被占用");
            }
            admin.setUsername(dto.getUsername().trim());
        }

        if (dto.getNickname() != null) {
            admin.setNickname(dto.getNickname().trim());
        }
        if (dto.getAvatar() != null) {
            admin.setAvatar(dto.getAvatar());
        }

        adminMapper.updateById(admin);
    }

    @Override
    @Transactional
    public void changePassword(AdminChangePwdDTO dto) {
        Long adminId = AdminContext.get();
        if (adminId == null) throw new BizException("管理员未登录");

        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>()
                        .select(Admin::getId, Admin::getPassword, Admin::getUsername)
                        .eq(Admin::getId, adminId));
        if (admin == null) throw new BizException("管理员不存在");

        if (!BCrypt.checkpw(dto.getOldPassword(), admin.getPassword())) {
            throw new BizException("旧密码错误");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BizException("新密码不能与旧密码相同");
        }

        Admin update = new Admin();
        update.setId(adminId);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        adminMapper.updateById(update);

        // 清除该管理员所有 token
        clearAllTokens(adminId);
    }

    /**
     * 清除某个管理员的所有登录 token
     */
    private void clearAllTokens(Long adminId) {
        String setKey = ADMIN_TOKENS_SET_PREFIX + adminId;

        // 1. 取出所有 token
        Set<String> tokens = redis.opsForSet().members(setKey);
        if (tokens != null && !tokens.isEmpty()) {
            // 2. 批量删除 token 字符串
            List<String> keys = tokens.stream()
                    .map(t -> ADMIN_TOKEN_PREFIX + t)
                    .collect(Collectors.toList());
            redis.delete(keys);
        }

        // 3. 删除索引 Set
        redis.delete(setKey);
    }
}
