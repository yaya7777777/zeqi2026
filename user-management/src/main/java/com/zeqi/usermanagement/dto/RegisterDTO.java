package com.zeqi.usermanagement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * =====================================================================================================================
 * 📝 RegisterDTO = 用户注册请求 DTO（前端注册表单提交过来的 JSON）
 * =====================================================================================================================
 * ⭐ DTO（Data Transfer Object 数据传输对象）是什么？
 *     DTO = 专门用来"接 HTTP 请求 / 返 HTTP 响应"的 Java 对象，和 Entity 的区别：
 *       · Entity：和数据库表一一对应，有 password 这种敏感字段
 *       · DTO：和前端接口契约一一对应，RegisterDTO 就是"注册时前端必须传什么字段我们就接什么字段"
 *
 * 🎯 为什么用 DTO 而不是直接用 User Entity 接参数？
 *     1. 字段不一致：注册只需要 username/password 2 个字段，User Entity 有 id/createdAt/updatedAt 等等不该前端传的
 *     2. 安全隔离：如果直接用 User Entity 接参数，恶意前端传 {"id":999,"password":"admin"} 会覆盖主键，危险
 *     3. 校验更精准：DTO 上单独加 @NotBlank/@Size 校验，不污染 Entity
 *
 * 📌 JSR-303 校验注解（配合 Controller 上的 @Valid 开启自动拦截 → 全局异常捕获，统一返回 400）：
 *     @NotBlank = 不能是 null、不能是 ""、不能全是空格（比 @NotNull/@NotEmpty 更严，字符串推荐）
 *     @Size(min=1, max=50) = 字符串长度限制（对应数据库 VARCHAR(50)）
 */
@Data
public class RegisterDTO {

    /** 用户名（必须 1~50 字符） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 50, message = "用户名长度需在 1-50 字符之间")
    private String username;

    /** 密码（题目允许明文，所以没 @Pattern，生产环境要加"至少8位含大小写数字特殊字符"） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 255, message = "密码长度需在 1-255 字符之间")
    private String password;
}
