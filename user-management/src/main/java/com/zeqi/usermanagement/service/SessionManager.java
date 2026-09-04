package com.zeqi.usermanagement.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * =====================================================================================================================
 * 🧰 SessionManager = 内存会话管理器（= Python 版的 Sessions = {} 全局 dict + 工具函数）
 * =====================================================================================================================
 * 🏷️ @Component = 通用 Spring Bean（Service/Repository 是特化的，这个是通用组件，因为既不是 DB 也不是业务，就是工具）
 *
 * 🎯 核心数据结构：ConcurrentHashMap<sessionId(String), userId(Long)>
 *     ConcurrentHashMap = 线程安全版本的 HashMap，多线程并发（Tomcat 1 个请求=1 个线程）读写不会死锁
 *     等价 Python：import threading; Sessions = {}; lock = threading.Lock() ... 但 Python dict 在 CPython GIL 下
 *                 原子性较好，但 Java 必须自己选并发容器。
 *
 * 💡 生产环境怎么升级？
 *   把这个类内部的 ConcurrentHashMap 换成 Redis String 命令：SET sessionId userId EX 86400（24 小时过期）
 *   接口 save()/findUserId()/invalidateAllByUserId() 一行代码改成 redisTemplate.opsForValue().set/get/keys * ...
 *   Controller 层一行代码都不用改！这就是【面向接口/分层解耦】的好处 ✨
 */
@Component
public class SessionManager {

    /** 🔐 核心存储：sessionId(String, 36 位 UUID) → userId(Long) */
    private final Map<String, Long> SESSIONS = new ConcurrentHashMap<>();

    /** 请求作用域：ThreadLocal<Long> = "当前线程（=当前HTTP请求）的当前用户ID"，类似 Python 里给 request 注入属性 */
    private static final ThreadLocal<Long> CURRENT_USER_HOLDER = new ThreadLocal<>();

    /* ========================================= 对外 API ========================================= */

    /** 登录成功：存 sessionId → userId */
    public void save(String sessionId, Long userId) {
        SESSIONS.put(sessionId, userId);
    }

    /** 拦截器校验：查 sessionId 是否存在，存在返回 userId，不存在返回 null */
    public Long findUserId(String sessionId) {
        if (sessionId == null) { return null; }
        return SESSIONS.get(sessionId);
    }

    /** 注销/改密码：删除一个用户所有的 sessionId（踢掉该用户所有登录设备） */
    public void invalidateAllByUserId(Long userId) {
        // 迭代 keySet，把"value == userId"的 entry 全部删
        SESSIONS.keySet().removeAll(
                SESSIONS.entrySet().stream()
                        .filter(e -> userId.equals(e.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet())
        );
    }

    /** 手动删除指定 sessionId（登出接口，目前没写，以后加 logout 接口直接用） */
    public void invalidate(String sessionId) {
        if (sessionId != null) { SESSIONS.remove(sessionId); }
    }

    /** 调试用：只读快照，看当前有多少个活跃 session */
    public Map<String, Long> snapshot() {
        return Collections.unmodifiableMap(SESSIONS);
    }

    /* ========================================= ThreadLocal 上下文传递 =========================================
     * 拦截器里校验通过后：调用 setCurrentUserId(userId) 把当前用户 ID 塞进 ThreadLocal
     * Controller 里想拿的时候：SessionManager.getCurrentUserId() 直接取（不用每次都从 request attribute 拿）
     * 【必须 remove】：请求结束前（finally）一定要清 ThreadLocal！Tomcat 是线程池，线程会复用，
     *   不清理会"用户A的数据被用户B拿到" = 严重安全漏洞（ThreadLocal 内存泄漏 + 数据污染）。
     */
    public static void setCurrentUserId(Long userId) { CURRENT_USER_HOLDER.set(userId); }
    public static Long getCurrentUserId()             { return CURRENT_USER_HOLDER.get(); }
    public static void clearCurrentUserId()           { CURRENT_USER_HOLDER.remove(); }
}
