# 智能医疗助手系统 — 技术栈文档

## 可信分诊与知识库边界

端到端流程：患者主诉 -> 高风险规则 -> 带版本的检索证据 -> 分诊卡 / 人工选科降级。分诊结果是非诊断结论；命中高风险规则会跳过检索与模型调用。`0.72` 是证据相关性阈值，不代表医疗置信度。MySQL V2-V7 迁移必须完成真实数据库验证，作为发布前置条件。知识重载接口为 ADMIN-only `POST /api/v1/admin/knowledge/reload`，公开 `/api/knowledge/reload` 已下线。

## 项目概述

基于大语言模型（LLM）的智能医疗助手系统，模拟北京协和医院在线客服场景。支持 AI 医疗咨询、智能分诊、预约挂号、科室医生查询等功能。采用前后端分离架构，集成 RAG（检索增强生成）技术，基于医院知识库提供精准医疗建议。

---

## 技术架构

```
┌─────────────────────────────────────────────────┐
│                   Vue 3 前端                      │
│  Vite + Vue Router + Axios + SSE 流式对话         │
└────────────────────┬────────────────────────────┘
                     │ REST API / SSE
┌────────────────────┴────────────────────────────┐
│              Spring Boot 3.2 后端                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ Controller│ │ Service  │ │ AI Agent (Tools) │ │
│  └─────┬─────┘ └────┬─────┘ └────────┬─────────┘ │
│        │            │               │            │
│  ┌─────┴────────────┴───────────────┴──────────┐ │
│  │              LangChain4j 框架                 │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐ │ │
│  │  │ ChatModel│ │ Embedding│ │ Content      │ │ │
│  │  │ (LLM)    │ │ Model    │ │ Retriever    │ │ │
│  │  └──────────┘ └──────────┘ └──────────────┘ │ │
│  └──────────────────────────────────────────────┘ │
│                                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ MySQL    │ │ MongoDB  │ │ Redis            │  │
│  │ 业务数据 │ │ 对话记忆 │ │ 向量存储 (RAG)   │  │
│  └──────────┘ └──────────┘ └──────────────────┘  │
└──────────────────────────────────────────────────┘
```

---

## 后端技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 核心框架 | Spring Boot | 3.2.6 | 应用主框架，自动配置 |
| JDK | Java | 17 | LTS 版本 |
| AI 框架 | LangChain4j | 1.0.0-beta3 | LLM 应用开发框架 |
| 大模型 | DeepSeek-V3 | — | 通过阿里云百炼平台接入 |
| 本地模型 | Ollama + DeepSeek-R1 | 1.5B | 本地推理备选方案 |
| 嵌入模型 | text-embedding-v3 | — | 文档向量化 |
| 向量存储 | Redis (RediSearch) | 7.x | 向量相似度检索 |
| 对话记忆 | MongoDB | 7.x | 聊天历史持久化 |
| 关系数据库 | MySQL | 8.x | 业务数据存储 |
| ORM | MyBatis-Plus | 3.5.11 | 数据持久层，Lambda 查询 |
| API 文档 | Knife4j (Swagger) | 4.3.0 | 在线接口文档与测试 |
| 流式输出 | Spring WebFlux + Reactor | — | SSE 流式响应 |
| 文档解析 | Apache PDFBox | — | PDF 知识库文档解析 |
| 工具库 | Lombok | 1.18.34 | 减少样板代码 |
| 构建工具 | Maven | — | 依赖管理与构建 |

---

## 前端技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Vue 3 | 3.5.x | Composition API |
| 构建工具 | Vite | 6.x | 快速开发构建 |
| 路由 | Vue Router | 4.5.x | SPA 页面路由 |
| HTTP 客户端 | Axios | 1.7.x | AJAX 异步请求 |
| SSE | Fetch API | — | 流式对话接收 |
| CSS | 原生 CSS | — | 自定义医疗风格 UI |

---

## 核心功能模块

### 后端安全与预约一致性（企业化改造）
- 认证接口统一为 `/api/v1/auth`，使用 BCrypt 哈希保存密码、JWT Bearer Token 识别当前用户
- 预约接口统一为 `/api/v1/appointments`：客户端只提交 `scheduleId`，后端从登录用户和排班生成预约快照
- MySQL 使用 `UPDATE schedule SET booked_slots = booked_slots + 1 WHERE booked_slots < total_slots` 原子扣减号源，并由事务保证预约写入失败时自动回滚
- 取消预约校验用户归属，仅允许 `PENDING`/`CONFIRMED -> CANCELLED` 条件状态转换；仅在转换成功后回补一次号源，越权取消和重复预约返回明确业务错误
- 小程序联调时需要在请求头携带 `Authorization: Bearer <token>`；预约请求体为 `{ "scheduleId": 101 }`
- JWT 持有 `PATIENT`、`DOCTOR` 或 `ADMIN` 角色；`doctor.user_id` 将医生档案映射到登录账号，医生队列始终按该映射和预约的 `doctor_id` 快照过滤
- 医生使用 `/api/v1/doctor/appointments` 查询自己的队列，并仅能确认待确认预约、拒绝待确认/已确认预约或完成已确认预约；响应不包含身份证、手机、密码、JWT 或患者档案
- 状态机为 `PENDING -> CONFIRMED -> COMPLETED`，`PENDING|CONFIRMED -> CANCELLED|REJECTED`。取消和拒绝在条件状态更新成功后各回补一次号源，已变更状态返回 409

### 后续业务闭环
- 候补队列在满号时接收申请，按急症/常规/未知风险优先级和 FIFO 排序，生成 15 分钟 offer；接受 offer 与 MySQL 号源扣减在同一事务内完成。
- 医生通过 `/api/v1/doctor/appointments/{id}/encounter` 完成已确认预约并填写不超过 2000 字的就诊摘要和随访建议；患者只能读取自己的摘要。
- 知识文档以 SHA-256 去重并保存不可变版本，管理员可发布和回滚；调用受保护的 reload 接口时，当前已发布版本会与内置种子一起写入 Redis，历史分诊证据携带版本快照，不会因重载被覆盖。上传仅接受 UTF-8 文本/Markdown，单文件上限 2 MiB。未声明科室的管理文档可用于通用检索，但会被可信分诊证据策略过滤。
- 每个请求生成或透传 `X-Trace-Id`，预约、分诊、候补、就诊和知识库写操作记录同库审计事件。审计明细脱敏密码、JWT、API Key、secret，不写入完整主诉。
- `GET /api/v1/admin/metrics/overview` 提供预约、分诊和候补状态计数，供演示和运行观测使用。

### 1. AI 智能问诊
- 基于 LangChain4j 的 AiService 注解式 Agent 定义
- **RAG 检索增强**：文档 Embedding → Redis 向量存储 → 相似度检索 → 注入 LLM
- **Tools 工具调用**：LLM 自动决策调用预约/查询等工具函数
- **Memory 对话记忆**：MongoDB 持久化，支持多轮上下文对话
- **SSE 流式输出**：基于 WebFlux 的 Flux\<String\> 实时推送

### 2. 预约挂号
- 公开查询科室、医生与可用排班；登录后才可预约、查看和取消预约
- 预约请求仅提交 `scheduleId`，后端依据 JWT 中的当前用户和排班快照创建记录
- 使用 MySQL 条件更新原子扣减号源，事务内保障预约创建、取消回补和重复预约校验
- 当前用户通过 `/api/v1/appointments/me` 查询自己的记录，不再通过身份证号查询他人预约

### 3. 科室医生
- 按科室筛选医生列表
- 医生职称、擅长领域、简介展示

### 4. 知识库管理
- 生产知识库加载医院、科室、神经内科和口腔科四份 Markdown 文档
- PDF、TXT 解析示例仅保留在 `src/test/resources/knowledge`，供标记为 `external` 的实验测试使用
- 一键重新加载知识库到向量存储

---

## 项目亮点

1. **完整的 AI Agent 实践**：Tools + RAG + Memory + Streaming 四大核心能力
2. **多模型兼容**：DeepSeek-V3（云端）+ Ollama（本地）双模型支持
3. **多数据库集成**：MySQL（业务）+ MongoDB（记忆）+ Redis（向量）三库协作
4. **业务一致性闭环**：候补 offer、状态机、就诊摘要和号源在事务边界内协作
5. **知识可治理与可追溯**：SHA-256 去重、不可变版本、发布/回滚、证据快照
6. **可观测交付**：Trace ID、脱敏审计、状态统计和可重复发布验证
7. **前后端分离**：Vue 3 SPA + Spring Boot REST API + CORS 跨域
8. **代码分层清晰**：Controller → Service → Mapper 标准三层架构
9. **API 文档完善**：Knife4j 在线接口文档，支持在线调试

---

## 项目结构

```
ai-medical-care/
├── pom.xml                          # Maven 依赖管理和构建入口
├── src/
│   ├── main/java/.../langchain4j/   # Controller/Service/Mapper、鉴权和 AI Agent
│   ├── main/resources/
│   │   ├── application.properties   # 应用配置
│   │   ├── db/                      # 初始化、迁移和演示排班脚本
│   │   ├── knowledge/               # 运行时加载的四份 Markdown
│   │   └── *-prompt-template.txt    # AI 系统提示词
│   └── test/                        # 核心单元/接口测试和 external 实验资源
├── frontend/                        # Vue 3 Web 项目
│   └── src/                         # 页面、路由、状态和 API 封装
├── deploy/                          # Compose、Redis 与 Nginx 部署资源
│   ├── docker-compose.yml
│   ├── Dockerfile.redis
│   ├── nginx/                       # Nginx 配置和 Windows 启停脚本
│   └── redis/Dockerfile             # 可选的 Redis/RediSearch 镜像
├── secrets.example.txt              # 配置模板，真实配置使用被忽略的 secrets.local.txt
└── docs/                            # 技术栈与向量库说明
```

---

## 本地运行

1. 新建 MySQL 数据库 `guiguxiaozhi`，首次初始化只执行 `src/main/resources/db/init-data.sql`。
2. 复制 `secrets.example.txt` 为被 Git 忽略的 `secrets.local.txt`，填写 `DASH_SCOPE_API_KEY`、`MYSQL_PASSWORD`、`JWT_USER_SECRET_KEY` 和 `JWT_ADMIN_SECRET_KEY`（JWT 值至少 32 位）。也可以改用同名环境变量。
3. 启动 MySQL、MongoDB、Redis 后，在项目根目录执行 `mvn spring-boot:run`，服务默认监听 `5137` 端口。需要构建带 RediSearch 的 Redis 镜像时执行 `docker build -f deploy/redis/Dockerfile -t ai-medical-care-redis .`。
4. Web 端在 `frontend` 目录执行 `npm install`、`npm run dev`，通过 Vite 代理访问后端 `5137` 端口。

已有旧库先备份数据并按顺序执行 `V2__secure_appointments.sql`、`V3__roles_and_doctor_accounts.sql`、`V4__appointment_lifecycle.sql`、`V5__triage_cases.sql`、`V6__waitlist_encounter_audit.sql`、`V7__knowledge_documents.sql`。V2 补充预约用户/排班归属，V3 增加角色与医生账号映射，V4 增加状态和医生快照并将历史记录标为 `LEGACY`，V5-V7 分别增加可信分诊、候补/摘要/审计和知识版本表。迁移可重复执行，不创建旧医生的登录账号。真实凭据只通过 `DASH_SCOPE_API_KEY`、`MYSQL_PASSWORD`、JWT secret 等环境变量或被忽略的 `secrets.local.txt` 提供；本项目不会修改本机 MySQL root 原密码。已有演示数据库如果只有历史日期排班，再使用 `mysql --default-character-set=utf8mb4 -uroot -p guiguxiaozhi -e "source src/main/resources/db/refresh-demo-schedules.sql"` 补充今天起未来 7 天的缺失排班。新数据库仍使用 `src/main/resources/db/init-data.sql`。

### Web 与 Nginx 交互

Web 有两种运行模式：

1. 开发模式：`frontend` 执行 `npm run dev`，Vite 使用 `localhost:3000`，并把 `/api`、`/xiaozhi` 转发到后端 `5137`。
2. 集成模式：先执行 `npm run build`，再运行 `deploy/nginx/start-nginx.ps1`。Nginx 默认监听 `8088`，静态根目录为 `frontend/dist`，并把 `/api/`、`/xiaozhi/` 原路径代理到 `127.0.0.1:5137`。访问 `http://localhost:8088/` 即可同时使用网页和后端 API。

Nginx 配置中的 `try_files $uri $uri/ /index.html` 用于支持 Vue Router history 模式刷新；AI 接口配置了 `proxy_buffering off` 和较长读取超时，用于保留流式响应。仓库不提交 `nginx.exe`，脚本支持 `-NginxExe` 指向本机已有的 Nginx。

默认 `mvn test` 排除标记为 `external` 的真实模型、向量库、Mongo CRUD 和旧库演示测试，避免消耗模型额度或依赖本地演示数据。完成模型额度、依赖服务和数据库迁移准备后，使用 `mvn -Pexternal-integration-tests test` 显式运行它们。

---

## 简历描述建议

> **智能医疗助手系统**（全栈项目）
> - 基于 Spring Boot 3.2 + LangChain4j 构建 AI 医疗助手，集成 DeepSeek-V3 大模型
> - 实现 RAG 检索增强生成，使用 Redis 向量存储 + Embedding 实现医疗知识库智能问答
> - 设计 AI Agent Tools 机制，LLM 可自动调用预约挂号、号源查询、取消预约等工具函数
> - 基于 MongoDB 实现对话记忆持久化，支持多轮上下文连续对话
> - 采用 SSE 流式输出技术，实现类似 ChatGPT 的打字机效果
> - 前端使用 Vue 3 + Vite + Axios 构建 SPA 应用，前后端分离架构
> - 集成 MySQL + MyBatis-Plus 管理业务数据，Knife4j 生成在线 API 文档
