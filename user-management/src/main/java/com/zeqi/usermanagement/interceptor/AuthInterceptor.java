package com.zeqi.usermanagement.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeqi.usermanagement.annotation.AuthRequired;
import com.zeqi.usermanagement.dto.Result;
import com.zeqi.usermanagement.service.SessionManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * =====================================================================================================================
 * 🛡️ AuthInterceptor = 认证拦截器（Spring MVC 的 HandlerInterceptor = Python 版 @login_required 装饰器）
 * =====================================================================================================================
 * 🎯 工作机制：Spring MVC 收到请求 → 匹配到 Controller 方法 → 先挨个走 preHandle() 拦截器
 *   preHandle 返回值：
 *     · true  = 放行，继续执行 Controller 方法
 *     · false = 拦截，不再进入 Controller，直接在 response 里写错误 JSON 返回 401
 *
 * 💡 判定是否需要登录（3 层优先级：方法 > 类 > 默认放行）
 *   ① 先看"目标方法"上有没有 @AuthRequired 注解 → 有就校验
 *   ② 方法上没有 → 再看"方法所在的 Controller 类"上有没有 @AuthRequired → 有就校验
 *   ③ 都没有 → 公开接口（注册/登录），直接放行
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求头里 sessionId 的 key（和题目要求完全一致，也和 Python 版 SESSION_HEADER 常量相同） */
    public static final String SESSION_HEADER_NAME = "sessionId";

    /** 注入 SessionManager + Jackson ObjectMapper（Spring Boot 自带，把 Result 对象序列化成 JSON 字符串） */
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(SessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果 handler 不是 HandlerMethod（比如访问静态资源 / 404）→ 直接放行
        if (!(handler instanceof HandlerMethod)) { return true; }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> beanType = handlerMethod.getBeanType();

        // 判断：方法上 @AuthRequired 或 类上 @AuthRequired
        boolean needAuth = method.isAnnotationPresent(AuthRequired.class)
                        || beanType.isAnnotationPresent(AuthRequired.class);
        if (!needAuth) {
            return true; // 公开接口（注册/登录）→ 直接过
        }

        // ====================== 需要登录：开始校验 ======================
        String sessionId = request.getHeader(SESSION_HEADER_NAME);
        Long userId = sessionManager.findUserId(sessionId);

        // sessionId 为空 / Sessions 里找不到 → 401 Unauthorized（题目明确要求）
        if (userId == null) {
            writeJsonResponse(response, 401, Result.fail(401, "未登录或 sessionId 无效，请重新登录"));
            return false;
        }

        // ✅ 校验通过：塞进 ThreadLocal，后面 Controller 里直接 SessionManager.getCurrentUserId() 拿
        SessionManager.setCurrentUserId(userId);
        return true;
    }

    /**
     * 请求结束（正常/异常都会走 afterCompletion）→ 一定要清 ThreadLocal！
     * （Tomcat 线程池复用线程，下一个请求会拿到上一个用户的 userId = 重大安全事故）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SessionManager.clearCurrentUserId();
    }

    /* ====================================== 内部工具：往 response 写 JSON ====================================== */
    private void writeJsonResponse(HttpServletResponse resp, int statusCode, Result<?> body) throws java.io.IOException {
        resp.setStatus(statusCode);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.getWriter().write(objectMapper.writeValueAsString(body));
        resp.getWriter().flush();
    }
}
