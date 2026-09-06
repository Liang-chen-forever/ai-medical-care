# 后端鉴权与预约并发安全实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将系统升级为具备 BCrypt 密码保护、JWT 鉴权、归属授权与 MySQL 原子扣减号源的 `/api/v1` 后端。

**Architecture:** 保持 Spring Boot 单体和 MyBatis-Plus 分层。JWT 拦截器负责身份验证，`@LoginUser` 将用户身份注入 Controller；事务服务执行预约、取消和号源回补，MySQL 条件更新是余号最终事实来源。

**Tech Stack:** Java 17、Spring Boot 3.2、MyBatis-Plus、Jakarta Validation、BCrypt、JJWT 0.12.6、MySQL、JUnit 5、Mockito。

**Spec:** `docs/superpowers/specs/2026-09-01-intelligent-medical-care-design.md`

## Global Constraints

- 保留工作区已有的未提交改动，不 reset、checkout、clean 或自动提交。
- v1 接口前缀为 `/api/v1`，响应固定为 `ApiResponse<T>{code,message,data}`。
- 密钥只来自环境变量或 Git 忽略的 `secrets.local.txt`；不得把真实值提交进仓库。
- 密码只保存 BCrypt 哈希，预约创建只接收 `scheduleId`，身份只从 Bearer JWT 取得。
- 预约和取消必须用 `@Transactional`；Redis 不作为库存判定依据。
- 新测试必须不访问 MySQL、MongoDB、Redis、模型服务或真实 API Key。

---

### Task 1: 依赖、环境配置与预约表迁移

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Modify: `secrets.example.txt`
- Modify: `secrets.local.txt`（Git 忽略文件）
- Modify: `src/main/resources/init.sql`
- Modify: `src/main/resources/db/init-data.sql`
- Create: `src/main/resources/db/migration/V2__secure_appointments.sql`

**Interfaces:**
- `app.jwt.secret`、`app.jwt.expiration-seconds`、`app.cors.allowed-origins`
- `appointment.user_id`、`appointment.schedule_id`、唯一键 `uk_appointment_user_schedule(user_id,schedule_id)`

- [ ] **Step 1: 加入安全依赖**

在 `pom.xml` 增加：

```xml
<dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-crypto</artifactId></dependency>
<dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.6</version></dependency>
<dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
<dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
```

- [ ] **Step 2: 写入无敏感值的配置契约**

在 `application.properties` 加入：

```properties
app.jwt.secret=${JWT_SECRET:}
app.jwt.expiration-seconds=${JWT_EXPIRATION_SECONDS:86400}
app.cors.allowed-origins=${APP_CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:5174}
```

在 `secrets.example.txt` 增加 `JWT_SECRET=replace_with_at_least_32_random_characters`，并在已忽略的 `secrets.local.txt` 写入一个本地随机 32 字符以上密钥。

- [ ] **Step 3: 定义新旧数据库结构**

为两份初始化脚本的 `appointment` 表加上：

```sql
user_id BIGINT NOT NULL COMMENT '预约所属用户ID',
schedule_id BIGINT NOT NULL COMMENT '关联排班ID',
UNIQUE KEY uk_appointment_user_schedule (user_id, schedule_id),
KEY idx_appointment_user_id (user_id),
KEY idx_appointment_schedule_id (schedule_id)
```

创建迁移文件，明确已有预约需先回填或删除；文件按顺序添加 `user_id`、`schedule_id`、唯一键和普通索引。本阶段不自动执行迁移，因为项目尚未接入 Flyway。

- [ ] **Step 4: 验证依赖解析**

Run: `mvn -DskipTests compile`

Expected: `BUILD SUCCESS`。

### Task 2: 统一响应、请求 DTO 和异常处理

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/common/ApiResponse.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/common/BusinessException.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/common/GlobalExceptionHandler.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/auth/LoginRequest.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/auth/RegisterRequest.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/auth/AuthResponse.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/appointment/CreateAppointmentRequest.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/controller/AppointmentControllerTest.java`

**Interfaces:**
- `ApiResponse.success(T data)` 返回 `code=200`。
- `BusinessException(HttpStatus,int,String)` 表示预期业务错误。
- `CreateAppointmentRequest(Long scheduleId)` 只允许正数 ID。

- [ ] **Step 1: 写失败的校验测试**

```java
mockMvc.perform(post("/api/v1/appointments")
        .contentType(MediaType.APPLICATION_JSON).content("{}"))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.code").value(400))
    .andExpect(jsonPath("$.message").value("scheduleId不能为空"));
```

- [ ] **Step 2: 运行测试确认缺失**

Run: `mvn -Dtest=AppointmentControllerTest test`

Expected: 初始编译失败，因为 v1 Controller、DTO、统一错误契约均不存在。

- [ ] **Step 3: 以最小契约实现响应与 DTO**

实现 `record ApiResponse<T>(int code,String message,T data)`，成功工厂为 `new ApiResponse<>(200,"success",data)`。`GlobalExceptionHandler` 映射：`MethodArgumentNotValidException` 到 `400 + 第一条校验错误`、`BusinessException` 到其 status/code/message、非法 JSON 到 `请求参数格式错误`、未知错误到 `系统繁忙，请稍后重试`。

```java
public record LoginRequest(
    @NotBlank(message = "用户名不能为空") String username,
    @NotBlank(message = "密码不能为空") String password) {}

public record CreateAppointmentRequest(
    @NotNull(message = "scheduleId不能为空")
    @Positive(message = "scheduleId必须大于0") Long scheduleId) {}
```

`RegisterRequest` 要求用户名 3–50、密码 6–72、身份证 18 位、手机号可选但不超过 20；`AuthResponse` 仅含 token、用户资料，不含密码。

- [ ] **Step 4: 重新运行校验测试**

Run: `mvn -Dtest=AppointmentControllerTest test`

Expected: 在 Task 5 添加 Controller 后，HTTP 400、code 400、错误信息三项断言均通过。

### Task 3: BCrypt 注册登录与 JWT 令牌

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/UserPrincipal.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/JwtProperties.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/JwtTokenService.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/MedicalCareApp.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/UserService.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/UserServiceImpl.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/controller/AuthController.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/config/CorsConfig.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/auth/JwtTokenServiceTest.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/service/UserServiceImplTest.java`

**Interfaces:**
- `UserService.register(RegisterRequest): User`
- `UserService.authenticate(LoginRequest): User`
- `JwtTokenService.createToken(UserPrincipal): String`
- `JwtTokenService.parseToken(String): UserPrincipal`
- `POST /api/v1/auth/register`、`POST /api/v1/auth/login`

- [ ] **Step 1: 写失败的认证单测**

```java
@Test
void registerHashesPasswordBeforeSaving() {
    when(userMapper.selectOne(any())).thenReturn(null);
    when(userMapper.insert(any(User.class))).thenReturn(1);
    userService.register(new RegisterRequest("alice", "plain-text", VALID_ID_CARD, "13800000000"));
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userMapper).insert(saved.capture());
    assertThat(passwordEncoder.matches("plain-text", saved.getValue().getPassword())).isTrue();
}

@Test
void createsAndParsesTokenForTheSameUser() {
    JwtTokenService tokens = new JwtTokenService("01234567890123456789012345678901", 3600);
    assertThat(tokens.parseToken(tokens.createToken(new UserPrincipal(7L, "alice"))))
        .isEqualTo(new UserPrincipal(7L, "alice"));
}
```

- [ ] **Step 2: 运行认证测试，确认失败**

Run: `mvn -Dtest=UserServiceImplTest,JwtTokenServiceTest test`

Expected: 编译失败，因新签名、JWT 服务未实现。

- [ ] **Step 3: 实现认证核心**

实现 `record UserPrincipal(Long userId,String username){}`。`JwtProperties` 使用 `@ConfigurationProperties(prefix="app.jwt")`，启动类通过 `@EnableConfigurationProperties(JwtProperties.class)` 激活。令牌用 HS256，subject 为 userId、claim `username` 为用户名；密钥为空或少于 32 字符时拒绝启动；畸形、过期和伪造 token 统一抛 `401 登录状态无效或已过期`。

`UserServiceImpl` 只按用户名查询并调用 `PasswordEncoder.matches`。缺失用户或密码不匹配抛 `401 用户名或密码错误`。注册校验用户名和身份证唯一性，使用 `PasswordEncoder.encode`，冲突抛 `409 用户名或身份证号已被注册`。

控制器映射为 `/api/v1/auth` 并使用 `@Valid DTO`。成功登录使用：

```java
String token = jwtTokenService.createToken(new UserPrincipal(user.getId(), user.getUsername()));
return ApiResponse.success(new AuthResponse(token, user.getId(), user.getUsername(), user.getIdCard(), user.getPhone()));
```

CORS 从 `app.cors.allowed-origins` 读取逗号分隔白名单，只放行 GET、POST、DELETE 与 `Content-Type`、`Authorization`，不得使用通配 origin 与 credentials 的组合。

- [ ] **Step 4: 验证认证单测**

Run: `mvn -Dtest=UserServiceImplTest,JwtTokenServiceTest test`

Expected: `BUILD SUCCESS`；密码捕获值为 BCrypt 哈希，token 往返可恢复 userId 7 和 alice。

### Task 4: JWT 拦截和当前用户注入

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/LoginUser.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/CurrentUser.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/LoginRequiredInterceptor.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/LoginUserArgumentResolver.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/config/WebMvcConfig.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/controller/AppointmentControllerTest.java`

**Interfaces:**
- `@LoginUser UserPrincipal` 只在验证 Bearer JWT 后解析。
- `/api/v1/appointments/**` 必须登录；auth、departments、schedules、consultations 保持公开。

- [ ] **Step 1: 写未登录失败测试**

```java
mockMvc.perform(get("/api/v1/appointments/me"))
    .andExpect(status().isUnauthorized())
    .andExpect(jsonPath("$.code").value(401))
    .andExpect(jsonPath("$.message").value("请先登录"));
```

- [ ] **Step 2: 运行测试确认路由未受保护**

Run: `mvn -Dtest=AppointmentControllerTest test`

Expected: 初始结果为 404 或非 401。

- [ ] **Step 3: 实现精确的拦截器行为**

定义 `CurrentUser.REQUEST_ATTRIBUTE = "CURRENT_USER"` 和 `@LoginUser` 参数注解。拦截器严格执行：

```java
String header = request.getHeader(HttpHeaders.AUTHORIZATION);
if (header == null || !header.startsWith("Bearer ")) {
    throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "请先登录");
}
request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, jwtTokenService.parseToken(header.substring(7)));
return true;
```

`WebMvcConfig` 只对 `/api/v1/appointments/**` 注册此拦截器，并注册仅支持 `@LoginUser UserPrincipal` 的 resolver；请求属性缺失时也返回 401。

- [ ] **Step 4: 验证未登录结果**

Run: `mvn -Dtest=AppointmentControllerTest test`

Expected: `/api/v1/appointments/me` 以标准 JSON 返回 HTTP 401。

### Task 5: 事务预约、取消回补和 v1 Controller

**Files:**
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/entity/Appointment.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/mapper/AppointmentMapper.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/mapper/ScheduleMapper.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/AppointmentBookingService.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/AppointmentBookingServiceImpl.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/controller/AppointmentController.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/service/AppointmentBookingServiceImplTest.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/controller/AppointmentControllerTest.java`

**Interfaces:**
- `ScheduleMapper.decrementIfAvailable(Long): int`
- `ScheduleMapper.incrementIfBooked(Long): int`
- `AppointmentBookingService.book(Long,Long): Appointment`
- `AppointmentBookingService.listMine(Long): List<Appointment>`
- `AppointmentBookingService.cancel(Long,Long): void`

- [ ] **Step 1: 写满号和越权取消的失败测试**

```java
when(scheduleMapper.selectById(101L)).thenReturn(schedule(101L, 20, 20));
when(appointmentMapper.existsByUserIdAndScheduleId(7L, 101L)).thenReturn(false);
when(scheduleMapper.decrementIfAvailable(101L)).thenReturn(0);
assertThatThrownBy(() -> bookingService.book(7L, 101L))
    .isInstanceOf(BusinessException.class).hasMessage("该时段号源已约满");
verify(appointmentMapper, never()).insert(any());

when(appointmentMapper.selectById(55L)).thenReturn(appointmentOwnedBy(8L, 101L));
assertThatThrownBy(() -> bookingService.cancel(7L, 55L))
    .isInstanceOf(BusinessException.class).hasMessage("无权操作该预约");
verify(scheduleMapper, never()).incrementIfBooked(anyLong());
```

- [ ] **Step 2: 运行测试确认事务服务缺失**

Run: `mvn -Dtest=AppointmentBookingServiceImplTest test`

Expected: 编译失败，事务服务与原子 Mapper 方法均不存在。

- [ ] **Step 3: 实现 Mapper 原子更新与事务规则**

`Appointment` 增加 `userId` 和 `scheduleId`。`ScheduleMapper` 增加：

```java
@Update("UPDATE schedule SET booked_slots = booked_slots + 1 WHERE id = #{scheduleId} AND booked_slots < total_slots")
int decrementIfAvailable(@Param("scheduleId") Long scheduleId);

@Update("UPDATE schedule SET booked_slots = booked_slots - 1 WHERE id = #{scheduleId} AND booked_slots > 0")
int incrementIfBooked(@Param("scheduleId") Long scheduleId);
```

`AppointmentMapper` 增加按 `user_id` 查询、按 `id + user_id` 查询和 `user_id + schedule_id` 检查。`book` 和 `cancel` 加 `@Transactional(rollbackFor=Exception.class)`。

`book` 依次执行：查询排班（404 `排班不存在`）→ 检查重复（409 `请勿重复预约`）→ 条件扣减（0 行则 409 `该时段号源已约满`）→ 从排班复制医生、科室、日期、时段快照 → 插入预约。`DuplicateKeyException` 映射 409 `请勿重复预约`，事务将已扣号源回滚。

`cancel` 依次执行：查询预约（404 `预约不存在`）→ 比较 userId（不等则 403 `无权操作该预约`）→ 删除预约 → 回补号源；回补非 1 行时抛 `409 预约状态异常，请稍后重试` 以回滚删除。`listMine` 只查当前 userId，按 id 降序。

- [ ] **Step 4: 实现受保护的 Controller 并验证**

Controller 映射改为 `/api/v1/appointments`，不接收姓名、身份证或预约实体。创建接口为：

```java
@PostMapping
public ApiResponse<Appointment> book(@LoginUser UserPrincipal user,
        @Valid @RequestBody CreateAppointmentRequest request) {
    return ApiResponse.success(bookingService.book(user.userId(), request.scheduleId()));
}
```

实现 `GET /me` 与 `DELETE /{id}`。在 MVC 测试中使用 userId 为 7 的有效 token，并验证：

```java
verify(bookingService).book(7L, 101L);
```

Run: `mvn -Dtest=AppointmentBookingServiceImplTest,AppointmentControllerTest test`

Expected: `BUILD SUCCESS`；覆盖满号、成功预约、重复预约、取消回补、越权取消、请求校验、无 token 和 token 用户注入。

### Task 6: 阶段验证与小程序交接

**Files:**
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/tools/AppointmentTools.java`（仅解决新字段造成的编译兼容）
- Modify: `src/main/resources/mapper/AppointmentMapper.xml`（没有调用后移除旧查询）
- Modify: `docs/技术栈文档.md`

**Interfaces:**
- `/api/v1/appointments` 是唯一面向用户的预约写入口。
- 下一阶段小程序以 Bearer token 调用新契约，预约 body 固定为 `{ "scheduleId": 101 }`。

- [ ] **Step 1: 搜索废弃接口和明文密码查询**

Run: `rg -n 'api/appointment|eq\(User::getPassword|/api/auth' src/main/java frontend`

Expected: 后端 Controller 不再有旧 `/api/appointment` 或 `/api/auth` 映射，且没有 `eq(User::getPassword, ...)`；客户端旧引用只记录，不在本阶段静默改写。

- [ ] **Step 2: 执行完整聚焦验证**

Run: `mvn -Dtest=JwtTokenServiceTest,UserServiceImplTest,AppointmentBookingServiceImplTest,AppointmentControllerTest test`

Expected: `BUILD SUCCESS`。

Run: `mvn -DskipTests package`

Expected: `BUILD SUCCESS`。不运行旧 `@SpringBootTest`，它们依赖外部数据库和模型服务；交付时需说明这一点。

- [ ] **Step 3: 审查改动范围**

Run: `git diff --check`

Expected: 无空白错误。

Run: `git status --short -- pom.xml src/main/java src/main/resources docs secrets.example.txt .gitignore`

Expected: 仅出现计划内文件和原有用户未提交改动；不暂存、不提交共享脏工作区。

- [ ] **Step 4: 更新项目技术文档**

在 `docs/技术栈文档.md` 写明：认证使用 BCrypt + JWT；预约利用 MySQL 条件更新和事务保证号源；小程序下一阶段需在 `Authorization` 头传 Bearer token，并改为 `POST /api/v1/appointments`、`{ "scheduleId": 101 }`。
