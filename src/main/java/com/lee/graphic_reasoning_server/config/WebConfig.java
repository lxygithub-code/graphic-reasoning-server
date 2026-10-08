package com.lee.graphic_reasoning_server.config;

import com.lee.graphic_reasoning_server.interceptor.AdminAuthInterceptor;
import com.lee.graphic_reasoning_server.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;         // 小程序端
    private final AdminAuthInterceptor adminAuthInterceptor; // 后台管理端

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // ============ 1. 管理端拦截器（先注册，优先级高） ============
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/dict/**"     // ★ 字典接口放行
                )
                .order(1);

        // ============ 2. 小程序端拦截器 ============
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",          // 小程序登录
                        "/api/dict/**",     // ★ 字典接口放行
                        "/api/auth/admin/**",     // ★ 管理端登录放行
                        "/api/admin/**",
                        // ★★★ 新增：游客可访问的白名单
                        "/api/question/random",             // 游客随机抽题（试玩）
                        "/api/question/count-by-exam-type", // 考试类型统计
                        "/api/practice/comment/list",        // 评论列表（只读）
                        "/api/question/*/preview"        // ★ 新增：题目预览（游客可访问）
                )
                .order(2);
    }
}