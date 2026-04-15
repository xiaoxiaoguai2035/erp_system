package com.dongjian.erp.manufacturingerpsystem.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/api/**")
                        .notMatch("/api/v1/auth/login")
                        .notMatch("/api/v1/health")
                        .check(r -> cn.dev33.satoken.stp.StpUtil.checkLogin())))
                .addPathPatterns("/**");
    }
}
