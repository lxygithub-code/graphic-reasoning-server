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
                        "/api/dict/**",     // ★ 字典接口放行
                        "/api/admin/upload/**"   // 上传接口如需提前放行（可选）
                )
                .order(1);

        // ============ 2. 小程序端拦截器 ============
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",          // 小程序登录
                        "/api/auth/admin/**",       // ★ 管理端登录/登出（用 ** 覆盖所有）
                        "/api/admin/**",            // ★★★ 关键：排除所有管理端接口
                        "/api/dict/**",     // ★ 字典接口放行
                        "/api/health"
                )
                .order(2);
    }
}