package com.zeqi.usermanagement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 📝 UpdateUsernameDTO / UpdatePasswordDTO / DeleteAccountDTO
 * 把"改用户名、改密码、注销确认"这三个 PATCH/DELETE 接口的请求体分别独立出来
 * （和 Python 版 3 个接口 Body 一一对应）
 */
public class AccountDTOs {

    /** DTO 1：修改用户名请求 Body */
    @Data
    public static class UpdateUsernameDTO {
        @NotBlank(message = "新用户名不能为空")
        @Size(min = 1, max = 50, message = "新用户名长度 1-50 字符")
        private String username;
    }

    /** DTO 2：修改密码请求 Body（必须校验【原密码】+【新密码】，参考 2.md 安全设计：敏感操作二次验证） */
    @Data
    public static class UpdatePasswordDTO {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 1, max = 255, message = "新密码长度 1-255 字符")
        private String newPassword;
    }

    /** DTO 3：注销账号请求 Body（高危操作，二次确认密码） */
    @Data
    public static class DeleteAccountDTO {
        @NotBlank(message = "注销必须再次确认密码")
        private String password;
    }
}
