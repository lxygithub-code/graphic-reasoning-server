package com.lee.graphic_reasoning_server.interceptor;

import cn.hutool.core.util.StrUtil;
import com.lee.graphic_reasoning_server.common.UserContext;
import com.lee.graphic_reasoning_server.mapper.UserDailyActiveMapper;
import com.lee.graphic_reasoning_server.mapper.UserHourlyActiveMapper;
import com.lee.graphic_reasoning_server.mapper.UserMapper;
import com.lee.graphic_reasoning_server.po.User;
import com.lee.graphic_reasoning_server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final UserDailyActiveMapper dailyActiveMapper;

    private static final String TOKEN_KEY_PREFIX = "login:token:";

    /** 活跃时间刷新防抖 key */
    private static final String ACTIVE_KEY_PREFIX = "user:active:";
    /** 每日活跃 UPSERT 防抖 key */
    private static final String DAILY_KEY_PREFIX = "user:daily:";

    /** L1 防抖：5 分钟内只写一次 DB */
    private static final long ACTIVE_THROTTLE_SECONDS = 300;
    /** L2 防抖：1 分钟内只 UPSERT 一次 */
    private static final long DAILY_THROTTLE_SECONDS = 60;

    /** 必须登录的精确路径 */
    private static final List<String> AUTH_REQUIRED_EXACT = Arrays.asList(
            "/api/practice/comment",
            "/api/practice/submit-exam"
    );

    /** 必须登录的路径前缀 */
    private static final List<String> AUTH_REQUIRED_PREFIXES = Arrays.asList(
            "/api/favorite",
            "/api/practice/records",
            "/api/practice/wrong",
            "/api/auth/me",
            "/api/auth/profile"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (!(handler instanceof HandlerMethod)) return true;

        String uri = request.getRequestURI();

        // 1. 尝试解析 token
        String token = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Long userId = JwtUtil.parse(token);
                if (userId != null) {
                    String cache = redis.opsForValue().get(TOKEN_KEY_PREFIX + token);
                    if (String.valueOf(userId).equals(cache)) {
                        UserContext.set(userId);
                        refreshActive(userId);   // ★ L1 + L2
                    }
                }
            } catch (Exception e) {
                log.warn("[Auth] token 解析异常: {}", e.getMessage());
            }
        }

        // 2. 强制登录接口拦截
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

    // 类里加常量
    private static final String HOURLY_KEY_PREFIX = "user:hourly:";
    // 类里注入
    private final UserHourlyActiveMapper hourlyActiveMapper;

    /**
     * L1 + L2 + L3 刷新活跃信息
     */
    private void refreshActive(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();

            // ---------- L1：t_user.last_active_time（5 分钟防抖） ----------
            String activeKey = ACTIVE_KEY_PREFIX + userId;
            if (Boolean.TRUE.equals(redis.opsForValue()
                    .setIfAbsent(activeKey, "1", Duration.ofSeconds(ACTIVE_THROTTLE_SECONDS)))) {
                User update = new User();
                update.setId(userId);
                update.setLastActiveTime(now);
                userMapper.updateById(update);
            }

            // ---------- L2：t_user_daily_active（1 分钟防抖） ----------
            String dailyKey = DAILY_KEY_PREFIX + userId + ":" + today;
            if (Boolean.TRUE.equals(redis.opsForValue()
                    .setIfAbsent(dailyKey, "1", Duration.ofSeconds(DAILY_THROTTLE_SECONDS)))) {
                dailyActiveMapper.upsert(userId, today);
            }

            // ---------- L3：t_user_hourly_active（每小时一次） ----------
            int hour = now.getHour();
            String hourlyKey = HOURLY_KEY_PREFIX + userId + ":" + today + ":" + hour;
            // TTL 2 小时，跨小时后 key 自动不同
            if (Boolean.TRUE.equals(redis.opsForValue()
                    .setIfAbsent(hourlyKey, "1", Duration.ofHours(2)))) {
                hourlyActiveMapper.insertIgnore(userId, today, hour);
            }
        } catch (Exception e) {
            log.warn("[活跃] 刷新失败 userId={}", userId, e);
        }
    }

    private boolean isAuthRequired(String uri) {
        if (AUTH_REQUIRED_EXACT.contains(uri)) return true;
        return AUTH_REQUIRED_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
    }
}