# Spring Boot 用户管理系统学习笔记

这份笔记不是单纯解释“代码怎么写”，而是解释**为什么项目要这样分层、每一层解决什么问题，以及一个 HTTP 请求到底是怎么穿过整个项目的**。

---

# 一、先建立整体认识：Spring Boot 到底在干什么？

任务四本质上不是让我们“写几个接口”，而是在练习一个最基础的后端 Web 项目。

可以把后端理解成：

```text
客户端
  │
  │ HTTP 请求
  ▼
Controller
  │
  │ 调用业务
  ▼
Service
  │
  │ 操作数据
  ▼
Repository
  │
  │ SQL / JPA
  ▼
MySQL
```

返回的时候反过来：

```text
MySQL
  ↓
Repository
  ↓
Service
  ↓
Controller
  ↓
JSON
  ↓
客户端
```

所以这个项目最核心的学习目标不是背注解，而是理解：

**HTTP → Controller → Service → Repository → Database**

这是以后学习 Spring Boot、Java 后端甚至其他后端框架时非常重要的一条主线。

---

# 二、为什么使用 Spring Boot？

如果完全不用 Spring，我们自己实现 HTTP 服务，需要考虑：

- 如何监听端口？
- 如何解析 HTTP 请求？
- 如何判断请求 URL？
- 如何把 JSON 转成 Java 对象？
- 如何连接数据库？
- 如何管理对象？
- 如何处理异常？

Spring Boot 帮我们把大量基础设施准备好了。

我们只需要关注：

```java
@PostMapping("/register")
public ResponseEntity<UserResponse> register(...) {
    ...
}
```

也就是说：

**Spring Boot 负责“框架”，我们负责“业务”。**

这就是学习框架时非常重要的思维方式。

---

# 三、为什么要有 Controller？

Controller 的职责可以简单理解成：

> 接待 HTTP 请求。

例如：

```java
@PostMapping("/register")
public ResponseEntity<UserResponse> register(
        @RequestBody RegisterRequest request) {
    return ...
}
```

这里：

- `@PostMapping`：说明这是一个 POST 接口
- `/register`：说明 URL
- `@RequestBody`：把 JSON 请求体转换成 Java 对象
- 返回值：最终会转换成 JSON

Controller 最好不要写大量业务逻辑。

不推荐：

```java
@PostMapping("/register")
public Object register(...) {
    // 查数据库
    // 判断用户名
    // 创建用户
    // 保存数据库
    // 处理异常
    // ...
}
```

因为 Controller 会越来越臃肿。

更推荐：

```java
Controller
    ↓
userService.register()
```

Controller 只负责：

**HTTP 层面的事情。**

---

# 四、为什么需要 Service？

Service 负责：

> 业务逻辑。

例如注册：

```text
用户提交用户名密码
        ↓
用户名是否已经存在？
        ↓
不存在
        ↓
创建 User
        ↓
保存
```

这些判断属于“业务规则”，因此放在 Service。

例如：

```java
if (userRepository.existsByUsername(request.getUsername())) {
    throw new ApiException(
        HttpStatus.CONFLICT,
        "username already exists"
    );
}
```

这样 Controller 就很干净。

以后如果注册业务变复杂：

```text
注册
 ↓
检查用户名
 ↓
检查验证码
 ↓
创建用户
 ↓
发送欢迎邮件
 ↓
记录日志
```

这些都应该主要由 Service 组织。

---

# 五、为什么需要 Repository？

Service 需要操作数据库。

如果让 Service 自己写大量 SQL：

```text
Service
 ├─ SQL
 ├─ 业务逻辑
 ├─ 参数判断
 └─ 异常处理
```

又会变得混乱。

所以我们把数据库操作单独抽出来：

```text
Service
   ↓
Repository
   ↓
Database
```

本项目：

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

Spring Data JPA 会帮我们提供很多基础数据库操作。

例如：

```java
userRepository.findById(id);
userRepository.findAll();
userRepository.save(user);
userRepository.existsByUsername(username);
```

因此我们不需要自己写大量 CRUD SQL。

---

# 六、为什么 User 是 Entity？

数据库里有：

```text
users
-------------------------
id
username
password
```

Java 中有：

```java
class User {
    Long id;
    String username;
    String password;
}
```

我们希望：

```text
Java 对象
   ↕
数据库记录
```

JPA 就是帮助我们完成这种映射的技术。

所以：

```java
@Entity
@Table(name = "users")
public class User {
    ...
}
```

意思可以理解成：

> User 这个 Java 类对应数据库中的 users 表。

而：

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

表示：

> id 是主键，并且由数据库自动生成。

---

# 七、为什么不能直接把 Entity 当接口返回？

假设 User：

```java
class User {
    Long id;
    String username;
    String password;
}
```

如果直接返回 User：

```json
{
  "id": 1,
  "username": "alice",
  "password": "123456"
}
```

密码就暴露了。

因此本项目创建：

```java
public record UserResponse(
    Long id,
    String username
) {}
```

返回：

```json
{
  "id": 1,
  "username": "alice"
}
```

这就是 DTO 的意义之一：

> 数据库模型和接口模型不要完全混在一起。

以后学习到更复杂的项目，会经常看到：

```text
Entity
DTO
VO
```

现在先理解一个核心原则：

**数据库怎么存，和 API 怎么返回，是两件事情。**

---

# 八、为什么登录需要 sessionId？

题目明确规定：

> 登录成功后，服务端保存 sessionId 与 userId 的对应关系。

因此登录：

```text
用户名 + 密码
       ↓
验证成功
       ↓
生成随机 sessionId
       ↓
保存：
sessionId → userId
       ↓
返回 sessionId
```

例如：

```text
abc-123 → 1
```

客户端以后请求：

```text
GET /users/me

sessionId: abc-123
```

服务器查：

```text
abc-123
   ↓
userId = 1
   ↓
查询 users 表 id=1
   ↓
返回 Alice
```

所以 sessionId 本质上是：

> “服务器暂时认可你是谁”的凭证。

---

# 九、为什么 SessionManager 单独放一个类？

如果把 session 直接写在 UserService：

```java
Map<String, Long> sessions;
```

也能运行。

但是以后项目变大：

```text
UserService
 ├─ 注册
 ├─ 登录
 ├─ 修改密码
 ├─ Session
 ├─ 注销
 └─ ...
```

就会越来越乱。

因此单独抽成：

```text
session/
└── SessionManager.java
```

它只负责：

```text
创建 session
查询 session
删除 session
```

这叫做**职责分离**。

一个类尽量只负责一类事情。

---

# 十、为什么使用 ConcurrentHashMap？

本项目使用：

```java
ConcurrentHashMap<String, Long>
```

而不是普通的：

```java
HashMap<String, Long>
```

因为 Web 服务可能同时处理多个请求。

例如：

```text
用户 A ─┐
用户 B ─┼→ Spring Boot
用户 C ─┘
```

多个请求可能同时操作 session。

`ConcurrentHashMap` 更适合这种并发场景。

不过要注意：

**这只是任务四的教学实现。**

它不是生产环境完整的 Session 方案。

---

# 十一、为什么 session 不存 MySQL？

这里需要区分两种数据。

用户：

```text
username
password
```

属于长期数据。

所以放：

```text
MySQL
```

session：

```text
sessionId → userId
```

在这个题目里只是登录状态。

所以可以暂时放：

```text
内存
```

但真正项目中，如果：

```text
服务器 A
服务器 B
服务器 C
```

那么：

```text
用户登录服务器 A
       ↓
session 在 A 内存

下一次请求到 B
       ↓
B 不认识这个 session
```

因此生产环境经常使用：

```text
Redis
```

保存 Session。

这个知识点非常重要：

**单机内存 Session 是为了学习简单；Redis Session 才更适合多实例服务。**

---

# 十二、为什么要有异常处理？

假设用户重复注册。

如果每个 Controller 都写：

```java
try {
    ...
} catch (...) {
    ...
}
```

项目会出现大量重复代码。

所以本项目使用：

```java
@RestControllerAdvice
public class GlobalExceptionHandler
```

统一处理异常。

例如：

```java
throw new ApiException(
    HttpStatus.CONFLICT,
    "username already exists"
);
```

统一处理器：

```text
ApiException
     ↓
GlobalExceptionHandler
     ↓
HTTP 409
{
  "message": "username already exists"
}
```

这就是：

**统一异常处理。**

它在第五题挑战中也是明确要求学习的内容。

---

# 十三、为什么要区分 400、401、404、409？

状态码不是随便写的。

### 400 Bad Request

请求本身有问题。

例如：

```json
{
  "username": ""
}
```

### 401 Unauthorized

没有通过身份认证。

例如：

```text
sessionId 不存在
sessionId 错误
```

### 404 Not Found

目标资源不存在。

例如：

```text
user id = 99999
```

### 409 Conflict

请求和当前资源状态发生冲突。

例如：

```text
注册 alice
但 alice 已经存在
```

所以状态码其实是在告诉客户端：

> 这次请求为什么没有成功。

---

# 十四、为什么注册成功使用 201？

题目要求：

```text
POST /auth/register
```

创建了一个新的用户资源。

HTTP 中：

```text
201 Created
```

表示：

> 新资源创建成功。

所以注册：

```text
201
```

比简单返回：

```text
200
```

更加符合 HTTP 语义。

---

# 十五、一次注册请求到底经历了什么？

这是任务四最值得理解的一部分。

假设 Postman 发送：

```http
POST /auth/register
Content-Type: application/json
```

```json
{
  "username": "alice",
  "password": "123456"
}
```

第一步：

```text
Spring Boot
```

收到 HTTP 请求。

第二步：

```text
AuthController
```

匹配：

```java
@PostMapping("/register")
```

第三步：

```text
RegisterRequest
```

接收 JSON：

```json
{
  "username": "alice",
  "password": "123456"
}
```

第四步：

Controller 调用：

```java
userService.register(request)
```

第五步：

Service 查询：

```java
userRepository.existsByUsername("alice")
```

第六步：

如果不存在：

```java
new User("alice", "123456")
```

第七步：

```java
userRepository.save(user)
```

JPA 最终执行类似：

```sql
INSERT INTO users(username, password)
VALUES ('alice', '123456');
```

第八步：

数据库保存成功。

第九步：

返回：

```json
{
  "id": 1,
  "username": "alice"
}
```

第十步：

HTTP：

```text
201 Created
```

整个链路：

```text
Postman
  ↓
HTTP
  ↓
Controller
  ↓
DTO
  ↓
Service
  ↓
Repository
  ↓
JPA
  ↓
MySQL
  ↑
Entity
  ↑
DTO
  ↑
JSON
  ↑
HTTP
  ↑
Postman
```

如果你能真正理解这一条链路，就已经开始真正理解 Spring Boot 后端了。

---

# 十六、为什么项目结构不能全部写在一个文件？

初学者最容易写成：

```text
UserController.java

里面：
HTTP
数据库
业务
Session
异常
```

刚开始看起来非常快。

但是项目一大：

```text
几千行
```

你就很难维护。

所以我们拆成：

```text
controller
service
repository
entity
dto
session
exception
```

对应：

| 包 | 职责 |
|---|---|
| controller | HTTP 接口 |
| service | 业务逻辑 |
| repository | 数据库访问 |
| entity | 数据库实体 |
| dto | 请求/响应数据 |
| session | 登录状态 |
| exception | 异常处理 |

这就是最基础的分层架构。

---

# 十七、为什么不是一开始就上 JWT？

任务四要求的是：

```text
sessionId
```

所以先实现 Session。

第五题才要求：

```text
JWT
```

不要为了“看起来高级”而提前把所有东西都塞进来。

学习后端应该遵循：

```text
HTTP
 ↓
REST API
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
 ↓
Session
 ↓
JWT
 ↓
Redis
 ↓
微服务
```

一步一步增加复杂度。

如果连：

```text
POST
GET
HTTP Header
JSON
Controller
Service
Repository
MySQL
```

都没有真正理解，就直接上 JWT、Redis、Spring Security，反而容易变成“会复制代码，但不知道代码为什么这么写”。

---

# 十八、为什么 Maven 要管理依赖？

项目需要：

```text
Spring Boot
Spring Web
Spring Data JPA
MySQL Driver
Validation
Test
```

如果手动下载 jar：

```text
jar1
jar2
jar3
jar4
...
```

非常麻烦。

所以 Maven 使用：

```xml
<dependency>
    ...
</dependency>
```

告诉 Maven：

> 我需要这个依赖。

Maven 会负责：

```text
下载
版本管理
依赖关系
编译
测试
打包
```

所以：

```text
pom.xml
```

可以理解成：

**项目的依赖清单 + 构建配置。**

---

# 十九、application.yml 为什么单独存在？

数据库地址：

```text
localhost:3306
```

数据库：

```text
zeqi_user
```

用户名：

```text
root
```

这些属于配置，而不是业务代码。

所以放：

```text
application.yml
```

而不是：

```java
UserService.java
```

这样以后换数据库：

```text
开发环境
测试环境
生产环境
```

可以通过配置改变，而不用修改业务代码。

---

# 二十、这个项目还有哪些地方故意没有做到生产级？

这一点一定要知道。

### 1. 密码明文

任务四允许：

```text
密码明文存储
```

所以本项目按题目要求实现。

但真正项目绝对不能这样。

第五题要求：

```text
密码 Hash
Salt
```

可以进一步使用：

```text
BCrypt
```

---

### 2. Session 放内存

现在：

```java
ConcurrentHashMap
```

服务重启：

```text
Session 全部消失
```

多服务器：

```text
Session 不共享
```

生产环境可以：

```text
Redis
```

---

### 3. 没有完整权限系统

现在只有：

```text
登录 / 未登录
```

以后可以扩展：

```text
USER
ADMIN
```

然后实现：

```text
RBAC
```

---

### 4. 没有使用 Spring Security

任务四没有要求。

因此先不用。

等你理解：

```text
Session
HTTP Header
认证
授权
```

再学习 Spring Security 会更容易。

---

# 二十一、推荐你的学习顺序

不要一上来背 Spring 注解。

建议按照下面顺序：

## 第一阶段：HTTP

搞懂：

```text
GET
POST
PATCH
DELETE

URL
Header
Body
Status Code
JSON
```

---

## 第二阶段：Spring Boot

搞懂：

```text
@SpringBootApplication
@RestController
@RequestMapping
@GetMapping
@PostMapping
@RequestBody
@RequestHeader
```

目标：

**能写一个简单接口。**

---

## 第三阶段：分层

理解：

```text
Controller
 ↓
Service
 ↓
Repository
```

目标：

**知道代码为什么要拆成三个地方。**

---

## 第四阶段：数据库

学习：

```text
MySQL
SQL
主键
唯一约束
INSERT
SELECT
UPDATE
DELETE
```

然后理解：

```text
JPA
```

---

## 第五阶段：认证

先理解：

```text
Session
```

再学习：

```text
JWT
```

最后：

```text
Spring Security
```

---

# 二十二、最终应该形成的后端思维

以后拿到一个后端需求，不要第一反应就是：

> “我要写哪个注解？”

而应该问：

### 1. 数据是什么？

例如：

```text
User
```

### 2. 数据存在哪里？

```text
MySQL
```

### 3. 客户端通过什么方式操作？

```text
HTTP API
```

### 4. 哪些是业务逻辑？

```text
注册
登录
修改密码
```

### 5. 怎么判断用户身份？

```text
Session
```

### 6. 代码怎么分层？

```text
Controller
Service
Repository
```

### 7. 出错怎么办？

```text
400
401
404
409
统一异常处理
```

当你能按照这个思路分析问题时，Spring Boot 就不再是“背框架”，而是在用框架实现后端系统。

---

# 二十三、把任务四浓缩成一句话

任务四实际上是在让你完成：

```text
HTTP API
   +
Java
   +
Spring Boot
   +
分层架构
   +
MySQL
   +
JPA
   +
Session
   +
状态码
```

真正需要掌握的不是这几个文件怎么复制，而是：

> **一个用户请求如何从 HTTP 进入后端，经过 Controller、Service、Repository，最终访问数据库，再把结果变成 JSON 返回给客户端。**

这条链路是你后面继续学习 Spring Boot、Spring Security、Redis、JWT、微服务等内容的基础。

---
# 第五题学习笔记：JWT、密码安全、统一异常与日志

第五题要求在第四题用户系统基础上增加 JWT、密码哈希、统一异常处理和日志。fileciteturn0file0L212-L240

## 1. 密码为什么不能明文？
数据库泄露时明文会直接暴露。密码验证只需要判断“输入是否正确”，因此使用单向哈希更合适。本项目用 BCrypt：注册时 `encode`，登录时 `matches`。

## 2. Hash 和 Salt
Hash 是单向摘要；Salt 是加入哈希过程的随机值，避免相同密码产生相同结果并增加预计算攻击成本。BCrypt 已内置盐值处理。

## 3. JWT 三部分
`Header.Payload.Signature`：Header 描述算法和类型，Payload 保存 claims，Signature 用服务端密钥签名。Payload 只是编码，不是加密，不应放密码等敏感信息。

## 4. JWT 请求链路
```text
Postman
 ↓ Authorization: Bearer <token>
JwtAuthInterceptor
 ↓ 验证签名/过期时间
userId
 ↓
Controller → Service → Repository → MySQL
```
缺少、伪造或过期 Token 返回 401。

## 5. 为什么用拦截器？
认证是多个接口都会需要的横切逻辑。集中放到 `JwtAuthInterceptor`，避免每个 Controller 重复解析 Token。

## 6. Session 和 JWT
Session：`sessionId → 服务端状态 → userId`；JWT：`Token → 服务端验证签名 → userId`。JWT 无状态，因此注销不像 Session 那样通过删除服务端 session 立即失效；需要立即撤销可增加黑名单/版本号/Redis。

## 7. 统一异常
`@RestControllerAdvice` 集中处理 `ApiException` 和参数异常，统一返回 `{"message":"..."}`，避免 Controller 到处 try-catch。

## 8. 日志
记录注册、登录成功/失败、修改密码、注销。不要打印密码、JWT、sessionId 等敏感凭证。

## 9. 推荐学习顺序
```text
HTTP → Controller → Service → Repository → MySQL
→ Session → Hash/Salt/BCrypt → 统一异常 → 日志
→ JWT → Bearer Token → 拦截器 → Spring Security
```

第五题真正要理解的是：**服务器如何确认一个请求携带的身份凭证没有被伪造或篡改。**
