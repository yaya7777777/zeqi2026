package com.zeqi.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =====================================================================================================================
 * 📦 Result<T> = 后端统一响应 DTO（返回给前端的 JSON 外壳，Python 版里每次都手动构造的 {code,message,data}）
 * =====================================================================================================================
 * ⭐ 为什么要"统一响应结构"？（面试常考）
 *     1. 前后端契约统一：前端同学拿到响应，先看 code 再拿 data，不用每个接口瞎猜字段
 *     2. 全局异常处理方便：不管 Controller 抛什么异常，GlobalExceptionHandler 统一包装成 Result<T>
 *     3. 减少重复代码：static 工厂方法 success()/fail() 一行写就完
 *
 * 🎯 3 个字段（和 Python 版返回体字段 100% 一致，Postman 脚本一个不用改）：
 *     code    : Integer = HTTP 状态码（200/201/400/401/404/409），前端 switch(code) 判断成功失败
 *     message : String  = 中文提示信息（"注册成功"/"用户名重复"…用户能看懂的文案）
 *     data    : T 泛型  = 真实业务数据（成功时为 UserVO/LoginVO/分页数据；失败时为 null 不返回）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** HTTP 语义状态码（和 HTTP Response Status 相同，方便前端看 Body 就知道，不用再去看响应头的 Status） */
    private Integer code;

    /** 中文可读的提示信息 */
    private String message;

    /** 业务数据，泛型 T = 可以塞任何类型（UserVO / List<UserVO> / Map / String 都行） */
    private T data;

    /* ====================== 👇 static 工厂方法（不用每次 new Result<>(...)，一行搞定） ====================== */

    /** 成功无数据（比如 DELETE 204、密码修改成功只给个文字提示） */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    /** 成功 + 带数据（最常用，例如注册完塞 UserVO） */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /** 成功 + 指定 code + 带数据（例如注册成功 code=201 Created） */
    public static <T> Result<T> success(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /** 失败（code 传 4xx/5xx，message 传失败原因） */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /** 失败 + 带数据（罕见场景：比如 409 冲突时把"冲突的现有数据"带回去给前端展示） */
    public static <T> Result<T> fail(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}
