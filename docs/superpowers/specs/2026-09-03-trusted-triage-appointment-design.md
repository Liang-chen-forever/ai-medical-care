# 可信诊前分诊与预约协同平台设计

**状态：** 已批准（由项目负责人授权采用推荐方案）  
**日期：** 2026-09-03  
**范围：** 第一阶段的单体应用演进

## 1. 目标与边界

将现有“AI 对话 + 挂号”演进为患者、医生协同参与的诊前服务闭环：

```text
患者主诉/对话 -> 风险拦截与结构化分诊 -> 预约或候补
-> 医生确认 -> 就诊摘要 -> 患者查看与评价
```

系统提供的是诊前分诊和就医流程协同，不进行诊断、处方、支付、医保或接入真实医院 HIS。所有演示患者、医生和知识库资料保持虚构或已授权。

本阶段保留 Spring Boot 单体、MyBatis-Plus、MySQL、Redis、MongoDB、LangChain4j 和 Vue 3 Web。不引入微服务、消息队列、分布式锁或真实临床数据；这些复杂度不服务于实习项目的可验证目标。

## 2. 核心决策

### 2.1 角色与界面

- `PATIENT`：Web 患者端。创建分诊记录、预约、候补、查看就诊摘要和评价。
- `DOCTOR`：仅 Web 端。查看本人排班中的待处理预约，确认、拒绝或完成就诊，并填写不含处方的就诊摘要。
- `ADMIN`：仅 Web 端。维护科室、医生、排班和知识库版本，查看审计记录和基础运营指标。

用户身份和角色都从 JWT 解析；客户端不能提交患者、医生或会话归属 ID。`doctor.user_id` 与登录用户一对一关联，患者与医生都沿用现有 `user` 表。

### 2.2 预约与候补状态机

预约状态为：

```text
PENDING -> CONFIRMED -> COMPLETED
    |          |
    +----------+-> CANCELLED
PENDING -> REJECTED
PENDING/CONFIRMED -> EXPIRED
```

- 创建预约时，数据库条件更新扣减号源，初始状态为 `PENDING`。
- 只有预约患者可以取消 `PENDING`/`CONFIRMED` 预约；取消、拒绝和过期只回补一次号源。
- 医生只能处理归属自己的预约，不能修改就诊人的身份或排班快照。
- 满号时患者可创建 `WAITING` 候补。释放号源后，系统按创建时间和风险优先级选择第一位候补，生成带到期时间的 `OFFERED` 通知；接受后原子创建预约，拒绝或超时转下一位。
- 库存的最终事实来源仍是 MySQL 的条件更新。候补选取和状态转换必须在事务中执行，避免以 Redis 作为一致性前提。

### 2.3 诊前分诊

`triage_case` 表保存一次可审计的诊前分诊，而不是把关键信息只留在聊天记忆中。它包含患者、主诉、结构化症状、风险等级、建议科室、置信度、处理状态、模型版本和知识库版本。

处理顺序固定为：

1. 用确定性规则检测紧急症状和自伤风险。命中后直接返回紧急就医指引，不调用模型。
2. 常规请求执行带知识库版本的检索和模型生成。
3. 返回结构化分诊卡：风险等级、建议科室、就医时效、免责声明和证据引用。
4. 低置信度、无引用或模型异常时，降级为人工科室选择和预约入口，不伪造医疗结论。

`triage_evidence` 保存检索到的文档、段落标识、摘录、分数和排序。第一阶段继续使用 Redis 向量检索，并为每个切片写入文档和版本元数据；后续才加入关键词召回、RRF 和重排序。这样先实现来源可追溯和可评估，而不是无证据地堆叠算法名词。

### 2.4 知识库与审计

知识库由管理员上传和发布。文档版本以文件哈希去重，切片和索引任务关联版本；发布新版本不覆盖历史分诊引用。现有启动自动导入和公开 reload 接口将被替换为管理员受保护的版本化操作。

关键业务操作写入 `audit_log`：操作者、角色、动作、资源类型和 ID、结果、Trace ID、请求时间。日志不记录密码、JWT、模型 Key 和完整病历正文。

### 2.5 安全基线

- 移除仓库中的模型 Key、数据库密码和可预测 JWT 密钥，改用环境变量或被忽略的本地配置；已经暴露的凭据必须轮换。
- 对 `/api/v1/triage/**`、`/api/v1/appointments/**`、`/api/v1/doctor/**`、`/api/v1/admin/**` 实施 JWT 和角色授权。
- 对话会话 ID 由后端创建并绑定当前用户，不能由请求体指定；Mongo 聊天记忆按 `userId + conversationId` 隔离。
- 用户可见的模型文本按纯文本或经过白名单净化的 Markdown 渲染，避免当前任意 `v-html` 带来的 XSS 风险。
- 管理类接口限流并写审计；所有写接口校验当前角色和资源归属。

## 3. 数据模型

在现有表基础上，使用可重复的 SQL 迁移增加以下结构：

| 实体 | 关键字段 | 责任 |
| --- | --- | --- |
| `user` | `role`, `status` | 登录主体及角色 |
| `doctor` | `user_id` | 医生资料到登录主体的映射 |
| `triage_case` | `patient_id`, `chief_complaint`, `risk_level`, `recommended_department`, `confidence`, `knowledge_version`, `status` | 诊前分诊快照 |
| `triage_evidence` | `triage_case_id`, `document_id`, `chunk_id`, `excerpt`, `score`, `rank` | 分诊证据与引用 |
| `appointment` | `status`, `triage_case_id`, `cancel_reason`, `handled_by`, `handled_at` | 预约状态和关联分诊 |
| `waitlist_entry` | `schedule_id`, `patient_id`, `triage_case_id`, `priority`, `status`, `offer_expires_at` | 候补和号源释放处理 |
| `encounter` | `appointment_id`, `doctor_id`, `patient_id`, `summary`, `follow_up_advice`, `completed_at` | 医生填写的就诊摘要 |
| `knowledge_document` | `version`, `file_hash`, `status`, `published_by` | 可发布、可追溯的知识库文档 |
| `audit_log` | `actor_id`, `actor_role`, `action`, `target_type`, `target_id`, `trace_id`, `result` | 安全和业务审计 |

`appointment` 保留患者、医生和排班的历史快照。新外键引用用于完整性校验，不要求旧演示预约一次性回填；历史记录可标记为 `LEGACY`，新流程只操作新契约的数据。

## 4. API 契约

接口统一使用现有 `ApiResponse<T>`。写操作只接收用户可编辑字段，归属、角色、时间、状态和库存变化由服务端生成。

- `POST /api/v1/triage/cases`：创建主诉并返回分诊卡和引用。
- `GET /api/v1/triage/cases/{id}`：患者查看自己的分诊记录；医生可在关联预约中只读查看。
- `POST /api/v1/appointments`：支持可选 `triageCaseId`，创建 `PENDING` 预约。
- `POST /api/v1/waitlist`、`POST /api/v1/waitlist/{id}/accept`：加入和接受候补。
- `GET /api/v1/doctor/appointments`、`POST /api/v1/doctor/appointments/{id}/confirm`、`POST /api/v1/doctor/appointments/{id}/reject`：医生处理预约。
- `POST /api/v1/doctor/appointments/{id}/encounter`：完成预约并创建就诊摘要。
- `GET /api/v1/admin/knowledge/documents`、`POST /api/v1/admin/knowledge/documents`、`POST /api/v1/admin/knowledge/documents/{id}/publish`：管理员知识库管理。

第一阶段不允许 AI Agent 直接创建、取消或确认预约。Agent 只能返回受控的深链接和结构化建议；真实状态改变仍经用户确认的 REST 接口完成。

## 5. 前端体验

患者端新增“诊前分诊”结果页，展示风险、科室、时效、证据、预约和候补入口。Web 覆盖患者主路径并保持统一 API。

Web 新增医生工作台：今日待处理预约、分诊摘要、确认/拒绝、完成就诊。管理员端只提供紧凑的排班、知识库版本、审计和核心指标列表，不在第一阶段做大屏可视化。

所有角色页都由服务器授权结果和路由守卫共同控制，前端隐藏按钮不是权限边界。

## 6. 可测试性与验收

必须新增不依赖真实模型、数据库、Redis 或云 Key 的单元/控制器测试：

- 角色越权、资源归属和聊天会话隔离。
- 预约、取消、拒绝、过期和完成的全部合法/非法状态迁移，以及并发号源不超卖。
- 候补的 FIFO 和风险优先级、接受/超时路径及库存回补。
- 高风险规则必须跳过模型；低置信度和缺失引用必须降级。
- 证据引用归属到正确的知识库版本；重复导入不会重复发布。
- 密钥只可从环境变量/忽略配置读取，接口日志不可包含认证信息。

交付要求包括 Docker Compose、本地演示账号、数据库迁移说明、接口文档、架构图和一组至少 30 条人工标注的离线分诊/检索评估用例。指标将记录召回命中率、带引用回答比例、高风险规则拦截率、接口 P95 延迟和预约状态转换失败率；未实际测得前不在 README 或简历中虚构数值。

## 7. 实施切片

1. **安全和基础设施**：轮换凭据、JWT/角色授权、Trace ID、受保护会话、迁移基线和测试夹具。
2. **预约协同**：状态机、医生账号映射、候补、通知记录和患者/医生接口。
3. **可信分诊**：分诊记录、紧急规则、证据快照、置信度降级和版本化知识库。
4. **Web/小程序闭环**：患者分诊/候补界面、医生工作台、管理员基础管理页。
5. **可交付性**：测试矩阵、离线评估集、Compose、CI 和项目文档。

每个切片可以独立验证；后续切片不改变已发布 API 的身份与库存规则。
