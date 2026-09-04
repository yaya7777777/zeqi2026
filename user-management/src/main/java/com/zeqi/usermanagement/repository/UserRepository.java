package com.zeqi.usermanagement.repository;

import com.zeqi.usermanagement.entity.User;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * =====================================================================================================================
 * 🗃️ UserRepository = DAO 数据访问层（= Python 版的 get_db_conn() + cursor.execute() 封装）
 * =====================================================================================================================
 * 🏷️ @Repository = Spring 自动把这个类注册成 Bean（IDEA/@ComponentScan 扫描到，存进 IOC 容器）
 *    Service 层通过 @Autowired / 构造器注入就能直接用。
 *
 * 🎯 为什么选 JdbcTemplate 不选 MyBatis/JPA？
 *   1. 招新题考察 SQL 基本功：JdbcTemplate 就是纯手写 SQL，评委一眼能看到你 SQL 写对没
 *   2. 学习成本最低：就是把 Python 里 cursor.execute(SQL, params) 换成 jdbcTemplate.query/update()，
 *      SQL 语句和参数占位符思路完全一样（Python 的 ? = Java 的 ?，都是 PreparedStatement 预编译，防注入 ✨）
 *   3. 零额外配置：spring-boot-starter-jdbc 自动帮你配置好 JdbcTemplate，DataSource 也自动配 H2
 */
@Repository
public class UserRepository {

    /**
     * Spring Boot 自动注入 JdbcTemplate（已经配置好 DataSource，直接用就行）
     * 用【构造器注入】而不是 @Autowired 字段注入：Spring 官方推荐姿势（可测试性好，依赖不可变 final）
     */
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ===============================================================================================================
     * 🔍 查询方法（SELECT）
     * =============================================================================================================== */

    /**
     * 根据主键 id 查用户
     * BeanPropertyRowMapper：Spring 帮你自动把 ResultSet 的列（下划线命名 created_at）→ Java 字段（驼峰 createdAt）
     * 等价 Python：cursor.execute("SELECT * FROM users WHERE id=?", (id,)); row = cursor.fetchone();
     */
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), id);
        return list.stream().findFirst();
    }

    /** 根据用户名查用户（登录 / 查重时用） */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), username);
        return list.stream().findFirst();
    }

    /** 查所有用户，按 id 升序（用户列表接口用） */
    public List<User> findAllOrderById() {
        String sql = "SELECT * FROM users ORDER BY id ASC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }

    /** 统计用户名存在且 id≠指定 id（修改用户名时查重用，防止把别的已存在用户名改重名 → 409） */
    public Integer countByUsernameAndIdNot(String username, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND id <> ?";
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Integer.class, username, excludeId));
    }

    /* ===============================================================================================================
     * ✏️ 写入方法（INSERT / UPDATE / DELETE）→ 返回 int = 影响的行数（0 表示没改到任何行）
     * =============================================================================================================== */

    /**
     * 插入新用户，返回【自动生成的自增主键 id】
     * 🔑 KeyHolder = Spring 拿 JDBC Statement.getGeneratedKeys() 封装好的工具（等价 Python cursor.lastrowid）
     * 这里用 PreparedStatementCreator + Statement.RETURN_GENERATED_KEYS，确保能拿回自增 id。
     */
    public Long insert(User user) {
        String sql = "INSERT INTO users(username, password, created_at, updated_at) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(java.sql.Connection con) throws java.sql.SQLException {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }
        }, keyHolder);
        // H2 返回的 key 可能是 BigDecimal，转 Long
        Number key = (Number) keyHolder.getKeys().get("ID");
        return key.longValue();
    }

    /** 改用户名（同时刷新 updated_at） */
    public int updateUsername(Long id, String newUsername) {
        String sql = "UPDATE users SET username = ?, updated_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql,
                newUsername,
                Timestamp.valueOf(LocalDateTime.now()),
                id);
    }

    /** 改密码（同时刷新 updated_at） */
    public int updatePassword(Long id, String newPassword) {
        String sql = "UPDATE users SET password = ?, updated_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql,
                newPassword,
                Timestamp.valueOf(LocalDateTime.now()),
                id);
    }

    /** 按主键删用户（注销账号） */
    public int deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
