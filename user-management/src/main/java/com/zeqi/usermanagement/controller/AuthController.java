package com.zeqi.usermanagement.controller;

import com.zeqi.usermanagement.dto.LoginDTO;
import com.zeqi.usermanagement.dto.LoginVO;
import com.zeqi.usermanagement.dto.RegisterDTO;
import com.zeqi.usermanagement.dto.Result;
import com.zeqi.usermanagement.dto.UserVO;
import com.zeqi.usermanagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * =====================================================================================================================
 * 🔑 AuthController = 认证控制器（处理"未登录状态下"的公开接口：注册 + 登录）
 * =====================================================================================================================
 * 🏷️ 注解分层解释：
 *   @RestController = @Controller + @ResponseBody 合体：这个类所有方法返回值直接序列化成 JSON 返回给前端
 *   @RequestMapping("/api/auth") = 本类所有接口 URL 前缀统一是 /api/auth，避免每个方法都写前缀
 *
 * ⚠️ 安全红线：注册/登录接口不能贴 @AuthRequired（用户还没 sessionId，再要求登录就死循环了）
 *   因此本类【不】加类级别的 @AuthRequired，内部方法也都不贴。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /* ===============================================================================================================
     * 1️⃣ 注册接口 → POST /api/auth/register  → 成功 HTTP 201 Created + 返回新用户信息（脱敏无密码）
     * ===============================================================================================================
     * 💡 为什么用 ResponseEntity 而不是直接 return Result.success(...)？
     *   因为题目明确要求"注册返回 201"：Result.code=201 只是 Body 里的数字，HTTP 响应头的 Status Code 也要是 201
     *   ResponseEntity.status(HttpStatus.CREATED).body(result) = 同时设置【响应头 Status】= 201 和【响应体 JSON】
     *   Postman / 浏览器 F12 看到的 Response Status = 201 Created（符合 RESTful 最佳实践 ⭐ 面试常考）
     *
     * 🎯 参数校验链：
     *   RegisterDTO 上 @NotBlank/@Size → @Valid 注解开启校验
     *   校验失败 → GlobalExceptionHandler 里 MethodArgumentNotValidException → 自动返回 400 + 中文错误信息
     *   （你不用自己写 if(username==null) return 400，框架替你干完 ✨）
     */
    @PostMapping("/register")
    public ResponseEntity<Result<UserVO>> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO newUser = userService.register(dto);
        Result<UserVO> body = Result.success(201, "注册成功", newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /* ===============================================================================================================
     * 2️⃣ 登录接口 → POST /api/auth/login  → 成功 200 + 返回 UserVO 基础信息 + sessionId
     * ===============================================================================================================
     * 登录成功之后：
     *   · Service 内部生成 UUID sessionId → 存 ConcurrentHashMap
     *   · LoginVO 把 sessionId 带回去给前端
     *   · 前端保存 localStorage.setItem('sessionId', res.data.sessionId)
     *   · 后续请求在 Postman Headers / axios interceptors 里加 sessionId: xxx 就行
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginInfo = userService.login(dto);
        return Result.success("登录成功", loginInfo);
    }
}
