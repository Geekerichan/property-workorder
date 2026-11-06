package com.prop.wo.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.prop.wo.common.interceptor.IdempotentInterceptor;
import com.prop.wo.common.interceptor.RoleAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：CORS、登录校验、角色校验、幂等
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final StringRedisTemplate stringRedisTemplate;

    /** 匿名可访问路径 */
    private static final String[] WHITE_LIST = {
            "/api/admin/auth/login",
            "/api/app/auth/login",
            "/api/h5/auth/**",
            "/api/h5/equip/**",
            "/api/h5/notice/**",
            "/api/h5/public/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/favicon.ico",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(WHITE_LIST);
        registry.addInterceptor(new RoleAuthInterceptor()).addPathPatterns("/api/**");
        registry.addInterceptor(new IdempotentInterceptor(stringRedisTemplate)).addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
