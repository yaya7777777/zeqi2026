package com.zeqi.usermanagement.config;

import com.zeqi.usermanagement.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * =====================================================================================================================
 * 🧭 WebMvcConfig = Spring MVC 配置类（注册拦截器 + CORS 跨域）
 * =====================================================================================================================
 * 🏷️ @Configuration = 告诉 Spring Boot：本类是"配置类"，里面的方法会被 Spring 解析并注册 Bean
 *   实现 WebMvcConfigurer 接口 → 你可以重写 N 个 addXxx() 方法定制 Spring MVC 的方方面面（拦截器/跨域/视图/静态资源…）
 *
 * ⚠️ 注意：pom.xml 已经加了 spring-boot-starter-web，这里就不必 @EnableWebMvc（加了会禁用 Spring Boot 对 MVC 的自动配置）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 认证拦截器（Spring 会自动注入，因为 AuthInterceptor 上有 @Component） */
    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 注册拦截器（相当于 Python Flask 里 @app.before_request 全局挂装饰器）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")                // 先默认拦截所有 URL（/** 表示递归匹配所有子孙路径）
                .excludePathPatterns(                   // 再"排除列表"里写公开接口（不拦截）
                        "/api/auth/**",              // ① 注册 / 登录（所有 /api/auth 开头的接口都公开）
                        "/h2-console/**",            // ② H2 控制台（浏览器看数据库用的，放行）
                        "/error",                    // ③ Spring Boot 默认错误页（别拦）
                        "/favicon.ico"               // ④ 浏览器网站小图标（别拦）
                );
    }

    /**
     * 🌐 全局 CORS 配置（跨域：浏览器前端 Vue/React 页面和后端不同端口时，浏览器同源策略拦截）
     * pom.xml 已经加了 spring-boot-starter-validation（也带了 spring-web），这里配置默认放行所有来源（学习用）
     * 生产环境要改成 allowedOrigins("https://your-frontend.com") 白名单，别用 "*"
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")                  // 允许所有前端域名调用（学习环境）
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")  // 允许的 HTTP 方法（7 种全覆盖）
                .allowedHeaders("*")                         // 允许任何请求头（sessionId 是自定义头，必须允许）
                .exposedHeaders("*")                         // 允许前端读任何响应头
                .allowCredentials(true)                      // 允许带 Cookie / Authorization 凭证
                .maxAge(3600);                                // 预检请求 OPTIONS 缓存 1 小时（减少重复 OPTIONS 预检）
    }
}
