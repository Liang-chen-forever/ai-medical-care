# 后续能力实现实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在既有可信分诊和预约闭环之上实现候补、就诊摘要、知识库治理、审计追踪和交付工程。

**Architecture:** 沿用 Spring Boot 单体、MyBatis-Plus、MySQL、Redis、MongoDB 和 Vue 3 Web。新增能力通过独立迁移、领域服务和角色化 REST 接口接入；MySQL 负责库存一致性，Redis 只负责知识向量索引。

**Tech Stack:** Java 17 source target, Spring Boot 3.2, MyBatis-Plus, JUnit 5, Vue 3/Vite, Docker Compose, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-05-follow-up-capabilities-design.md`

## Global Constraints

- MySQL remains the source of truth for appointment capacity and status transitions.
- Every write endpoint derives identity and role from JWT; client-submitted ownership IDs are ignored or rejected.
- No password, JWT, API key, or complete medical complaint is written to audit records or generated evidence.
- Existing V2-V5 migrations and public API contracts remain backward compatible.
- Default `mvn test` must remain service-free; external tests stay under the `external` tag.

---

### Task 1: Waitlist domain and migration

**Files:**
- Create: `src/main/resources/db/migration/V6__waitlist_encounter_audit.sql`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/waitlist/WaitlistStatus.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/entity/WaitlistEntry.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/entity/Encounter.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/entity/AuditLog.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/mapper/WaitlistEntryMapper.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/mapper/EncounterMapper.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/mapper/AuditLogMapper.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/waitlist/WaitlistPriorityTest.java`

**Interfaces:**
- Produces `WaitlistStatus`, `WaitlistEntry`, `Encounter`, and `AuditLog` persistence contracts.
- Migration is idempotent and adds `appointment.triage_case_id`, `waitlist_entry`, `encounter`, and `audit_log`.

- [ ] **Step 1:** Write priority and status tests for risk ordering, FIFO tie-break, and legal transitions.
- [ ] **Step 2:** Run `mvn -Dtest=WaitlistPriorityTest test` and confirm RED.
- [ ] **Step 3:** Implement enums, entities, mappers, and V6 SQL with guarded columns/indexes.
- [ ] **Step 4:** Run the focused test and migration contract tests; confirm GREEN.
- [ ] **Step 5:** Commit `feat: add waitlist encounter and audit schema`.

### Task 2: Waitlist service and APIs

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/WaitlistService.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/WaitlistServiceImpl.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/controller/WaitlistController.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/waitlist/*`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/AppointmentBookingServiceImpl.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/controller/AppointmentController.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/service/WaitlistServiceImplTest.java`

**Interfaces:**
- `POST /api/v1/waitlist` joins the current patient.
- `GET /api/v1/waitlist/me` lists current patient entries.
- `POST /api/v1/waitlist/{id}/accept` accepts an unexpired offer.
- `POST /api/v1/waitlist/{id}/cancel` cancels a waiting entry.

- [ ] **Step 1:** Write tests for full schedule join, risk priority, offer acceptance, expiry rejection, and ownership checks.
- [ ] **Step 2:** Run the focused test and confirm RED.
- [ ] **Step 3:** Implement transactional queue selection, offer expiry, and conditional inventory updates.
- [ ] **Step 4:** Add controllers, DTO validation, and appointment triage-case linkage.
- [ ] **Step 5:** Run service/controller tests and commit `feat: add transactional waitlist flow`.

### Task 3: Encounter workflow and patient view

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/EncounterService.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/EncounterServiceImpl.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/controller/EncounterController.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/encounter/*`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/controller/DoctorAppointmentController.java`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/service/EncounterServiceImplTest.java`

**Interfaces:**
- `POST /api/v1/doctor/appointments/{id}/encounter` confirms a doctor-owned confirmed appointment and writes summary.
- `GET /api/v1/appointments/{id}/encounter` allows only the appointment patient or owning doctor to read.

- [ ] **Step 1:** Write tests for doctor ownership, confirmed-only completion, validation limits, and patient visibility.
- [ ] **Step 2:** Run focused tests and confirm RED.
- [ ] **Step 3:** Implement atomic appointment transition plus encounter insert in one transaction.
- [ ] **Step 4:** Add controller contracts and run appointment regression tests.
- [ ] **Step 5:** Commit `feat: add doctor encounter summaries`.

### Task 4: Knowledge document governance

**Files:**
- Create: `src/main/resources/db/migration/V7__knowledge_documents.sql`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeDocumentStatus.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/entity/KnowledgeDocument.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/mapper/KnowledgeDocumentMapper.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/KnowledgeDocumentService.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/KnowledgeDocumentServiceImpl.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/controller/KnowledgeDocumentController.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/knowledge/*`
- Test: `src/test/java/com/Liang/java/ai/langchain4j/service/KnowledgeDocumentServiceImplTest.java`

**Interfaces:**
- `GET /api/v1/admin/knowledge/documents`
- `POST /api/v1/admin/knowledge/documents` multipart text/markdown upload
- `POST /api/v1/admin/knowledge/documents/{id}/publish`
- `POST /api/v1/admin/knowledge/documents/{id}/rollback`

- [ ] **Step 1:** Write hash de-duplication, draft/publish/rollback, and 2 MiB validation tests.
- [ ] **Step 2:** Run focused tests and confirm RED.
- [ ] **Step 3:** Implement SHA-256 content hashing, immutable versions, and guarded status transitions.
- [ ] **Step 4:** Add admin controller and preserve existing seed reload behavior.
- [ ] **Step 5:** Commit `feat: add governed knowledge documents`.

### Task 5: Trace ID, audit events, and metrics

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/audit/TraceIdFilter.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/audit/AuditService.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/audit/impl/AuditServiceImpl.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/controller/AdminMetricsController.java`
- Modify: business services and `WebMvcConfig`.
- Test: `src/test/java/com/Liang/java/ai/langchain4j/audit/TraceIdFilterTest.java`

- [ ] **Step 1:** Write tests for generated/preserved trace IDs and redacted audit payloads.
- [ ] **Step 2:** Run focused tests and confirm RED.
- [ ] **Step 3:** Implement filter, audit persistence calls, and basic status-count metrics.
- [ ] **Step 4:** Add audit calls to appointment, triage, waitlist, encounter, and knowledge writes.
- [ ] **Step 5:** Commit `feat: add traceable audit events`.

### Task 6: Search evaluation and delivery files

**Files:**
- Create: `evaluation/triage-cases.jsonl`
- Create: `scripts/evaluate-triage.ps1`
- Create: `docker-compose.yml`
- Create: `.github/workflows/ci.yml`
- Modify: `README.md`, `docs/技术栈文档.md`, release validation runner.
- Test: `src/test/java/com/Liang/java/ai/langchain4j/release/MigrationScriptContractTest.java`

- [ ] **Step 1:** Add at least 30 synthetic, non-sensitive evaluation rows and a deterministic scoring script.
- [ ] **Step 2:** Add Compose services for MySQL, Redis Stack, MongoDB and the application with environment-only secrets.
- [ ] **Step 3:** Add CI jobs for Maven tests/package and frontend builds.
- [ ] **Step 4:** Extend release validation to V6/V7 and run static contracts.
- [ ] **Step 5:** Update deployment and resume-facing documentation; run all verification commands.
- [ ] **Step 6:** Commit `chore: add reproducible delivery workflow`.
