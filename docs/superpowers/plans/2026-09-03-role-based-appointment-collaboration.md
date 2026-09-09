# Role-Based Appointment Collaboration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the patient-only appointment demo into a secure patient-doctor collaboration flow with RBAC and an explicit appointment state machine.

**Architecture:** Keep the Spring Boot monolith and MyBatis-Plus. JWT expands from identity-only to a role-bearing principal; service methods own resource authorization and transactions. MySQL conditional updates remain the capacity authority, while Vue adds a compact doctor workflow beside the existing patient client.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, MySQL 8, JJWT, BCrypt, JUnit 5, Mockito, MockMvc, Vue 3, Vite.

**Spec:** `docs/superpowers/specs/2026-09-03-trusted-triage-appointment-design.md`

## Global Constraints

- Preserve existing uncommitted user changes; never reset, clean, checkout, amend, or stage unrelated paths.
- Keep `ApiResponse<T>{code,message,data}` and the `/api/v1` API prefix.
- Move every usable key, password and JWT secret to an environment variable or ignored `secrets.local.txt`; rotate already exposed credentials outside the repository.
- JWT is the only source of identity and role. Requests never contain owner, doctor, patient, or role IDs.
- Do not add Spring Security, a microservice, a queue, a distributed lock, or another data store.
- `PENDING` and `CONFIRMED` reservations occupy capacity. A transition to `CANCELLED`, `REJECTED`, or `EXPIRED` restores it exactly once.
- Tests must use mocks/test configuration only: no database, MongoDB, Redis or model provider.

---

### Task 1: Secure configuration and isolate chat conversations

**Files:**
- Modify: `src/main/resources/application.properties`
- Modify: `secrets.example.txt`
- Modify: `.gitignore`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/config/WebMvcConfig.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/controller/XiaozhiController.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/bean/ChatForm.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/assistant/XiaozhiAgent.java`
- Modify: `frontend/src/api/index.js`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/controller/XiaozhiControllerTest.java`

**Interfaces:**
- Produces: `POST /api/v1/chat/conversations/{conversationId}/messages`.
- Produces: `XiaozhiAgent#chat(String memoryId, String message, String date)` where memory ID is `${userId}:${conversationId}`.

- [ ] **Step 1: Write the failing authentication and ownership tests**

```java
@Test
void chatRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/v1/chat/conversations/9/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userMessage\":\"头痛\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401));
}

@Test
void chatBuildsMemoryKeyFromAuthenticatedUser() throws Exception {
    mockMvc.perform(post("/api/v1/chat/conversations/9/messages")
            .header(HttpHeaders.AUTHORIZATION, bearerFor(new UserPrincipal(7L, "alice")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userMessage\":\"头痛\"}"))
        .andExpect(status().isOk());
    verify(xiaozhiAgent).chat(eq("7:9"), eq("头痛"), anyString());
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -Dtest=XiaozhiControllerTest test`

Expected: FAIL because `/xiaozhi/chat` is public and accepts client-owned `memoryId`.

- [ ] **Step 3: Implement environment-only configuration and protected chat**

Replace every literal secret with an empty environment-backed value:

```properties
jwt.user-secret-key=${JWT_USER_SECRET_KEY:}
jwt.admin-secret-key=${JWT_ADMIN_SECRET_KEY:}
spring.datasource.password=${MYSQL_PASSWORD:}
langchain4j.open-ai.chat-model.api-key=${DASH_SCOPE_API_KEY:}
langchain4j.open-ai.streaming-chat-model.api-key=${DASH_SCOPE_API_KEY:}
langchain4j.open-ai.embedding-model.api-key=${DASH_SCOPE_API_KEY:}
```

Ignore `.env` and `secrets.local.txt`; `secrets.example.txt` contains variable names and non-working samples only. Replace `ChatForm` with:

```java
public record ChatForm(
    @NotBlank(message = "消息不能为空")
    @Size(max = 1000, message = "消息不能超过1000个字符") String userMessage) {}
```

Map the controller to `POST /api/v1/chat/conversations/{conversationId}/messages`; inject `@LoginUser UserPrincipal`, validate positive `conversationId`, and call `xiaozhiAgent.chat(user.userId() + ":" + conversationId, form.userMessage(), LocalDate.now().toString())`. Update the agent memory parameter to `String` and add `/api/v1/chat/**` to the login interceptor. Do not retain a JSON `memoryId` compatibility path.

Keep the existing locally generated numeric chat ID as a client-only `conversationId`; update both `chatWithAI(conversationId, userMessage)` implementations to post `{ userMessage }` to `/api/v1/chat/conversations/${conversationId}/messages`. No browser or mini-program request includes `memoryId`.

- [ ] **Step 4: Run focused verification**

Run: `mvn -Dtest=XiaozhiControllerTest test`

Expected: PASS; anonymous calls return JSON 401 and user 7 gets memory ID `7:9`.

Run: `rg -n "sk-[A-Za-z0-9]|spring.datasource.password=[^$]|secret-key=[^$]" src/main/resources secrets.example.txt`

Expected: no usable credential.

- [ ] **Step 5: Commit the isolated security baseline**

Stage only Task 1 files, then commit with message `security: isolate chat sessions and external credentials`.

### Task 2: Add roles to users and JWT claims

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/UserRole.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/RequireRole.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/auth/RoleRequiredInterceptor.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/auth/UserPrincipal.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/auth/JwtTokenService.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/entity/User.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/entity/Doctor.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/UserServiceImpl.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/controller/AuthController.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/dto/auth/AuthResponse.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/config/WebMvcConfig.java`
- Modify: `src/main/resources/db/init-data.sql`
- Create: `src/main/resources/db/migration/V3__roles_and_doctor_accounts.sql`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/auth/JwtTokenServiceTest.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/auth/RoleRequiredInterceptorTest.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/service/UserServiceImplTest.java`

**Interfaces:**
- Produces: `enum UserRole { PATIENT, DOCTOR, ADMIN }`.
- Produces: `record UserPrincipal(Long userId, String username, UserRole role)`.
- Produces: `@RequireRole(UserRole... value)`.

- [ ] **Step 1: Write the failing role tests**

```java
@Test
void registrationAlwaysAssignsPatientRole() {
    when(userMapper.selectOne(any())).thenReturn(null);
    when(userMapper.insert(any(User.class))).thenReturn(1);
    userService.register(new RegisterRequest("alice", "plain-text", VALID_ID_CARD, "13800000000"));
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userMapper).insert(saved.capture());
    assertThat(saved.getValue().getRole()).isEqualTo(UserRole.PATIENT);
}

@Test
void tokenRoundTripRetainsRole() {
    UserPrincipal source = new UserPrincipal(7L, "doctor1", UserRole.DOCTOR);
    assertThat(tokens.parseToken(tokens.createToken(source))).isEqualTo(source);
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -Dtest=UserServiceImplTest,JwtTokenServiceTest,RoleRequiredInterceptorTest test`

Expected: FAIL because role types, claims, and interceptor do not exist.

- [ ] **Step 3: Implement role persistence and strict authorization**

Create `UserRole` with `PATIENT`, `DOCTOR`, `ADMIN`. Add `UserRole role` to `User`, `Long userId` to `Doctor`, and always set `PATIENT` in public registration. Fresh SQL adds `user.role VARCHAR(16) NOT NULL DEFAULT 'PATIENT'`, `doctor.user_id BIGINT NULL`, and `uk_doctor_user_id`. V3 uses `information_schema` dynamic SQL to add columns/index once, backfills legacy users as `PATIENT`, and never creates login accounts for legacy doctors.

Extend `UserPrincipal`; add JWT claim `role=principal.role().name()`. Missing or invalid role claim must return `401 登录状态无效或已过期`, never a default role. Add `@RequireRole(UserRole... value)` and an interceptor that reads it from a `HandlerMethod`; mismatches throw `BusinessException(HttpStatus.FORBIDDEN, 403, "无权访问该资源")`. Register it after login for `/api/v1/doctor/**` and `/api/v1/admin/**`. Add `UserRole role` to `AuthResponse`, then make `AuthController` issue a role-bearing principal and response.

- [ ] **Step 4: Run role regressions**

Run: `mvn -Dtest=UserServiceImplTest,JwtTokenServiceTest,RoleRequiredInterceptorTest,AppointmentControllerTest test`

Expected: PASS; patient routes retain access, invalid role tokens fail, and patients receive JSON 403 at doctor routes.

- [ ] **Step 5: Commit the role contract**

Stage only Task 2 files, then commit with message `feat: add role-based API access`.

### Task 3: Implement doctor-owned appointment lifecycle transitions

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/appointment/AppointmentStatus.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/entity/Appointment.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/mapper/AppointmentMapper.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/mapper/DoctorMapper.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/AppointmentBookingService.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/AppointmentBookingServiceImpl.java`
- Modify: `src/main/resources/db/init-data.sql`
- Create: `src/main/resources/db/migration/V4__appointment_lifecycle.sql`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/service/AppointmentBookingServiceImplTest.java`

**Interfaces:**
- Produces: `AppointmentStatus { PENDING, CONFIRMED, COMPLETED, CANCELLED, REJECTED, EXPIRED, LEGACY }`.
- Produces: `confirmByDoctor(Long doctorUserId, Long appointmentId)`, `rejectByDoctor(Long doctorUserId, Long appointmentId, String reason)`, `completeByDoctor(Long doctorUserId, Long appointmentId)`.
- Consumes: authenticated doctor user ID only; service resolves doctor record and schedule ownership.

- [ ] **Step 1: Write failing lifecycle and capacity tests**

```java
@Test
void doctorCanConfirmOnlyOwnPendingAppointment() {
    when(doctorMapper.selectOne(any())).thenReturn(doctor(42L, 17L));
    when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, PENDING));
    when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));
    when(appointmentMapper.transitionStatus(55L, PENDING, CONFIRMED, 17L, null)).thenReturn(1);

    bookingService.confirmByDoctor(17L, 55L);

    verify(appointmentMapper).transitionStatus(55L, PENDING, CONFIRMED, 17L, null);
    verifyNoInteractions(scheduleMapper);
}

@Test
void rejectedConfirmedAppointmentRestoresCapacityOnce() {
    when(doctorMapper.selectOne(any())).thenReturn(doctor(42L, 17L));
    when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, CONFIRMED));
    when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));
    when(appointmentMapper.transitionStatus(55L, CONFIRMED, REJECTED, 17L, "医生拒绝")).thenReturn(1);
    when(scheduleMapper.incrementIfBooked(101L)).thenReturn(1);

    bookingService.rejectByDoctor(17L, 55L, "医生拒绝");

    verify(scheduleMapper).incrementIfBooked(101L);
}
```

- [ ] **Step 2: Run tests to verify status behavior is absent**

Run: `mvn -Dtest=AppointmentBookingServiceImplTest test`

Expected: FAIL because status, doctor ownership and transition APIs are missing.

- [ ] **Step 3: Add state model, migration, and transaction-safe transitions**

Add `doctorId`, `status`, `cancelReason`, `handledBy`, and `handledAt` to `Appointment`. New bookings copy `doctorId` from the schedule. Fresh schema gives `doctor_id BIGINT NOT NULL`, `status VARCHAR(16) NOT NULL DEFAULT 'PENDING'`, `idx_appointment_doctor_status (doctor_id, status)`, and `idx_appointment_user_status (user_id, status)`. V4 adds nullable fields safely, backfills `doctor_id` by joining `schedule` where possible and assigns historical records `LEGACY`; historical entries must not participate in the new workflow.

Add this mapper operation:

```java
@Update("UPDATE appointment SET status = #{to}, handled_by = #{handledBy}, " +
        "handled_at = CURRENT_TIMESTAMP, cancel_reason = #{reason} " +
        "WHERE id = #{id} AND status = #{from}")
int transitionStatus(@Param("id") Long id, @Param("from") AppointmentStatus from,
                     @Param("to") AppointmentStatus to, @Param("handledBy") Long handledBy,
                     @Param("reason") String reason);
```

New booking writes `PENDING`. Patient cancellation conditionally transitions `PENDING`/`CONFIRMED` to `CANCELLED`; only an affected row of `1` triggers `incrementIfBooked`. A zero-row transition returns `409 预约状态已变更，请刷新后重试` and never changes capacity.

The doctor service resolves `Doctor.userId=doctorUserId`, then verifies the appointment schedule has the same `doctorId`. Confirm permits only `PENDING -> CONFIRMED`; reject permits `PENDING|CONFIRMED -> REJECTED` then restores capacity; completion permits only `CONFIRMED -> COMPLETED` and never changes capacity. Each write method is `@Transactional(rollbackFor = Exception.class)`.

- [ ] **Step 4: Run lifecycle regressions**

Run: `mvn -Dtest=AppointmentBookingServiceImplTest,AppointmentControllerTest test`

Expected: PASS; coverage includes wrong doctor 403, duplicate transition 409, cancellation, rejection restoration, and completion without restoration.

- [ ] **Step 5: Commit the state machine**

Stage only Task 3 files, then commit with message `feat: add doctor appointment lifecycle`.

### Task 4: Expose doctor APIs and a role-aware Web work queue

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/doctor/RejectAppointmentRequest.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/doctor/DoctorAppointmentResponse.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/controller/DoctorAppointmentController.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/AppointmentBookingService.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/AppointmentBookingServiceImpl.java`
- Modify: `frontend/src/api/index.js`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/views/MyAppointments.vue`
- Create: `frontend/src/views/DoctorAppointments.vue`
- Create: `frontend/test/doctor-appointments.test.js`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/controller/DoctorAppointmentControllerTest.java`

**Interfaces:**
- Produces: `GET /api/v1/doctor/appointments?status=PENDING` and doctor `confirm`, `reject`, `complete` actions.
- Produces: `/doctor/appointments` visible only to `DOCTOR`.
- Consumes: `@LoginUser UserPrincipal`, `@RequireRole(UserRole.DOCTOR)`, and Task 3 transitions.

- [ ] **Step 1: Write failing doctor controller and client tests**

```java
@Test
void doctorCanConfirmOwnPendingAppointment() throws Exception {
    mockMvc.perform(post("/api/v1/doctor/appointments/55/confirm").header(HttpHeaders.AUTHORIZATION, doctorBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    verify(bookingService).confirmByDoctor(17L, 55L);
}

@Test
void patientCannotAccessDoctorQueue() throws Exception {
    mockMvc.perform(get("/api/v1/doctor/appointments").header(HttpHeaders.AUTHORIZATION, patientBearer()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("无权访问该资源"));
}
```

```js
it('uses the doctor endpoint without a doctor ID in the request', async () => {
  await confirmDoctorAppointment(55)
  expect(post).toHaveBeenCalledWith('/api/v1/doctor/appointments/55/confirm')
})
```

- [ ] **Step 2: Run tests to verify endpoint and client are absent**

Run: `mvn -Dtest=DoctorAppointmentControllerTest test`

Expected: FAIL because the controller and list query do not exist.

Run: `npm test -- --run frontend/test/doctor-appointments.test.js`

Expected: FAIL because doctor API wrappers do not exist. If no frontend test runner exists, add Vitest using the existing Node support range before writing the test.

- [ ] **Step 3: Implement validation-first doctor endpoints**

Create:

```java
public record RejectAppointmentRequest(
    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 200, message = "拒绝原因不能超过200个字符") String reason) {}
```

Annotate `DoctorAppointmentController` at `/api/v1/doctor/appointments` with `@RequireRole(UserRole.DOCTOR)`. List only the authenticated doctor's appointments, filter by an optional valid `AppointmentStatus`, and return a DTO omitting `idCard`, password, phone, JWT, and arbitrary patient profile fields. Add `POST /{id}/confirm`, `POST /{id}/reject`, `POST /{id}/complete`; each accepts no actor identity and delegates `principal.userId()` to the service.

Persist `role` in frontend auth. Add API functions `getDoctorAppointments(status)`, `confirmDoctorAppointment(id)`, `rejectDoctorAppointment(id, reason)`, and `completeDoctorAppointment(id)` using only the server route and optional rejection body. Add a route guard requiring role `DOCTOR`; expose only that role's workbench navigation. Patient appointments show server status and a cancel control only for `PENDING`/`CONFIRMED`.

`DoctorAppointments.vue` uses a status filter and stable table/grid for appointment time, department and status. Confirm and complete are icon controls with tooltips; reject asks for a bounded reason. Disable an action while pending, then refresh from server after success.

- [ ] **Step 4: Run controller and frontend verification**

Run: `mvn -Dtest=DoctorAppointmentControllerTest,AppointmentControllerTest test`

Expected: PASS; a patient gets 403 before controller execution and a doctor action uses the authenticated user ID.

Run: `npm test -- --run frontend/test/doctor-appointments.test.js`

Expected: PASS.

Run: `npm run build`

Expected: Vite build succeeds from `frontend`.

- [ ] **Step 5: Document and commit the workflow**

Update `README.md` and `docs/tech-stack.md` with roles, migration order, state transition rules, and external credential setup. Stage only Task 4 files, then commit with message `feat: add role-aware appointment workflows`.

### Task 5: Verify and publish the first subproject boundary

**Files:**
- Modify: `README.md`
- Modify: `docs/tech-stack.md`

**Interfaces:**
- Consumes: Tasks 1-4.
- Produces: reproducible tests and an accurate project capability statement.

- [ ] **Step 1: Run all non-external backend tests**

Run: `mvn test`

Expected: BUILD SUCCESS; external model/database tests remain excluded.

- [ ] **Step 2: Run focused behavior tests**

Run: `mvn -Dtest=JwtTokenServiceTest,UserServiceImplTest,AppointmentBookingServiceImplTest,AppointmentControllerTest,DoctorAppointmentControllerTest,XiaozhiControllerTest,RoleRequiredInterceptorTest test`

Expected: BUILD SUCCESS; coverage includes roles, session isolation, state transitions and capacity recovery.

- [ ] **Step 3: Build and scan the delivery**

Run: `mvn -DskipTests package`

Expected: BUILD SUCCESS.

Run: `npm run build`

Expected: BUILD SUCCESS from `frontend`.

Run: `rg -n "sk-[A-Za-z0-9]|password=[^$]|secret-key=[^$]" src/main/resources secrets.example.txt`

Expected: no usable credential.

Run: `git diff --check`

Expected: no whitespace errors.

- [ ] **Step 4: Commit verification documentation**

After confirming all prior task commits exist, stage only the Task 5 documentation files and commit with message `docs: document appointment collaboration workflow`.

## Plan Self-Review

- **Spec coverage:** Task 1 implements the first security and conversation-ownership boundary. Task 2 implements user, doctor and admin roles. Tasks 3-4 implement the patient-doctor appointment lifecycle and first Web workflow. Candidate waitlists, structured triage, knowledge versions, admin management, Compose/CI and evaluation data are independent subsequent plans, not omitted requirements.
- **Placeholder scan:** Each task names files, types, routes, transitions, error messages, tests and commands. No step relies on an unstated interface.
- **Type consistency:** `UserRole` precedes the expanded `UserPrincipal`; `AppointmentStatus` precedes mapper transitions; Task 4 calls the exact Task 3 service methods.
