# Release Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reproducible, isolated external release validation for migrations, triage transaction rollback, and Redis knowledge reload.

**Architecture:** SQL migrations become schema-neutral and retain their idempotent guards. Application knowledge reload explicitly uses the configured embedding model and a configurable Redis store. Default tests prove contracts without services; `external`-tagged tests and a PowerShell runner operate only on generated validation resources.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, JUnit 5, MySQL 8/InnoDB, Redis Stack/RediSearch, PowerShell 7.

**Spec:** `docs/superpowers/specs/2026-09-04-release-validation-design.md`

## Global Constraints

- Never target `guiguxiaozhi`, `xiaozhi-index`, or `langchain4j:vector:xiaozhi:` from release validation.
- External test resources must use the `ai_medical_care_release_validation_` prefix and clean up only resources matching it.
- Keep all passwords and API keys in environment variables; never print or write their values.
- Preserve the default `mvn test` behavior by retaining JUnit's `external` exclusion.
- New behavior requires a failing test before production code.

---

### Task 1: Make Migration Execution Schema-Neutral

**Files:**
- Modify: `src/main/resources/db/migration/V2__secure_appointments.sql`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/release/MigrationScriptContractTest.java`

**Interfaces:**
- Consumes: migration files V2--V5 as classpath resources.
- Produces: migrations that operate on the selected database and static contract coverage for their guarded indexes.

- [ ] **Step 1: Write the failing migration contract test**

Add tests that load V2--V5, assert no migration contains `USE guiguxiaozhi`, assert V2--V4 query `DATABASE()`/`@schema_name`, and assert the eight expected index identifiers: `uk_appointment_user_schedule`, `idx_appointment_user_id`, `idx_appointment_schedule_id`, `uk_doctor_user_id`, `idx_appointment_doctor_status`, `idx_appointment_user_status`, `idx_triage_case_patient_created`, and `uk_triage_evidence_case_rank`.

- [ ] **Step 2: Run the test and verify RED**

Run: `mvn -Dtest=MigrationScriptContractTest test`

Expected: the test fails because V2 hard-codes `USE guiguxiaozhi`.

- [ ] **Step 3: Remove the schema selection from V2**

Delete only `USE guiguxiaozhi;`; retain its comments, `SET NAMES utf8mb4`, guarded column/index logic, and history-preserving behavior.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `mvn -Dtest=MigrationScriptContractTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/db/migration/V2__secure_appointments.sql src/test/java/com/Liang/java/ai/langchain4j/release/MigrationScriptContractTest.java
git commit -m "fix: make appointment migration schema neutral"
```

### Task 2: Bind Knowledge Reload to the Configured Embedding Model

**Files:**
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedLoader.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/config/RedisEmbeddingStoreConfig.java`
- Modify: `src/main/resources/application.properties`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/config/KnowledgeRedisProperties.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedLoaderTest.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/config/KnowledgeRedisPropertiesTest.java`

**Interfaces:**
- Consumes: `EmbeddingModel`, `EmbeddingStore<TextSegment>`, `app.knowledge.redis.host`, `.port`, `.index-name`, `.prefix`, `.dimension`.
- Produces: `KnowledgeSeedLoader(KnowledgeSeedCatalog, EmbeddingStore<TextSegment>, EmbeddingModel)` and a Redis store built from property values.

- [ ] **Step 1: Write failing tests**

Create a loader test using a mock `EmbeddingModel` and in-memory store. Stub `embedAll` with one 8-dimensional embedding per segment and assert `reload()` invokes that exact model and stores searchable documents. Create a properties-binding test that binds explicit `app.knowledge.redis.*` values and asserts all five values are exposed.

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest=KnowledgeSeedLoaderTest,KnowledgeRedisPropertiesTest test`

Expected: compilation/test failure because the loader does not accept an embedding model and properties do not exist.

- [ ] **Step 3: Implement minimal dependency injection and property binding**

Add a `@ConfigurationProperties("app.knowledge.redis")` bean with defaults matching current production values. Update `RedisEmbeddingStoreConfig` to enable and consume it. Update `KnowledgeSeedLoader` to construct `EmbeddingStoreIngestor.builder().embeddingModel(embeddingModel).embeddingStore(embeddingStore).build()` before ingesting the catalog documents.

- [ ] **Step 4: Add default environment-overridable properties and verify GREEN**

Keep `xiaozhi-index`, `langchain4j:vector:xiaozhi:`, `localhost`, `6379`, and `1024` as defaults, with `KNOWLEDGE_REDIS_*` environment overrides. Run the focused tests again.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedLoader.java src/main/java/com/Liang/java/ai/langchain4j/config/RedisEmbeddingStoreConfig.java src/main/java/com/Liang/java/ai/langchain4j/config/KnowledgeRedisProperties.java src/main/resources/application.properties src/test/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedLoaderTest.java src/test/java/com/Liang/java/ai/langchain4j/config/KnowledgeRedisPropertiesTest.java
git commit -m "fix: bind knowledge reload to configured embeddings"
```

### Task 3: Add Isolated External Integration Tests

**Files:**
- Create: `src/test/java/com/Liang/java/ai/langchain4j/release/ExternalValidationEnvironment.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/release/TriageTransactionExternalIntegrationTest.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/release/KnowledgeRedisExternalIntegrationTest.java`

**Interfaces:**
- Consumes: `RELEASE_VALIDATION_JDBC_URL`, `RELEASE_VALIDATION_DB_USER`, `RELEASE_VALIDATION_DB_PASSWORD`, `RELEASE_VALIDATION_REDIS_HOST`, and `RELEASE_VALIDATION_REDIS_PORT`.
- Produces: JUnit `@Tag("external")` tests that reject non-temporary database URLs, force one evidence write failure, and clean up a generated Redis index.

- [ ] **Step 1: Write failing environment guard tests**

Add tests for `ExternalValidationEnvironment.requireValidationJdbcUrl()` that accept only URLs whose selected database starts with `ai_medical_care_release_validation_` and reject `guiguxiaozhi`. Add a Redis name test asserting generated index and prefix begin with the same validation prefix.

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest=ExternalValidationEnvironmentTest test`

Expected: failure because the guard class does not exist.

- [ ] **Step 3: Implement the guard and real external tests**

Implement environment parsing without logging values. Mark real tests `@Tag("external")`. The MyBatis test creates `release_validation_reject_evidence` as a `BEFORE INSERT` trigger, calls `TriageServiceImpl#create` with valid evidence, asserts the exception, and asserts both triage tables contain zero rows. The Redis test builds an 8-dimensional store with a UUID-derived validation index/prefix, reloads the catalog using a deterministic local embedding model, searches for one seed, and finally sends `FT.DROPINDEX <index> DD` only after prefix validation.

- [ ] **Step 4: Run default tests and verify external tests remain excluded**

Run: `mvn test`

Expected: PASS without requiring running MySQL/Redis.

- [ ] **Step 5: Commit**

```powershell
git add src/test/java/com/Liang/java/ai/langchain4j/release
git commit -m "test: add isolated release integration checks"
```

### Task 4: Create the Reproducible Release Runner and Evidence Documentation

**Files:**
- Create: `scripts/release-validation.ps1`
- Create: `scripts/release-validation/schema-baseline.sql`
- Create: `docs/verification/release-validation.md`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: `RELEASE_VALIDATION_DB_PASSWORD` and optional database/Redis host, port, and user environment variables.
- Produces: a generated temporary database, a timestamped Markdown result under `docs/verification/runs/`, and an external Maven invocation.

- [ ] **Step 1: Write failing static runner contract tests**

Extend `MigrationScriptContractTest` to assert the runner validates the temporary database prefix before `DROP DATABASE`, runs V2--V5 twice, invokes `-Pexternal-integration-tests`, and writes under `docs/verification/runs/`.

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest=MigrationScriptContractTest test`

Expected: failure because runner and baseline SQL do not exist.

- [ ] **Step 3: Implement the runner and baseline schema**

Use a timestamp plus random suffix for the database. Set `MYSQL_PWD` only for spawned `mysql` and Maven processes, never write it to a file. Create minimum legacy `user`, `doctor`, `schedule`, and `appointment` tables; run every migration twice through the generated JDBC target; query expected indexes; run tagged external tests with dedicated Redis variables; write a Markdown status record in a `finally` block; and drop only the validated temporary database.

- [ ] **Step 4: Document prerequisites and artifact policy**

Document MySQL 8, Redis Stack/RediSearch, required environment variable names, exact runner invocation, safety guards, expected output, and why generated run records are ignored. Add `docs/verification/runs/` to `.gitignore`.

- [ ] **Step 5: Run focused and package verification**

Run: `mvn -Dtest=MigrationScriptContractTest test`

Run: `mvn test`

Run: `mvn -DskipTests package`

Expected: all commands PASS.

- [ ] **Step 6: Commit**

```powershell
git add scripts/release-validation.ps1 scripts/release-validation/schema-baseline.sql docs/verification/release-validation.md .gitignore src/test/java/com/Liang/java/ai/langchain4j/release/MigrationScriptContractTest.java
git commit -m "docs: add reproducible release validation runner"
```

## Self-Review

- Spec coverage: Task 1 covers portable/idempotent migrations; Task 3 verifies the actual transaction rollback and Redis reload; Task 4 exercises all migrations twice and records reproducible evidence; Task 2 eliminates the implicit embedding-model mismatch discovered during validation preparation.
- Placeholder scan: no TODO/TBD markers or undefined handoff steps remain.
- Type consistency: Task 2 defines the loader constructor and properties consumed by Task 3; Task 3 defines the environment keys consumed by Task 4.
