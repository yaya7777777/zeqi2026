# 2026 择栖工作室后端招新题——任务四 Spring Boot 用户管理系统

本项目按照题目第四大题要求实现，技术栈为 Java + Spring Boot + Spring Data JPA + MySQL。

> 注意：题目明确写明“密码允许明文存储”，因此本项目为了与任务四验收要求保持一致，暂时使用明文密码。真正项目中绝对不应这样做；第五题挑战要求改为密码哈希存储。

## 1. 项目结构

```text
user-management/
├─ pom.xml
├─ schema.sql
├─ study-notes.md
├─ task4-answer.md
├─ postman/
│  └─ Task4.postman_collection.json
└─ src/
   └─ main/
      ├─ java/com/zeqi/user/
      │  ├─ UserManagementApplication.java
      │  ├─ controller/
      │  ├─ dto/
      │  ├─ entity/
      │  ├─ exception/
      │  ├─ repository/
      │  ├─ service/
      │  └─ session/
      └─ resources/application.yml
```

## 2. 环境

- JDK 17+
- Maven 3.9+
- MySQL 8.x
- Postman

## 3. 创建数据库

执行 `schema.sql`。

然后修改：

`src/main/resources/application.yml`

中的：

- `spring.datasource.username`
- `spring.datasource.password`

如果你的 MySQL 密码不是 `123456`，一定要修改。

## 4. 启动

```bash
mvn spring-boot:run
```

启动后：

`http://localhost:8080`

## 5. 接口

### 注册

`POST /auth/register`

```json
{
  "username": "alice",
  "password": "123456"
}
```

成功：

`201 Created`

```json
{
  "id": 1,
  "username": "alice"
}
```

重复用户名：

`409 Conflict`

```json
{
  "message": "username already exists"
}
```

### 登录

`POST /auth/login`

```json
{
  "username": "alice",
  "password": "123456"
}
```

成功：

`200 OK`

```json
{
  "sessionId": "随机UUID"
}
```

### 当前用户

`GET /users/me`

请求头：

```text
sessionId: 登录返回的 sessionId
```

未登录/错误 session：

`401 Unauthorized`

### 用户列表

`GET /users`

请求头：

```text
sessionId: 登录返回的 sessionId
```

### 修改用户名

`PATCH /users/me/username`

请求头：

```text
sessionId: 登录返回的 sessionId
```

Body：

```json
{
  "username": "bob"
}
```

### 修改密码

`PATCH /users/me/password`

请求头：

```text
sessionId: 登录返回的 sessionId
```

Body：

```json
{
  "password": "654321"
}
```

### 注销

`POST /auth/logout`

请求头：

```text
sessionId: 登录返回的 sessionId
```

注销后原 sessionId 再访问需要登录的接口，会得到 `401`。

## 6. 为什么 session 放在服务端？

题目明确要求：

> 登录成功后，服务端保存 sessionId 与 userId 的对应关系；需要登录态的接口从请求头读取 sessionId，并进行校验。

因此本项目使用：

```java
ConcurrentHashMap<String, Long>
```

维护：

```text
sessionId -> userId
```

这是一种教学型实现。它的优点是非常直观；缺点是服务重启后 session 会消失，而且多实例部署时不同服务器之间不会共享 session。实际项目通常会使用 Redis 等集中式存储。

## 7. 与任务四验收要求对应

1. 创建 users 表：`schema.sql`
2. 建立数据库连接：`application.yml`
3. 用户注册：`POST /auth/register`
4. 用户登录 + 当前用户：`POST /auth/login` + `GET /users/me`
5. 用户列表、修改用户名、修改密码、注销：均已实现
6. 状态码：
   - 未登录：401
   - 用户不存在：404
   - 重复注册/用户名冲突：409
   - 参数错误：400
   - 注册成功：201

## 8. Postman

导入 `postman/Task4.postman_collection.json`。

建议按照：

注册 → 登录 → 复制 sessionId → 当前用户 → 用户列表 → 修改用户名 → 修改密码 → 注销 → 再访问当前用户

的顺序测试，并截图作为任务提交材料。

## 9. 第五题挑战

当前版本已经升级到第五题：JWT + BCrypt + 全局异常处理 + 日志。

- 登录返回 `token`
- 认证接口使用 `Authorization: Bearer <token>`
- 密码使用 BCrypt 哈希，不再明文保存
- 统一异常返回 `{"message":"..."}`
- 记录注册、登录成功/失败、修改密码、注销日志

JWT 是无状态认证，注销接口不能单独让已签发 JWT 立即失效；客户端应删除 Token。需要服务端即时撤销时，可进一步设计黑名单、token version 或 Redis。
