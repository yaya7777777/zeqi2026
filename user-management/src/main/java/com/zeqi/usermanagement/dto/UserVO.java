package com.zeqi.usermanagement.dto;

import com.zeqi.usermanagement.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * =====================================================================================================================
 * 🔒 UserVO = User View Object（返回给前端的"安全视图对象" = Python 版 row_to_dict 白名单过滤后的结果）
 * =====================================================================================================================
 * ⚠️ 最重要的安全设计：【绝对不能把 User Entity 直接 return 给前端】！
 *     User Entity 里有 password 字段，Jackson 序列化时会自动把 password 写进 JSON 响应，
 *     浏览器 F12 / 抓包工具一眼看到所有用户密码 = 重大安全漏洞 = P0 事故！
 *
 * 🎯 UserVO 手动只列"可以暴露给前端"的字段（白名单思想）：
 *     ✅ id、username、createdAt、updatedAt
 *     ❌ password（一律不出现）
 *
 * 💡 字段类型和 Python 版返回体 100% 一致，保证 Postman 测试脚本和 4.md 截图教程完全复用！
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * ✨ 构造器：把 User Entity → User VO（"脱敏转换"，核心就是"手动选哪些列带到前端"）
     * 每个地方要转都调用 new UserVO(user)，不用每次写 4 行 set，避免漏改
     */
    public UserVO(User user) {
        if (user == null) { return; }
        this.id = user.getId();
        this.username = user.getUsername();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
