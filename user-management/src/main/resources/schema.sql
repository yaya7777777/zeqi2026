-- =====================================================================================================================
-- 📘 schema.sql = Spring Boot 启动时自动执行的"建表脚本"（相当于 Python 版的 init_db.sql）
-- 触发条件：spring.sql.init.mode=always + 脚本放在 classpath 下。用 IF NOT EXISTS 保证可以重复执行不报错。
-- =====================================================================================================================

-- 表：用户表 users（和 Python 版字段 100% 一致，保证 Postman 测试脚本不用改！）
-- H2 兼容 MySQL 语法：AUTO_INCREMENT = MySQL 的自增；CLOB 当 TEXT 用；TIMESTAMP 类型通用
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键（自增用户ID）',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名（UNIQUE 约束防重名，冲突返回 409）',
    password    VARCHAR(255) NOT NULL COMMENT '密码（题目允许明文，生产必须 BCryptPasswordEncoder 哈希）',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间'
);

-- 给 username 加索引（虽然 UNIQUE 会自动建唯一索引，这里显式写出来让你知道原理：B+ 树按 username 有序存）
-- CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
