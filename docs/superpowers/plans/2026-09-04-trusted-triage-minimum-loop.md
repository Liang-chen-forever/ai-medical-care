# Trusted Triage Minimum Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver an auditable, evidence-backed and safely degradable patient triage loop that turns a chief complaint into a persisted triage card without using an LLM for clinical decisions.

**Architecture:** Keep the Spring Boot monolith and MyBatis-Plus. A pure emergency-rule engine sits before a `ContentRetriever` adapter; a policy accepts only complete, high-confidence evidence and a transactional service persists the case and snapshots. The Vue patient page consumes the new REST contract, while the existing streaming chat becomes plain-text-only so model output cannot execute as HTML.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, MySQL 8, LangChain4j 1.0.0-beta3, JUnit 5, Mockito, MockMvc, Vue 3, Vite, Node test runner.

**Spec:** `docs/superpowers/specs/2026-09-04-trusted-triage-minimum-loop-design.md`

## Global Constraints

- Preserve the existing Spring Boot monolith, MyBatis-Plus, Redis embedding store, Vue 3, and `ApiResponse<T>` contract; do not introduce a queue, microservice, Spring Security, or new datastore.
- Patients are the only users of `/api/v1/triage/**`; identity and role always come from JWT, never from request JSON or query parameters.
- Emergency rules execute before retrieval and never call the retriever or chat model when they match.
- A recommendation can only come from an evidence item with nonblank `documentId`, `department`, `knowledgeVersion`, a deterministic `chunkId`, and score `>= 0.72`.
- `EVIDENCE_BACKED` cases persist their evidence in the same transaction; `EMERGENCY_BLOCKED` and `FALLBACK` cases persist no invented evidence or recommendation.
- `retrievalConfidence` is retrieval relevance, never a diagnosis probability; every returned card includes the fixed non-diagnostic disclaimer.
- Existing chat remains separate from triage; render chat/model text as plain text and never with `v-html`.
- All default tests run with mocks or test configuration only: no MySQL, Redis, MongoDB, embedding provider, or chat-model calls.
- The existing `POST /api/knowledge/reload` route must be removed; only an `ADMIN` can call `POST /api/v1/admin/knowledge/reload`.

---

## File Structure

| Path | Responsibility |
| --- | --- |
| `src/main/java/.../knowledge/KnowledgeSeed.java` | Immutable source, version and department metadata for one bundled knowledge document. |
| `src/main/java/.../knowledge/KnowledgeSeedCatalog.java` | Loads bundled Markdown and attaches stable retrieval metadata. |
| `src/main/java/.../knowledge/KnowledgeSeedLoader.java` | Rebuilds the project-owned vector index from the versioned seed catalog. |
| `src/main/java/.../dto/knowledge/KnowledgeReloadResponse.java` | Safe reload result with seed count and knowledge version. |
| `src/main/java/.../triage/EmergencyRiskRuleEngine.java` | Pure deterministic emergency rule matching. |
| `src/main/java/.../triage/RiskRuleMatch.java` | Stable high-risk rule-code value. |
| `src/main/java/.../triage/TriageEvidenceRetriever.java` | Internal boundary for fetching evidence from a chief complaint. |
| `src/main/java/.../triage/LangChain4jTriageEvidenceRetriever.java` | Adapter from LangChain4j `ContentRetriever` to internal evidence values. |
| `src/main/java/.../triage/TriageEvidencePolicy.java` | Filters, ranks and converts evidence to a recommendation. |
| `src/main/java/.../triage/RetrievedEvidence.java` | Internal safe evidence value used by the adapter and policy. |
| `src/main/java/.../triage/TriageRecommendation.java` | Derived department, relevance score and version from top evidence. |
| `src/main/java/.../triage/TriageRiskLevel.java` | `EMERGENCY`, `ROUTINE`, `UNKNOWN` persistence enum. |
| `src/main/java/.../triage/TriageCaseStatus.java` | `EMERGENCY_BLOCKED`, `EVIDENCE_BACKED`, `FALLBACK` persistence enum. |
| `src/main/java/.../triage/TriageFallbackReason.java` | Explicit failure reason enum for degraded cases. |
| `src/main/java/.../entity/TriageCase.java` | MyBatis-Plus entity for an auditable triage case. |
| `src/main/java/.../entity/TriageEvidence.java` | MyBatis-Plus entity for one persisted evidence snapshot. |
| `src/main/java/.../mapper/TriageCaseMapper.java` | Triage-case table access. |
| `src/main/java/.../mapper/TriageEvidenceMapper.java` | Triage-evidence table access. |
| `src/main/java/.../dto/triage/*.java` | Validated request and safe API response records. |
| `src/main/java/.../service/TriageService.java` | Patient-scoped triage API boundary. |
| `src/main/java/.../service/impl/TriageServiceImpl.java` | Rule-first orchestration, persistence transaction and resource ownership. |
| `src/main/java/.../controller/TriageController.java` | Patient-only `/api/v1/triage/cases` HTTP adapter. |
| `src/main/resources/db/migration/V5__triage_cases.sql` | Idempotent MySQL schema for cases, snapshots and indexes. |
| `frontend/src/views/TriageView.vue` | Patient chief-complaint submission, result card, evidence list and safe next actions. |
| `frontend/src/api/index.js` | Triage REST wrappers. |
| `frontend/src/router/index.js` | Patient-only `/triage` route guard. |
| `frontend/src/utils/routeAccess.js` | Pure route-authorization decision used by the global router and Node test. |
| `frontend/src/App.vue` | Patient-only navigation entry. |
| `frontend/src/views/ChatView.vue` | Plain-text rendering of model output. |

### Task 1: Version Knowledge Seeds And Protect Reload

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeed.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedCatalog.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedLoader.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/knowledge/KnowledgeReloadResponse.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/config/KnowledgeBaseInitializer.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/controller/KnowledgeController.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedCatalogTest.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/controller/KnowledgeControllerTest.java`

**Interfaces:**
- Produces: `KnowledgeSeed(String documentId, String resourcePath, String department, String knowledgeVersion)`.
- Produces: `KnowledgeSeedCatalog#loadDocuments(): List<Document>` where every document has string metadata `documentId`, `department`, and `knowledgeVersion`.
- Produces: `KnowledgeSeedLoader#reload(): KnowledgeReloadResponse` and `POST /api/v1/admin/knowledge/reload` restricted to `ADMIN`.
- Consumed later: `KnowledgeSeedCatalog.DOCUMENT_ID`, `DEPARTMENT`, and `KNOWLEDGE_VERSION` metadata keys are used by `LangChain4jTriageEvidenceRetriever`.

- [ ] **Step 1: Write the failing seed-metadata and admin-authorization tests**

```java
@Test
void bundledSeedsCarryStableTriageMetadata() {
    List<Document> documents = new KnowledgeSeedCatalog().loadDocuments();

    assertThat(documents).hasSize(4);
    assertThat(documents).allSatisfy(document -> {
        assertThat(document.metadata().getString(KnowledgeSeedCatalog.DOCUMENT_ID)).isNotBlank();
        assertThat(document.metadata().getString(KnowledgeSeedCatalog.KNOWLEDGE_VERSION)).isEqualTo("2026.09");
    });
    assertThat(documents).anySatisfy(document ->
            assertThat(document.metadata().getString(KnowledgeSeedCatalog.DEPARTMENT)).isEqualTo("神经内科"));
}

@Test
void patientCannotReloadKnowledge() throws Exception {
    mockMvc.perform(post("/api/v1/admin/knowledge/reload")
                    .header(HttpHeaders.AUTHORIZATION, patientBearer()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
    verifyNoInteractions(knowledgeSeedLoader);
}

@Test
void adminCanReloadKnowledge() throws Exception {
    when(knowledgeSeedLoader.reload()).thenReturn(new KnowledgeReloadResponse(4, "2026.09"));
    mockMvc.perform(post("/api/v1/admin/knowledge/reload")
                    .header(HttpHeaders.AUTHORIZATION, adminBearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.documentsLoaded").value(4));
    verify(knowledgeSeedLoader).reload();
}
```

- [ ] **Step 2: Run the tests to verify the new behavior is absent**

Run: `mvn -Dtest=KnowledgeSeedCatalogTest,KnowledgeControllerTest test`

Expected: FAIL because the catalog, loader and protected `/api/v1/admin/knowledge/reload` endpoint do not exist.

- [ ] **Step 3: Add seed catalog and loader with stable document metadata**

```java
public record KnowledgeSeed(String documentId, String resourcePath,
                            String department, String knowledgeVersion) {}

public record KnowledgeReloadResponse(int documentsLoaded, String knowledgeVersion) {}

@Component
public class KnowledgeSeedCatalog {
    public static final String DOCUMENT_ID = "documentId";
    public static final String DEPARTMENT = "department";
    public static final String KNOWLEDGE_VERSION = "knowledgeVersion";

    private static final List<KnowledgeSeed> SEEDS = List.of(
            new KnowledgeSeed("hospital-overview", "src/main/resources/knowledge/医院信息.md", "", "2026.09"),
            new KnowledgeSeed("department-overview", "src/main/resources/knowledge/科室信息.md", "", "2026.09"),
            new KnowledgeSeed("neurology-overview", "src/main/resources/knowledge/神经内科.md", "神经内科", "2026.09"),
            new KnowledgeSeed("dentistry-overview", "src/main/resources/knowledge/口腔科.md", "口腔科", "2026.09"));

    public List<Document> loadDocuments() {
        return SEEDS.stream().map(this::load).toList();
    }

    private Document load(KnowledgeSeed seed) {
        Document source = FileSystemDocumentLoader.loadDocument(Path.of(seed.resourcePath()));
        Metadata metadata = source.metadata().copy()
                .put(DOCUMENT_ID, seed.documentId())
                .put(DEPARTMENT, seed.department())
                .put(KNOWLEDGE_VERSION, seed.knowledgeVersion());
        return Document.from(source.text(), metadata);
    }
}
```

Implement `KnowledgeSeedLoader#reload()` with the project-owned `EmbeddingStore<TextSegment>`: clear the configured `langchain4j:vector:xiaozhi:` seed index, ingest `catalog.loadDocuments()`, then return `new KnowledgeReloadResponse(4, "2026.09")`. `KnowledgeBaseInitializer` must delegate to that loader so startup and admin reload use the same source. Do not retain the old controller-local file list.

Change the controller to:

```java
@RestController
@RequestMapping("/api/v1/admin/knowledge")
@RequireRole(UserRole.ADMIN)
public class KnowledgeController {
    @PostMapping("/reload")
    public ApiResponse<KnowledgeReloadResponse> reloadKnowledge() {
        return ApiResponse.success(knowledgeSeedLoader.reload());
    }
}
```

Register both login and role interceptors for `/api/v1/triage/**` in `WebMvcConfig` later in Task 4, but keep the existing `/api/v1/admin/**` coverage for this route. Delete the `/api/knowledge/reload` mapping rather than preserving it as a public compatibility route.

- [ ] **Step 4: Run focused verification**

Run: `mvn -Dtest=KnowledgeSeedCatalogTest,KnowledgeControllerTest test`

Expected: PASS; the catalog returns versioned metadata, patient receives `403`, and admin reaches only the loader boundary.

- [ ] **Step 5: Commit the knowledge boundary**

```bash
git add src/main/java/com/Liang/java/ai/langchain4j/knowledge \
  src/main/java/com/Liang/java/ai/langchain4j/dto/knowledge/KnowledgeReloadResponse.java \
  src/main/java/com/Liang/java/ai/langchain4j/config/KnowledgeBaseInitializer.java \
  src/main/java/com/Liang/java/ai/langchain4j/controller/KnowledgeController.java \
  src/test/java/com/Liang/java/ai/langchain4j/knowledge/KnowledgeSeedCatalogTest.java \
  src/test/java/com/Liang/java/ai/langchain4j/controller/KnowledgeControllerTest.java
git commit -m "feat: version knowledge seeds and protect reload"
```

### Task 2: Add Deterministic Risk And Evidence Policy

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/EmergencyRiskRuleEngine.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/RiskRuleMatch.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/RetrievedEvidence.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/TriageEvidencePolicy.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/TriageRecommendation.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/TriageRiskLevel.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/TriageCaseStatus.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/TriageFallbackReason.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/triage/EmergencyRiskRuleEngineTest.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/triage/TriageEvidencePolicyTest.java`

**Interfaces:**
- Produces: `Optional<RiskRuleMatch> EmergencyRiskRuleEngine#match(String chiefComplaint)`.
- Produces: `RetrievedEvidence(String documentId, String chunkId, String department, String knowledgeVersion, String excerpt, double score)`.
- Produces: `List<RetrievedEvidence> TriageEvidencePolicy#validAndRanked(List<RetrievedEvidence>)` and `TriageRecommendation TriageEvidencePolicy#recommend(List<RetrievedEvidence>)`.
- Consumed later: a matching rule becomes an `EMERGENCY_BLOCKED` case; a policy recommendation becomes an `EVIDENCE_BACKED` case.

- [ ] **Step 1: Write the failing pure-domain tests**

```java
@ParameterizedTest
@CsvSource({
        "胸痛并且喘不过气,CHEST_PAIN_WITH_BREATHLESSNESS",
        "突然大出血,BLEEDING",
        "刚刚昏过去了,LOSS_OF_CONSCIOUSNESS",
        "一侧手脚无力说话含糊,ACUTE_NEUROLOGICAL_DEFICIT",
        "我不想活了,SELF_HARM_RISK"
})
void emergencySymptomsReturnStableRuleCodes(String complaint, String code) {
    assertThat(engine.match(complaint)).contains(new RiskRuleMatch(code));
}

@Test
void nonEmergencyComplaintHasNoEmergencyMatch() {
    assertThat(engine.match("最近反复头痛，想了解应该挂什么科")).isEmpty();
}

@Test
void policyKeepsOnlyCompleteHighConfidenceEvidenceAndUsesTopDepartment() {
    List<RetrievedEvidence> valid = policy.validAndRanked(List.of(
            evidence("neuro", "n-1", "神经内科", "2026.09", 0.91),
            evidence("dental", "d-1", "口腔科", "2026.09", 0.73),
            evidence("bad", "", "口腔科", "2026.09", 0.99),
            evidence("low", "l-1", "神经内科", "2026.09", 0.71)));

    assertThat(valid).extracting(RetrievedEvidence::chunkId).containsExactly("n-1", "d-1");
    assertThat(policy.recommend(valid)).isEqualTo(new TriageRecommendation("神经内科", 0.91, "2026.09"));
}

private RetrievedEvidence evidence(String documentId, String chunkId, String department,
                                   String version, double score) {
    return new RetrievedEvidence(documentId, chunkId, department, version, "受限摘录", score);
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -Dtest=EmergencyRiskRuleEngineTest,TriageEvidencePolicyTest test`

Expected: FAIL because the triage package does not exist.

- [ ] **Step 3: Implement a pure rule engine and policy**

```java
public record RiskRuleMatch(String ruleCode) {}

public record RetrievedEvidence(String documentId, String chunkId, String department,
                                String knowledgeVersion, String excerpt, double score) {}

public record TriageRecommendation(String department, double confidence,
                                   String knowledgeVersion) {}

@Component
public class EmergencyRiskRuleEngine {
    public Optional<RiskRuleMatch> match(String complaint) {
        String text = complaint.toLowerCase(Locale.ROOT);
        if ((text.contains("胸痛") || text.contains("胸口痛"))
                && (text.contains("呼吸困难") || text.contains("喘不过气") || text.contains("气短"))) {
            return Optional.of(new RiskRuleMatch("CHEST_PAIN_WITH_BREATHLESSNESS"));
        }
        if (text.contains("大出血") || text.contains("大量出血") || text.contains("止不住血")) {
            return Optional.of(new RiskRuleMatch("BLEEDING"));
        }
        if (text.contains("昏过去") || text.contains("意识丧失") || text.contains("失去意识")) {
            return Optional.of(new RiskRuleMatch("LOSS_OF_CONSCIOUSNESS"));
        }
        if (text.contains("偏瘫") || text.contains("一侧无力") || text.contains("言语不清") || text.contains("说话含糊")) {
            return Optional.of(new RiskRuleMatch("ACUTE_NEUROLOGICAL_DEFICIT"));
        }
        if (text.contains("自杀") || text.contains("自残") || text.contains("伤害自己") || text.contains("不想活")) {
            return Optional.of(new RiskRuleMatch("SELF_HARM_RISK"));
        }
        return Optional.empty();
    }
}
```

`TriageEvidencePolicy` must reject null candidates, blank required metadata, blank excerpts and scores below `0.72`; sort remaining items descending by score with `chunkId` as a deterministic tie breaker. `recommend` must use the first valid item only. Add these enums exactly:

```java
public enum TriageRiskLevel { EMERGENCY, ROUTINE, UNKNOWN }
public enum TriageCaseStatus { EMERGENCY_BLOCKED, EVIDENCE_BACKED, FALLBACK }
public enum TriageFallbackReason { NO_EVIDENCE, INVALID_EVIDENCE, RETRIEVAL_UNAVAILABLE }
```

- [ ] **Step 4: Run focused verification**

Run: `mvn -Dtest=EmergencyRiskRuleEngineTest,TriageEvidencePolicyTest test`

Expected: PASS; all emergency categories return stable codes and only valid evidence can choose a department.

- [ ] **Step 5: Commit the deterministic decision layer**

```bash
git add src/main/java/com/Liang/java/ai/langchain4j/triage \
  src/test/java/com/Liang/java/ai/langchain4j/triage
git commit -m "feat: add rule based triage policy"
```

### Task 3: Persist Triage Cases And Integrate Evidence Retrieval

**Files:**
- Create: `src/main/resources/db/migration/V5__triage_cases.sql`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/entity/TriageCase.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/entity/TriageEvidence.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/mapper/TriageCaseMapper.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/mapper/TriageEvidenceMapper.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/TriageEvidenceRetriever.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/triage/LangChain4jTriageEvidenceRetriever.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/triage/TriageCaseResponse.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/triage/TriageEvidenceResponse.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/triage/TriageCaseSummaryResponse.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/TriageService.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/service/impl/TriageServiceImpl.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/service/TriageServiceImplTest.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/triage/LangChain4jTriageEvidenceRetrieverTest.java`

**Interfaces:**
- Consumes: Task 1 metadata keys and Task 2 risk/policy APIs.
- Produces: `TriageService#create(Long patientId, String chiefComplaint)`, `listMine(Long patientId)`, and `getMine(Long patientId, Long caseId)`.
- Produces: `TriageEvidenceRetriever#retrieve(String chiefComplaint): List<RetrievedEvidence>` with no LLM dependency.
- Produces: transactional `TriageCaseResponse` containing only safe triage fields and evidence snapshots.

- [ ] **Step 1: Write failing evidence-adapter and service behavior tests**

```java
@Test
void adapterCopiesScoreAndStableMetadataFromLangChainContent() {
    TextSegment segment = TextSegment.from("头痛建议前往神经内科", Metadata.from(Map.of(
            KnowledgeSeedCatalog.DOCUMENT_ID, "neurology-overview",
            KnowledgeSeedCatalog.DEPARTMENT, "神经内科",
            KnowledgeSeedCatalog.KNOWLEDGE_VERSION, "2026.09")));
    when(contentRetriever.retrieve(Query.from("反复头痛"))).thenReturn(List.of(
            Content.from(segment, Map.of(ContentMetadata.SCORE, 0.88))));

    RetrievedEvidence evidence = adapter.retrieve("反复头痛").get(0);

    assertThat(evidence.documentId()).isEqualTo("neurology-overview");
    assertThat(evidence.department()).isEqualTo("神经内科");
    assertThat(evidence.knowledgeVersion()).isEqualTo("2026.09");
    assertThat(evidence.chunkId()).matches("[a-f0-9]{64}");
    assertThat(evidence.score()).isEqualTo(0.88);
}

@Test
void emergencyCaseDoesNotRetrieveEvidenceAndStoresRuleCode() {
    when(caseMapper.insert(any(TriageCase.class))).thenAnswer(invocation -> {
        invocation.getArgument(0, TriageCase.class).setId(31L);
        return 1;
    });

    TriageCaseResponse response = service.create(7L, "胸痛而且喘不过气");

    assertThat(response.status()).isEqualTo(TriageCaseStatus.EMERGENCY_BLOCKED);
    assertThat(response.ruleCode()).isEqualTo("CHEST_PAIN_WITH_BREATHLESSNESS");
    verifyNoInteractions(evidenceRetriever, evidenceMapper);
}

@Test
void validEvidencePersistsCaseAndRankedSnapshots() {
    when(evidenceRetriever.retrieve("反复头痛")).thenReturn(List.of(
            evidence("neuro", "b", "神经内科", "2026.09", 0.82),
            evidence("neuro", "a", "神经内科", "2026.09", 0.91)));
    assignId(caseMapper, 32L);

    TriageCaseResponse response = service.create(7L, "反复头痛");

    assertThat(response.status()).isEqualTo(TriageCaseStatus.EVIDENCE_BACKED);
    assertThat(response.recommendedDepartment()).isEqualTo("神经内科");
    assertThat(response.evidence()).extracting(TriageEvidenceResponse::rank).containsExactly(1, 2);
    verify(evidenceMapper, times(2)).insert(any(TriageEvidence.class));
}

@Test
void unavailableRetrieverPersistsFallbackWithoutDepartment() {
    when(evidenceRetriever.retrieve("牙痛")).thenThrow(new IllegalStateException("vector unavailable"));
    assignId(caseMapper, 33L);

    TriageCaseResponse response = service.create(7L, "牙痛");

    assertThat(response.status()).isEqualTo(TriageCaseStatus.FALLBACK);
    assertThat(response.fallbackReason()).isEqualTo(TriageFallbackReason.RETRIEVAL_UNAVAILABLE);
    assertThat(response.recommendedDepartment()).isNull();
    verifyNoInteractions(evidenceMapper);
}

private void assignId(TriageCaseMapper mapper, long id) {
    when(mapper.insert(any(TriageCase.class))).thenAnswer(invocation -> {
        invocation.getArgument(0, TriageCase.class).setId(id);
        return 1;
    });
}

private RetrievedEvidence evidence(String documentId, String chunkId, String department,
                                   String version, double score) {
    return new RetrievedEvidence(documentId, chunkId, department, version, "受限摘录", score);
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -Dtest=LangChain4jTriageEvidenceRetrieverTest,TriageServiceImplTest test`

Expected: FAIL because adapters, persistent entities and triage service do not exist.

- [ ] **Step 3: Add the idempotent migration and persistence types**

```sql
CREATE TABLE IF NOT EXISTS triage_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    chief_complaint VARCHAR(1000) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    recommended_department VARCHAR(50) NULL,
    retrieval_confidence DECIMAL(5,4) NULL,
    knowledge_version VARCHAR(32) NULL,
    rule_code VARCHAR(64) NULL,
    fallback_reason VARCHAR(32) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_triage_case_patient_created (patient_id, created_at),
    KEY idx_triage_case_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS triage_evidence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    triage_case_id BIGINT NOT NULL,
    document_id VARCHAR(128) NOT NULL,
    chunk_id CHAR(64) NOT NULL,
    excerpt VARCHAR(500) NOT NULL,
    score DECIMAL(5,4) NOT NULL,
    rank_no INT NOT NULL,
    knowledge_version VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_triage_evidence_case_rank (triage_case_id, rank_no),
    KEY idx_triage_evidence_case (triage_case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Use explicit `@TableName("triage_case")` and `@TableName("triage_evidence")`; map `rankNo` to `rank_no` with `@TableField("rank_no")`. Both mapper interfaces extend `BaseMapper`.

- [ ] **Step 4: Implement LangChain4j adapter and transactional service**

```java
public interface TriageEvidenceRetriever {
    List<RetrievedEvidence> retrieve(String chiefComplaint);
}

@Component
public class LangChain4jTriageEvidenceRetriever implements TriageEvidenceRetriever {
    public LangChain4jTriageEvidenceRetriever(
            @Qualifier("contentRetrieverXiaozhiPincone") ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Override
    public List<RetrievedEvidence> retrieve(String chiefComplaint) {
        return contentRetriever.retrieve(Query.from(chiefComplaint)).stream()
                .map(this::toEvidence)
                .toList();
    }

    private RetrievedEvidence toEvidence(Content content) {
        TextSegment segment = content.textSegment();
        Metadata metadata = segment.metadata();
        Object rawScore = content.metadata().get(ContentMetadata.SCORE);
        double score = rawScore instanceof Number number ? number.doubleValue() : Double.NaN;
        String documentId = metadata.getString(KnowledgeSeedCatalog.DOCUMENT_ID);
        return new RetrievedEvidence(documentId, sha256(documentId + "\\n" + segment.text()),
                metadata.getString(KnowledgeSeedCatalog.DEPARTMENT),
                metadata.getString(KnowledgeSeedCatalog.KNOWLEDGE_VERSION),
                truncate(segment.text(), 500), score);
    }
}
```

Define the response records before implementing the service so every consumer shares the same safe shape:

```java
public record TriageEvidenceResponse(String documentId, String chunkId, String excerpt,
                                     double score, int rank, String knowledgeVersion) {}

public record TriageCaseSummaryResponse(Long id, TriageRiskLevel riskLevel,
                                         TriageCaseStatus status, String recommendedDepartment,
                                         LocalDateTime createdAt) {}

public record TriageCaseResponse(Long id, TriageRiskLevel riskLevel, TriageCaseStatus status,
        String recommendedDepartment, String careTiming, Double retrievalConfidence,
        String knowledgeVersion, String ruleCode, TriageFallbackReason fallbackReason,
        String emergencyInstruction, String disclaimer, List<TriageEvidenceResponse> evidence) {}
```

`TriageServiceImpl#create` is annotated `@Transactional(rollbackFor = Exception.class)` and follows this exact control flow:

1. If `riskRuleEngine.match(complaint)` is present, insert `EMERGENCY` / `EMERGENCY_BLOCKED` with the stable rule code, no recommendation, no confidence and no evidence, then return the fixed emergency instruction.
2. Otherwise call only `evidenceRetriever.retrieve(complaint)`. A thrown runtime exception becomes an inserted `UNKNOWN` / `FALLBACK` with `RETRIEVAL_UNAVAILABLE`.
3. A nonempty raw result that `policy.validAndRanked` empties becomes `UNKNOWN` / `FALLBACK` with `INVALID_EVIDENCE`; an empty raw result becomes `UNKNOWN` / `FALLBACK` with `NO_EVIDENCE`.
4. For valid evidence, derive the policy recommendation, insert `ROUTINE` / `EVIDENCE_BACKED`, then insert evidence rows rank 1..N in the same transaction.

`getMine` must return `404` for a nonexistent ID and `403` when `patientId` differs. `listMine` selects only `patient_id = patientId`, newest first, and returns at most 20 summaries. Every response sets the literal disclaimer `非诊断结论，如症状加重请及时就医`; ordinary recommendations use `建议尽快线下确认`; emergency results use `请立即前往急诊或拨打 120` and have no evidence list.

- [ ] **Step 5: Run focused verification**

Run: `mvn -Dtest=LangChain4jTriageEvidenceRetrieverTest,TriageServiceImplTest test`

Expected: PASS; the adapter derives a deterministic fingerprint, emergency cases bypass retrieval, valid evidence persists snapshots, and retrieval failures cannot fabricate a department.

- [ ] **Step 6: Commit persistence and workflow**

```bash
git add src/main/resources/db/migration/V5__triage_cases.sql \
  src/main/java/com/Liang/java/ai/langchain4j/entity/TriageCase.java \
  src/main/java/com/Liang/java/ai/langchain4j/entity/TriageEvidence.java \
  src/main/java/com/Liang/java/ai/langchain4j/mapper/TriageCaseMapper.java \
  src/main/java/com/Liang/java/ai/langchain4j/mapper/TriageEvidenceMapper.java \
  src/main/java/com/Liang/java/ai/langchain4j/triage/LangChain4jTriageEvidenceRetriever.java \
  src/main/java/com/Liang/java/ai/langchain4j/triage/TriageEvidenceRetriever.java \
  src/main/java/com/Liang/java/ai/langchain4j/dto/triage \
  src/main/java/com/Liang/java/ai/langchain4j/service/TriageService.java \
  src/main/java/com/Liang/java/ai/langchain4j/service/impl/TriageServiceImpl.java \
  src/test/java/com/Liang/java/ai/langchain4j/service/TriageServiceImplTest.java \
  src/test/java/com/Liang/java/ai/langchain4j/triage/LangChain4jTriageEvidenceRetrieverTest.java
git commit -m "feat: add evidence backed triage cases"
```

### Task 4: Expose Patient-Scoped Triage APIs

**Files:**
- Create: `src/main/java/com/Liang/java/ai/langchain4j/dto/triage/CreateTriageCaseRequest.java`
- Create: `src/main/java/com/Liang/java/ai/langchain4j/controller/TriageController.java`
- Modify: `src/main/java/com/Liang/java/ai/langchain4j/config/WebMvcConfig.java`
- Create: `src/test/java/com/Liang/java/ai/langchain4j/controller/TriageControllerTest.java`

**Interfaces:**
- Consumes: Task 3 `TriageService#create`, `listMine`, and `getMine` plus `@LoginUser UserPrincipal`.
- Produces: `POST /api/v1/triage/cases`, `GET /api/v1/triage/cases`, and `GET /api/v1/triage/cases/{id}`.
- Produces: every triage route requires JWT and `@RequireRole(UserRole.PATIENT)` before controller execution.

- [ ] **Step 1: Write failing controller contract tests**

```java
@Test
void patientCreatesCaseFromChiefComplaintOnly() throws Exception {
    when(triageService.create(7L, "反复头痛")).thenReturn(evidenceBackedCase(31L));

    mockMvc.perform(post("/api/v1/triage/cases")
                    .header(HttpHeaders.AUTHORIZATION, patientBearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"chiefComplaint\":\"反复头痛\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(31))
            .andExpect(jsonPath("$.data.recommendedDepartment").value("神经内科"))
            .andExpect(jsonPath("$.data.idCard").doesNotExist());

    verify(triageService).create(7L, "反复头痛");
}

@Test
void doctorIsForbiddenBeforeTriageControllerRuns() throws Exception {
    mockMvc.perform(get("/api/v1/triage/cases").header(HttpHeaders.AUTHORIZATION, doctorBearer()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("无权访问该资源"));
    verifyNoInteractions(triageService);
}

@Test
void complaintMustHaveAtLeastTwoCharacters() throws Exception {
    mockMvc.perform(post("/api/v1/triage/cases")
                    .header(HttpHeaders.AUTHORIZATION, patientBearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"chiefComplaint\":\"痛\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("主诉长度需为2到1000个字符"));
}
```

- [ ] **Step 2: Run the controller tests to verify they fail**

Run: `mvn -Dtest=TriageControllerTest test`

Expected: FAIL because no patient triage route or controller is registered.

- [ ] **Step 3: Implement the secure HTTP adapter**

```java
public record CreateTriageCaseRequest(
        @NotBlank(message = "主诉不能为空")
        @Size(min = 2, max = 1000, message = "主诉长度需为2到1000个字符")
        String chiefComplaint) {}

@RestController
@RequestMapping("/api/v1/triage/cases")
@RequireRole(UserRole.PATIENT)
public class TriageController {
    @PostMapping
    public ApiResponse<TriageCaseResponse> create(@LoginUser UserPrincipal principal,
                                                    @Valid @RequestBody CreateTriageCaseRequest request) {
        return ApiResponse.success(triageService.create(principal.userId(), request.chiefComplaint().trim()));
    }

    @GetMapping
    public ApiResponse<List<TriageCaseSummaryResponse>> list(@LoginUser UserPrincipal principal) {
        return ApiResponse.success(triageService.listMine(principal.userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<TriageCaseResponse> get(@LoginUser UserPrincipal principal,
                                                 @PathVariable @Positive(message = "分诊ID必须大于0") Long id) {
        return ApiResponse.success(triageService.getMine(principal.userId(), id));
    }
}
```

Add `/api/v1/triage/**` to both the login and role interceptor path patterns. Do not accept a `patientId`, `role`, `riskLevel`, `status`, `department`, confidence or evidence in the request body.

- [ ] **Step 4: Run focused verification**

Run: `mvn -Dtest=TriageControllerTest,RoleRequiredInterceptorTest test`

Expected: PASS; anonymous requests receive JSON `401`, a doctor receives `403` before service invocation, patient ownership derives from the JWT, and invalid input returns the stated `400` message.

- [ ] **Step 5: Commit the patient API**

```bash
git add src/main/java/com/Liang/java/ai/langchain4j/dto/triage/CreateTriageCaseRequest.java \
  src/main/java/com/Liang/java/ai/langchain4j/controller/TriageController.java \
  src/main/java/com/Liang/java/ai/langchain4j/config/WebMvcConfig.java \
  src/test/java/com/Liang/java/ai/langchain4j/controller/TriageControllerTest.java
git commit -m "feat: expose patient triage cases"
```

### Task 5: Add Patient Triage Experience And Safe Chat Rendering

**Files:**
- Create: `frontend/src/views/TriageView.vue`
- Modify: `frontend/src/api/index.js`
- Modify: `frontend/src/router/index.js`
- Create: `frontend/src/utils/routeAccess.js`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/views/ChatView.vue`
- Create: `frontend/test/triage-api.test.js`
- Modify: `README.md`
- Modify: `docs/技术栈文档.md`

**Interfaces:**
- Consumes: Task 4 response fields `status`, `riskLevel`, `recommendedDepartment`, `careTiming`, `retrievalConfidence`, `disclaimer`, `emergencyInstruction`, `fallbackReason`, and `evidence`.
- Produces: patient-only `/triage` route; `createTriageCase(chiefComplaint)`, `getTriageCases()`, `getTriageCase(id)` wrappers; no executable HTML rendering from chat or triage content.

- [ ] **Step 1: Write failing frontend API tests**

```js
test('triage creation sends only the chief complaint', async () => {
  const calls = []
  const originalPost = apiModule.api.post
  apiModule.api.post = (...args) => {
    calls.push(args)
    return Promise.resolve({ data: null })
  }

  try {
    await apiModule.createTriageCase('反复头痛')
    assert.deepEqual(calls, [['/api/v1/triage/cases', { chiefComplaint: '反复头痛' }]])
  } finally {
    apiModule.api.post = originalPost
  }
})

test('triage detail reads only the case resource path', async () => {
  const calls = []
  const originalGet = apiModule.api.get
  apiModule.api.get = (...args) => {
    calls.push(args)
    return Promise.resolve({ data: null })
  }

  try {
    await apiModule.getTriageCase(31)
    assert.deepEqual(calls, [['/api/v1/triage/cases/31']])
  } finally {
    apiModule.api.get = originalGet
  }
})

test('patient-only route redirects a doctor away from triage', () => {
  assert.deepEqual(routeAccess({ requiresAuth: true, role: 'PATIENT' }, true, 'DOCTOR'), { name: 'Chat' })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm test -- test/triage-api.test.js`

Expected: FAIL because triage wrappers do not exist.

- [ ] **Step 3: Add API wrappers, route and patient result page**

```js
export function createTriageCase(chiefComplaint) {
  return api.post('/api/v1/triage/cases', { chiefComplaint })
}

export function getTriageCases() {
  return api.get('/api/v1/triage/cases')
}

export function getTriageCase(id) {
  return api.get(`/api/v1/triage/cases/${id}`)
}
```

Register `/triage` as `{ requiresAuth: true, role: 'PATIENT' }`. Show the navigation item only when `user?.role === 'PATIENT'`, on both desktop navigation and mobile tab bar.

Move the authorization decision into a pure helper so Node can test the same rule the router uses:

```js
export function routeAccess(meta, authenticated, role) {
  if (meta.requiresAuth && !authenticated) return { name: 'Login' }
  if (meta.role && meta.role !== role) return authenticated ? { name: 'Chat' } : { name: 'Login' }
  return undefined
}
```

`router.beforeEach` must call `routeAccess(to.meta, isAuthenticated(), getUser()?.role)` and, for unauthenticated protected requests, attach its existing `{ redirect: to.fullPath }` query before returning the login location. Import `routeAccess` in `frontend/test/triage-api.test.js` and use it in the added route assertion.

`TriageView.vue` must use a `<textarea maxlength="1000">` bound to a patient chief complaint, disable submit while it is blank or pending, and show the server card after success. Render all returned content with Vue interpolation (`{{ }}`), never `v-html`.

Implement three mutually exclusive states:

```vue
<section v-if="result?.status === 'EMERGENCY_BLOCKED'" class="triage-alert triage-alert-danger">
  <h2>请立即寻求紧急医疗帮助</h2>
  <p>{{ result.emergencyInstruction }}</p>
  <p>{{ result.disclaimer }}</p>
</section>

<section v-else-if="result?.status === 'EVIDENCE_BACKED'" class="triage-card">
  <p>建议科室：{{ result.recommendedDepartment }}</p>
  <p>{{ result.careTiming }}</p>
  <ul><li v-for="evidence in result.evidence" :key="evidence.chunkId">{{ evidence.excerpt }}</li></ul>
  <button class="btn btn-primary" @click="goToAppointment(result.recommendedDepartment)">查看该科室号源</button>
</section>

<section v-else-if="result?.status === 'FALLBACK'" class="triage-alert triage-alert-warning">
  <p>暂时无法提供可靠的分诊建议，请选择科室后线下确认。</p>
  <button class="btn btn-outline" @click="router.push('/department')">查看科室</button>
</section>
```

`goToAppointment` must push `{ name: 'Appointment', query: { dept: department } }`. It must not offer appointment navigation in the emergency card.

Replace the two existing `v-html="renderMarkdown(...)"` chat bubbles with `{{ msg.content }}` and `{{ streamingText }}`; delete `renderMarkdown`; add `white-space: pre-wrap` to `.bubble-text`. This deliberately treats every model, retrieval and user string as text.

- [ ] **Step 4: Run frontend verification**

Run: `npm test`

Expected: PASS; existing appointment/doctor API tests and new triage API wrappers pass.

Run: `npm run build`

Expected: Vite production build succeeds with the patient route and no unresolved imports.

- [ ] **Step 5: Update delivery documentation**

Add to `README.md` and `docs/技术栈文档.md`:

```text
患者主诉 -> 高风险规则 -> 带版本的检索证据 -> 分诊卡 / 人工选科降级
```

State explicitly that the system provides non-diagnostic department guidance, high-risk rules skip retrieval and model calls, `0.72` is an evidence relevance threshold rather than a medical confidence, and live MySQL verification of V2 through V5 remains a release prerequisite. Document `POST /api/v1/admin/knowledge/reload` as `ADMIN`-only and remove references to public `/api/knowledge/reload`.

- [ ] **Step 6: Commit the patient experience**

```bash
git add frontend/src/views/TriageView.vue frontend/src/api/index.js \
  frontend/src/router/index.js frontend/src/utils/routeAccess.js frontend/src/App.vue frontend/src/views/ChatView.vue \
  frontend/test/triage-api.test.js README.md "docs/技术栈文档.md"
git commit -m "feat: add patient triage experience"
```

### Task 6: Verify The Delivery Slice

**Files:**
- Modify: `README.md`
- Modify: `docs/技术栈文档.md`

**Interfaces:**
- Consumes: Tasks 1-5.
- Produces: reproducible verification evidence and an accurate capability statement for the second delivery slice.

- [ ] **Step 1: Run all non-external backend tests**

Run: `mvn test`

Expected: `BUILD SUCCESS`; no test attempts MySQL, Redis, MongoDB, embedding or chat-model access.

- [ ] **Step 2: Run focused behavior tests**

Run: `mvn -Dtest=KnowledgeSeedCatalogTest,KnowledgeControllerTest,EmergencyRiskRuleEngineTest,TriageEvidencePolicyTest,LangChain4jTriageEvidenceRetrieverTest,TriageServiceImplTest,TriageControllerTest test`

Expected: `BUILD SUCCESS`; coverage proves protected seed reload, rule-first emergency interception, evidence filtering, deterministic source fingerprints, fallback paths, persistence decisions and patient API authorization.

- [ ] **Step 3: Build and scan the deliverable**

Run: `mvn -DskipTests package`

Expected: `BUILD SUCCESS`.

Run: `npm test`

Expected: all Node tests pass from `frontend`.

Run: `npm run build`

Expected: Vite production build passes from `frontend`.

Run: `rg -n "v-html|/api/knowledge/reload|sk-[A-Za-z0-9]|password=[^$]|secret-key=[^$]" frontend/src src/main/resources secrets.example.txt`

Expected: no `v-html`, public reload route, or usable credential match.

Run: `git diff --check`

Expected: no whitespace errors.

- [ ] **Step 4: Record external release checks**

Update both docs to list these as not yet completed in the local default suite: execute V2 through V5 on a populated MySQL 8 database; verify migration idempotence and indexes; verify an injected evidence-write failure rolls back the parent case and child evidence; and exercise admin reload against a dedicated Redis vector index.

- [ ] **Step 5: Commit verification documentation**

```bash
git add README.md "docs/技术栈文档.md"
git commit -m "docs: verify trusted triage delivery"
```

## Plan Self-Review

- **Spec coverage:** Task 1 covers versioned bundled evidence metadata and ADMIN-only reload. Task 2 covers deterministic emergency interception and evidence eligibility. Task 3 covers V5, evidence retrieval adaptation, persistence, transactional snapshots and fallback semantics. Task 4 covers JWT-derived patient authorization and HTTP validation. Task 5 covers the patient triage page, route guard, evidence and fallback actions, and the XSS boundary. Task 6 covers delivery verification and the explicitly external MySQL/Redis checks.
- **Placeholder scan:** No implementation step contains an unresolved placeholder or unnamed validation/error handling. Each generated type, method, endpoint, enum value and test target is named before later tasks consume it.
- **Type consistency:** `RetrievedEvidence` flows from `TriageEvidenceRetriever` to `TriageEvidencePolicy` and `TriageServiceImpl`; policy output is `TriageRecommendation`; the public service returns `TriageCaseResponse`; controller and Vue consume exactly those public fields. The seed metadata names used by the adapter originate in Task 1.
