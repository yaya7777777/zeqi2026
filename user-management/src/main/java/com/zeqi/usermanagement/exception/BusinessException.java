package com.zeqi.usermanagement.exception;

import lombok.Getter;

/**
 * ⚠️ BusinessException = 业务自定义异常（Controller/Service 发现不满足业务条件直接 throw new ...）
 * 配合 GlobalExceptionHandler 全局捕获，统一转成 Result.fail(code, message) JSON 返回
 * 为什么自定义异常？比每个方法都 return Result.fail(...) 好：
 *   ① Service 层里不用每个方法都返回 Result（Service 返回纯业务数据 User/List<User>，更纯粹）
 *   ② 抛异常会自动中断后面的逻辑（return 有时不小心忘了继续往下走就 bug 了）
 */
@Getter
public class BusinessException extends RuntimeException {

    /** HTTP 状态码：400 参数错 / 401 未登录 / 404 找不到 / 409 资源冲突… */
    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /* ===== 常用快捷静态工厂（throw BusinessException.badRequest("xxx") 写起来语义更清晰） ===== */
    public static BusinessException badRequest(String message)   { return new BusinessException(400, message); }
    public static BusinessException unauthorized(String message) { return new BusinessException(401, message); }
    public static BusinessException notFound(String message)     { return new BusinessException(404, message); }
    public static BusinessException conflict(String message)     { return new BusinessException(409, message); }
}
