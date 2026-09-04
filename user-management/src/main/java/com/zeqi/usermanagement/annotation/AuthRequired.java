package com.zeqi.usermanagement.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * =====================================================================================================================
 * 🔐 @AuthRequired = 自定义注解（贴在需要登录的 Controller 方法或类上）
 * =====================================================================================================================
 * ⭐ Java 注解 = "贴在代码上的标签"，本身没有任何逻辑，只有和【拦截器 + 反射】配合起来才有用：
 *     AuthInterceptor 每次请求前会去反射查看：
 *       · 当前 HandlerMethod 上有没有 @AuthRequired？
 *       · 或者类（Controller）上有没有 @AuthRequired？
 *       · 有 → 必须校验 sessionId Header（查不到就 401）
 *       · 没有 → 直接放行
 *
 * 等价 Python 版：@login_required 装饰器贴函数
 *
 * 元注解解释：
 *   @Target({ElementType.METHOD, ElementType.TYPE})  → 这个注解可以贴在【方法】上，也可以贴在【类/接口】上
 *   @Retention(RetentionPolicy.RUNTIME)                → 运行时还保留（反射才能读得到；SOURCE/CLASS 阶段就没了）
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthRequired {
    // 注解没有属性（标记型注解，有就代表要校验，不需要值）
}
