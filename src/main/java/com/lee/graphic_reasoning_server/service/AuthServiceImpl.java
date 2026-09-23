package com.lee.graphic_reasoning_server.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.graphic_reasoning_server.common.BizException;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.config.WxConfig;
import com.lee.graphic_reasoning_server.dto.LoginDTO;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.util.JwtUtil;
import com.lee.graphic_reasoning_server.vo.LoginVO;
import com.lee.graphic_reasoning_server.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final WxConfig wxConfig;
    private final UserMapper userMapper;
    private final StringRedisTemplate redis;

    private static final String TOKEN_KEY_PREFIX = "login:token:";

    @Override
    public LoginVO login(LoginDTO dto) {
        if (StrUtil.isBlank(dto.getCode())) {
            throw new BizException("code 不能为空");
        }

        // 1. 调用微信 jscode2session
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + wxConfig.getAppid()
                + "&secret=" + wxConfig.getSecret()
                + "&js_code=" + dto.getCode()
                + "&grant_type=authorization_code";

        String respBody = HttpUtil.get(url, 5000);
        log.info("jscode2session 返回：{}", respBody);
        JSONObject res = JSON.parseObject(respBody);

        String openid = res.getString("openid");
        if (StrUtil.isBlank(openid)) {
            throw new BizException("微信登录失败：" + res.getString("errmsg"));
        }
        String unionid = res.getString("unionid");

        // 2. 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        boolean newUser = false;

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setNickname(StrUtil.blankToDefault(dto.getNickname(), "图推用户"));
            user.setAvatarUrl(StrUtil.nullToEmpty(dto.getAvatarUrl()));
            user.setGender(dto.getGender() == null ? 0 : dto.getGender());
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.insert(user);
            newUser = true;
        } else {
            // 只更新必要字段
            user.setLastLoginTime(LocalDateTime.now());
            if (StrUtil.isNotBlank(dto.getNickname())) user.setNickname(dto.getNickname());
            if (StrUtil.isNotBlank(dto.getAvatarUrl())) user.setAvatarUrl(dto.getAvatarUrl());
            if (dto.getGender() != null) user.setGender(dto.getGender());
            userMapper.updateById(user);
        }

        // 3. 生成 token 并写入 Redis
        String token = JwtUtil.create(user.getId());
        redis.opsForValue().set(TOKEN_KEY_PREFIX + token,
                String.valueOf(user.getId()),
                Duration.ofDays(30));

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setNewUser(newUser);
        return vo;
    }

    @Override
    public UserVO currentUser() {
        Long userId = UserContext.get();
        if (userId == null) throw new BizException("未登录");
        User user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setGender(user.getGender());
        vo.setLastLoginTime(user.getLastLoginTime() == null ? null
                : user.getLastLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return vo;
    }

    @Override
    public void logout() {
        Long userId = UserContext.get();
        if (userId == null) return;
        // 简单做法：前端清 token。如果要服务端失效，需要按 userId 反查 token
        // 这里不做复杂处理
    }
}
