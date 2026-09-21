package com.lee.graphic_reasoning_server.interceptor;


import cn.hutool.core.util.StrUtil;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;

    private static final String TOKEN_KEY_PREFIX = "login:token:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token) || !token.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或 token 缺失");
            return false;
        }
        token = token.substring(7);

        Long userId = JwtUtil.parse(token);
        if (userId == null) {
            writeUnauthorized(response, "token 无效或已过期");
            return false;
        }

        // 校验 Redis 中是否存在（服务端可主动失效）
        String cache = redis.opsForValue().get(TOKEN_KEY_PREFIX + token);
        if (!String.valueOf(userId).equals(cache)) {
            writeUnauthorized(response, "登录态已失效，请重新登录");
            return false;
        }

        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
    }
}
