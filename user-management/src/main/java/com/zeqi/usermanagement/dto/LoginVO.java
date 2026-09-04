package com.zeqi.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 🎯 LoginVO = 登录成功后返回给前端的 DTO = UserVO 基础上，再额外加一个 sessionId 字段
 * 前端拿到 sessionId 后：保存到 localStorage，后续请求头里加 Key=sessionId Value=xxx
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 🎫 随机会话标识：后端 Sessions Map 的 key，UUID 生成
     * （Python 版也是塞到 user_info["sessionId"]，保持字段名一模一样 🌟）
     */
    private String sessionId;
}
