package com.lee.graphic_reasoning_server.interceptor;

import cn.hutool.core.util.StrUtil;
import com.lee.graphic_reasoning_server.common.AdminContext;
import com.lee.graphic_reasoning_server.service.AdminServiceImpl;
import com.lee.graphic_reasoning_server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;

    // ★ 关键：用管理端的前缀，和 AdminServiceImpl 里的保持一致
    private static final String ADMIN_TOKEN_PREFIX = AdminServiceImpl.ADMIN_TOKEN_PREFIX;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token) || !token.startsWith("Bearer ")) {
            writeUnauthorized(response, "管理员未登录");
            return false;
        }
        token = token.substring(7);

        Long adminId = JwtUtil.parse(token);
        if (adminId == null) {
            writeUnauthorized(response, "token 无效或已过期");
            return false;
        }

        // ★ 用管理端前缀查
        String cache = redis.opsForValue().get(ADMIN_TOKEN_PREFIX + token);
        if (!String.valueOf(adminId).equals(cache)) {
            writeUnauthorized(response, "登录态已失效");
            return false;
        }

        AdminContext.set(adminId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        AdminContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
    }
}