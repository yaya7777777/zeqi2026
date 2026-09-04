package com.zeqi.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * =====================================================================================================================
 * 🚀 Spring Boot 启动类（相当于 Java 版的 main 函数入口 = Python 版 app.py 的 if __name__=='__main__'）
 * =====================================================================================================================
 * ⭐ 一个注解搞定三件事：
 *     @SpringBootApplication =
 *         ① @Configuration（本类是一个配置类，可以注册 Bean）+
 *         ② @EnableAutoConfiguration（✨ 最核心！Spring Boot 的"开箱即用"魔法：
 *                                       看到 classpath 有 H2 驱动，自动帮你建 DataSource；
 *                                       看到 spring-boot-starter-web，自动帮你启动内嵌 Tomcat；
 *                                       看到 JdbcTemplate 依赖，自动帮你注入 JdbcTemplate Bean）+
 *         ③ @ComponentScan（从本类所在包 com.zeqi.usermanagement 开始递归扫描所有
 *                                       @Controller/@Service/@Repository/@Component 注解的类，自动注册到 Spring IOC 容器）
 *
 * 🎯 启动方式（3 种）：
 *     1. IDEA：在本类上右键 → Run 'UserManagementApplication'（最常用）
 *     2. 命令行：cd user-management → mvn spring-boot:run（不需要手动装 Maven，IDEA 自带 mvnw wrapper 也行）
 *     3. 打 jar 包：mvn package → java -jar target/user-management-1.0.0-SNAPSHOT.jar（生产部署用）
 */
@SpringBootApplication
public class UserManagementApplication {

    public static void main(String[] args) {
        // SpringApplication.run() = 启动 Spring 容器 + 初始化所有单例 Bean + 启动内嵌 Tomcat 监听 5000 端口
        SpringApplication.run(UserManagementApplication.class, args);

        // 启动成功后，控制台会看到：
        //   "Started UserManagementApplication in X.XXX seconds (JVM running for X.XXX)"
        //   然后浏览器打开 http://127.0.0.1:5000/h2-console 就能看到 H2 数据库控制台
    }
}
