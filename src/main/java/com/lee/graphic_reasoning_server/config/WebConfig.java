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

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 管理端（独立体系，跟小程序端不共用 UserContext）
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/dict/**")   // 字典接口两端都用，放行
                .order(1);

        // 小程序端：全量进拦截器，内部再判定是否强制登录
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",       // 登录接口本身
                        "/api/auth/admin/**",    // 管理端登录
                        "/api/admin/**"          // 管理端由上面那个拦截器处理
                )
                .order(2);
    }
}