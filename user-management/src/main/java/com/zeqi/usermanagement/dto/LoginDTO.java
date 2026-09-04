package com.zeqi.usermanagement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 📝 LoginDTO = 登录请求 DTO（和 RegisterDTO 字段一样，但依然建议分开写：以后加 captcha 验证码、rememberMe 时方便各自扩展）
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
