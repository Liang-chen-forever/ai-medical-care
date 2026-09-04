# 可信分诊最小闭环设计

**状态：** 已批准
**日期：** 2026-09-04
**前置能力：** 角色 JWT、会话隔离、预约状态机和医生工作台已在 `feat/trusted-appointment-collaboration` 实现。
**范围：** 第二阶段，仅建立可追溯、可降级的患者诊前分诊闭环。

## 1. 目标与非目标

当前项目的 AI 聊天只能输出自由文本，知识检索没有可见来源、版本或失败降级，因此不能作为可信分诊能力展示。本阶段将患者主诉变成可持久化的分诊案例，并返回由服务端控制的分诊卡与证据快照。

系统仍然只做科室建议和就医流程引导，不做诊断、处方、支付、真实病历或真实医院 HIS 集成。所有演示资料必须是虚构或已授权的公开非敏感资料。

本阶段明确不做候补队列、就诊记录、评价、知识库上传审核、RRF/重排序、管理员运营台、Docker Compose、CI 和离线标注集。它们依赖稳定的分诊案例契约，作为后续独立阶段实现。

## 2. 核心原则

1. **规则优先。** 高风险症状只由确定性规则引擎判定，命中后绝不调用检索器或聊天模型。
2. **证据优先。** 常规分诊只有取得带文档、切片和知识库版本的有效检索证据，才能返回科室建议。
3. **不确定即降级。** 证据不足、元数据不完整或检索异常时，系统持久化 `FALLBACK` 案例，提示用户自行选择科室和预约，不编造医疗结论。
4. **模型不在决策边界。** 现有流式聊天继续作为健康科普能力；新的分诊接口不让 LLM 决定风险、置信度或科室，避免不可复现的临床判断。
5. **身份由 JWT 派生。** 请求不接收患者、医生、角色或案例归属 ID。患者只能创建和读取自己的分诊案例。

## 3. 端到端流程

```text
患者提交主诉
  -> 输入校验与身份解析
  -> 高风险规则引擎
       -> 命中：保存 EMERGENCY_BLOCKED 案例，返回急诊指引
       -> 未命中：语义检索带元数据的知识切片
            -> 证据有效：保存 EVIDENCE_BACKED 案例和证据快照，返回分诊卡
            -> 无证据/异常：保存 FALLBACK 案例，返回人工选科入口
```

风险规则初始覆盖胸痛并伴呼吸困难、大出血、意识丧失或急性偏瘫/言语障碍，以及明确自伤表述。规则以可测试的代码和稳定 `ruleCode` 保存；它们只负责紧急拦截，不把未命中的用户标记为安全。

常规分诊从现有 LangChain4j `ContentRetriever` 取得候选切片。启动导入的种子文档必须为每个切片保留以下元数据：`documentId`、`chunkId`、`knowledgeVersion`、`department`。适配层过滤得分低于 `0.72`、缺少元数据或不含科室归属的候选；至少一条有效候选才可生成推荐。分诊卡的 `confidence` 是检索相关度，不表示医疗诊断概率。

推荐科室取最高分有效证据的 `department` 元数据；普通案例的时效固定为“建议尽快线下确认”，不输出疾病名称或治疗建议。返回卡片统一附带“非诊断结论，如症状加重请及时就医”的免责声明。

## 4. 数据模型与迁移

新增可重复执行的 `V5__triage_cases.sql`，新表使用 `CREATE TABLE IF NOT EXISTS`，不改写历史预约。

### `triage_case`

| 字段 | 说明 |
| --- | --- |
| `id` | 案例主键 |
| `patient_id` | JWT 中的患者 ID |
| `chief_complaint` | 已校验的主诉，最多 1000 字符 |
| `risk_level` | `EMERGENCY`、`ROUTINE` 或 `UNKNOWN` |
| `status` | `EMERGENCY_BLOCKED`、`EVIDENCE_BACKED` 或 `FALLBACK` |
| `recommended_department` | 有效证据的科室；降级时为空 |
| `retrieval_confidence` | 最高有效证据分数；紧急或降级时为空 |
| `knowledge_version` | 证据对应版本；紧急或降级时为空 |
| `rule_code` | 命中的高风险规则；普通案例为空 |
| `fallback_reason` | `NO_EVIDENCE`、`INVALID_EVIDENCE` 或 `RETRIEVAL_UNAVAILABLE` |
| `created_at` | 服务端创建时间 |

为 `(patient_id, created_at)` 和 `(status, created_at)` 建索引。

### `triage_evidence`

| 字段 | 说明 |
| --- | --- |
| `id` | 证据主键 |
| `triage_case_id` | 所属分诊案例 |
| `document_id` | 来源文档稳定标识 |
| `chunk_id` | 来源切片稳定标识 |
| `excerpt` | 限长摘录，不保存完整对话 |
| `score` | 检索分数 |
| `rank` | 服务端排序 |
| `knowledge_version` | 来源知识库版本 |

为 `(triage_case_id, rank)` 建唯一索引。案例与证据在同一事务内持久化，任何一个写入失败均回滚。

## 5. API 契约与授权

所有接口沿用 `ApiResponse<T>` 和 `/api/v1` 前缀。

| 接口 | 授权 | 行为 |
| --- | --- | --- |
| `POST /api/v1/triage/cases` | `PATIENT` | 创建分诊案例，请求体只有 `chiefComplaint` |
| `GET /api/v1/triage/cases` | `PATIENT` | 返回当前患者最近案例摘要 |
| `GET /api/v1/triage/cases/{id}` | `PATIENT` | 返回当前患者自己的案例和证据快照 |

`chiefComplaint` 必须非空、去除首尾空白，长度为 2 到 1000。非法输入返回现有格式的 `400`；未登录返回 JSON `401`；非患者角色或跨用户读取返回 `403`。

响应只包含案例 ID、风险等级、状态、推荐科室、就医时效、检索置信度、固定免责声明和安全的证据字段。它不返回 JWT、手机号、身份证、聊天记忆、模型提示词、原始文档全文或其他患者资料。

现有公开的 `POST /api/knowledge/reload` 同时收敛为受 `ADMIN` 角色保护的 `/api/v1/admin/knowledge/reload`。本阶段仍只允许加载受版本标注的内置种子资料；上传、发布与回收留到知识库管理阶段。

## 6. 组件边界

- `TriageController`：转换 HTTP 请求和响应，不包含规则或检索逻辑。
- `TriageService`：编排持久化事务、规则、检索、证据筛选与降级。
- `EmergencyRiskRuleEngine`：纯 Java、无外部依赖，返回稳定规则代码或空结果。
- `TriageEvidenceRetriever`：将 LangChain4j 检索结果转为带元数据的内部 `RetrievedEvidence`，隔离第三方 API。
- `TriageRecommendationPolicy`：仅从有效证据元数据导出科室和固定时效，不调用模型。
- `TriageCaseMapper`、`TriageEvidenceMapper`：只管理各自表的数据访问。
- `KnowledgeSeedCatalog`：定义种子知识的稳定 ID、版本和科室元数据；初始化器和受保护 reload 共用它。

现有 `XiaozhiAgent` 与聊天 API 不参与分诊创建。聊天页面的模型文本改为纯文本渲染，移除 `v-html`，防止模型或检索文本被当作 HTML 执行。

## 7. Web 体验

Web 患者端新增“诊前分诊”页面：主诉输入、提交状态、结果卡、证据列表和降级后的科室/预约入口。紧急结果使用明确的就医提示，不显示预约操作；普通结果显示推荐科室和证据摘录；降级结果只显示人工选科入口。

路由守卫要求 `PATIENT`。医生工作台和管理员入口不显示该页面。前端隐藏不是权限边界，所有数据访问仍由后端 JWT 角色和资源归属验证。

## 8. 验收与测试

默认测试不连接 MySQL、MongoDB、Redis、嵌入模型或聊天模型。

1. 规则引擎命中每类高风险输入，并验证检索器和模型均未调用。
2. 有效证据生成 `EVIDENCE_BACKED` 案例、保存稳定版本和按分数排序的快照。
3. 无证据、缺少必需元数据、检索异常均生成 `FALLBACK`，不提供编造的科室或置信度。
4. 控制器覆盖匿名访问、角色越权、跨患者读取和输入长度校验。
5. 知识库 reload 在 `ADMIN` 前被拒绝，管理员角色才能执行。
6. 前端 API 与路由守卫覆盖患者访问和降级跳转；Vite 生产构建通过。
7. 交付前针对真实 MySQL 8 手工执行 V2 到 V5，并验证索引、幂等性和案例/证据事务回滚。

## 9. 后续衔接

下一阶段可将 `triage_case_id` 作为可选字段接入预约，随后实现候补队列和就诊记录。知识库上传、切分、版本发布、审计日志与检索评估必须消费本阶段已保存的案例和证据，而不改变其归属、安全和降级语义。
