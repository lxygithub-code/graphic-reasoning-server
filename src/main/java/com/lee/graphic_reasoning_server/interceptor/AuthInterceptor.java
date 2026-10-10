package com.lee.graphic_reasoning_server.interceptor;

import cn.hutool.core.util.StrUtil;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;

    private static final String TOKEN_KEY_PREFIX = "login:token:";

    /**
     * ★ 必须登录的"精确路径"
     * 用精确匹配是因为 /api/practice/comment/list（游客可看）和
     * /api/practice/comment（必须登录）前缀撞车
     */
    private static final List<String> AUTH_REQUIRED_EXACT = Arrays.asList(
            "/api/practice/comment",
            "/api/practice/submit-exam"
    );

    /**
     * ★ 必须登录的"路径前缀"
     * 未列出的接口一律允许游客访问（UserContext 可能为 null）
     */
    private static final List<String> AUTH_REQUIRED_PREFIXES = Arrays.asList(
            "/api/favorite",           // 收藏相关
            "/api/practice/records",   // 我的答题记录
            "/api/practice/wrong",     // 错题本
            "/api/auth/me",            // 我的信息
            "/api/auth/profile"        // 修改资料
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 OPTIONS 预检
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 非 HandlerMethod（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String uri = request.getRequestURI();

        // ============ 1. 尝试解析 token（有就填，没有就空着） ============
        String token = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Long userId = JwtUtil.parse(token);
                if (userId != null) {
                    String cache = redis.opsForValue().get(TOKEN_KEY_PREFIX + token);
                    if (String.valueOf(userId).equals(cache)) {
                        UserContext.set(userId);
                    }
                }
            } catch (Exception e) {
                log.warn("[Auth] token 解析异常: {}", e.getMessage());
            }
        }

        // ============ 2. 只有"强制登录"接口才拦截 ============
        if (isAuthRequired(uri) && UserContext.get() == null) {
            writeUnauthorized(response, "请先登录");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isAuthRequired(String uri) {
        if (AUTH_REQUIRED_EXACT.contains(uri)) {
            return true;
        }
        return AUTH_REQUIRED_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
    }
}