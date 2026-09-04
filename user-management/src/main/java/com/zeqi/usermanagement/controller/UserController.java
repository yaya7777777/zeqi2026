package com.zeqi.usermanagement.controller;

import com.zeqi.usermanagement.annotation.AuthRequired;
import com.zeqi.usermanagement.dto.AccountDTOs;
import com.zeqi.usermanagement.dto.Result;
import com.zeqi.usermanagement.dto.UserVO;
import com.zeqi.usermanagement.service.SessionManager;
import com.zeqi.usermanagement.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =====================================================================================================================
 * 👤 UserController = 用户控制器（处理"需要登录"的 5 个接口：查看自己/用户列表/改用户名/改密码/注销）
 * =====================================================================================================================
 * ⭐ 类级别的 @AuthRequired：本类【所有方法】默认都需要登录！
 *   - 如果某方法想"反豁免"（公开），Spring 没 built-in 反注解，做法是：
 *     在 WebMvcConfig.excludePathPatterns 里排除那个 URL，或者把该方法移到别的 Controller。
 *   - 这里我们 5 个接口全要登录，直接贴在类上最干净。
 */
@RestController
@RequestMapping("/api/users")
@AuthRequired   // 🔐 类级别认证：本类的所有方法必须过拦截器 sessionId 校验 → 否则 401
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 🎯 工具：拿到当前登录用户 ID
     * 拦截器 AuthInterceptor.preHandle() 通过 ThreadLocal.set(userId) 塞进来的
     * 等价 Python 版：get_current_user_id() 工具函数
     */
    private Long me() {
        return SessionManager.getCurrentUserId();
    }

    /* ===============================================================================================================
     * 3️⃣ 必做：查看当前登录用户信息 → GET /api/users/me
     * =============================================================================================================== */
    @GetMapping("/me")
    public Result<UserVO> meInfo() {
        UserVO me = userService.getCurrentUser(me());
        return Result.success("获取成功", me);
    }

    /* ===============================================================================================================
     * 4️⃣ 加分：查看用户列表 → GET /api/users    （返回 users 数组 + total 总数，方便前端分页）
     * =============================================================================================================== */
    @GetMapping
    public Result<Map<String, Object>> listAll() {
        List<UserVO> list = userService.listAllUsers();
        Map<String, Object> data = new HashMap<>();
        data.put("users", list);
        data.put("total", list.size());
        return Result.success("获取成功", data);
    }

    /* ===============================================================================================================
     * 5️⃣ 加分：修改用户名 → PATCH /api/users/username  （Body 传新 username）
     * =============================================================================================================== */
    @PatchMapping("/username")
    public Result<UserVO> updateUsername(@Valid @RequestBody AccountDTOs.UpdateUsernameDTO dto) {
        UserVO updated = userService.updateUsername(me(), dto);
        return Result.success("用户名修改成功", updated);
    }

    /* ===============================================================================================================
     * 6️⃣ 加分：修改密码 → PATCH /api/users/password （Body 传 oldPassword + newPassword）
     *    【安全加固】参考 2.md 设计：敏感操作二次验证 + 改密码后自动踢掉所有已登录设备
     * =============================================================================================================== */
    @PatchMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody AccountDTOs.UpdatePasswordDTO dto) {
        userService.updatePassword(me(), dto);
        return Result.success("密码修改成功，请使用新密码重新登录");
    }

    /* ===============================================================================================================
     * 7️⃣ 加分：注销账号 → DELETE /api/users/me （Body 传 password 二次确认）
     * =============================================================================================================== */
    @DeleteMapping("/me")
    public Result<Void> deleteAccount(@Valid @RequestBody AccountDTOs.DeleteAccountDTO dto) {
        userService.deleteAccount(me(), dto);
        return Result.success("账号已注销，感谢使用");
    }
}
