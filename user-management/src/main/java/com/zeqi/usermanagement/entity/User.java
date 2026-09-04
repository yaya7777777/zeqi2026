package com.zeqi.usermanagement.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * =====================================================================================================================
 * 👤 User 实体类（Entity = 和数据库 users 表一一对应的 Java 对象 = Python 版 row_to_dict 转成的 dict）
 * =====================================================================================================================
 * ⭐ 设计原则：ORM 映射时，"字段名和表列名保持一致"（H2/JPA 默认下划线转驼峰：created_at → createdAt，
 *             也可以在 yml 里设置 map-underscore-to-camel-case=false 关掉自动转换，本项目为了清晰，手写 BeanPropertyRowMapper。
 *
 * 🎯 字段对照表：
 *   users 表列名    →   Java 字段名    →  类型        →  说明
 *   id              →   id             →  Long        →  主键自增
 *   username        →   username       →  String      →  用户名（唯一）
 *   password        →   password       →  String      →  密码（🔥 严禁出现在任何响应 DTO 里！安全红线）
 *   created_at      →   createdAt      →  LocalDateTime（JDK8+ 新日期 API，替换老的 Date，线程安全不可变）
 *   updated_at      →   updatedAt     →   LocalDateTime
 */
@Data   // Lombok 注解 = 自动帮你生成：Getter/Setter/ToString/EqualsAndHashCode/NoArgsConstructor（5 合 1 魔法）
// ❌ 如果不想装 Lombok，就把 @Data 删掉，手动写下面这些样板代码（IDEA 按 Alt+Insert 一键生成）：
//    ① 无参构造 ② 全参构造 ③ Getter/Setter 每字段 ④ toString() ⑤ equals() & hashCode()
public class User {

    /** 主键：自增用户 ID */
    private Long id;

    /** 用户名（唯一，非空） */
    private String username;

    /**
     * 🔒 密码（题目允许明文存储，生产必须哈希）
     * ⚠️ 超级重要：这个 password 字段只在 Service/Repository 层内部用！
     *    Controller 返回前端时，必须通过 UserVO DTO 手动丢掉 password，绝对不能序列化给前端！
     */
    private String password;

    /** 创建时间（INSERT 时 DEFAULT CURRENT_TIMESTAMP 自动赋值） */
    private LocalDateTime createdAt;

    /** 最后更新时间（UPDATE 时手动 set 新值） */
    private LocalDateTime updatedAt;
}
