# 智能医疗助手

面向在线医疗服务场景的 Java 全栈项目：提供 AI 问诊、可信分诊、候补挂号、医生就诊摘要、知识库版本治理和可审计预约流程。客户端使用 Vue 3 Web，后端使用 Spring Boot、MyBatis-Plus、MySQL、MongoDB、Redis 和 LangChain4j。

智能分诊流程：患者主诉 -> 高风险规则 -> 带版本的检索证据 -> 分诊卡 / 人工选科降级。分诊仅提供非诊断性指导；高风险规则命中时跳过检索和模型调用。证据阈值 0.72 表示检索相关性，不是医疗置信度。V2-V7 MySQL 迁移完成并经真实数据库验证是发布前置条件。知识重载为 ADMIN-only `POST /api/v1/admin/knowledge/reload`；公开 `/api/knowledge/reload` 已移除。

## 项目结构

```text
Intelligent-Healthcare-System/
├── ai-medical-care/                # Spring Boot 后端（Maven 单体）
│   ├── pom.xml
│   ├── src/                        # 业务、鉴权、AI Agent、资源和测试
│   ├── Dockerfile
│   └── secrets.example.txt         # 密钥配置示例
├── frontend/                       # Vue 3 Web 客户端
├── deploy/                         # Compose、Redis 与 Nginx 部署资源
├── docs/                           # 技术实现说明
├── scripts/                        # 验证和运维脚本
└── evaluation/                     # 离线评估数据
```

## 核心接口

公开接口：

- `GET /api/v1/departments`
- `GET /api/v1/departments/{department}/doctors`
- `GET /api/v1/schedules?department=&date=&period=`

登录后接口，统一携带 `Authorization: Bearer <accessToken>`：

- `POST /api/v1/appointments`，请求体仅为 `{ "scheduleId": 101 }`
- `GET /api/v1/appointments/me`
- `DELETE /api/v1/appointments/{id}`

医生工作台接口只接受带有 `DOCTOR` 角色的 JWT：

- `GET /api/v1/doctor/appointments?status=PENDING`
- `POST /api/v1/doctor/appointments/{id}/confirm`
- `POST /api/v1/doctor/appointments/{id}/reject`，请求体为 `{ "reason": "..." }`
- `POST /api/v1/doctor/appointments/{id}/complete`
- `POST /api/v1/doctor/appointments/{id}/encounter`，请求体为 `{ "summary": "...", "followUpAdvice": "..." }`

候补与就诊摘要接口：

- `POST /api/v1/waitlist`、`GET /api/v1/waitlist/me`
- `POST /api/v1/waitlist/{id}/accept`、`POST /api/v1/waitlist/{id}/cancel`
- `GET /api/v1/appointments/{id}/encounter`

管理员接口：

- `GET/POST /api/v1/admin/knowledge/documents`，以及 `/{id}/publish`、`/{id}/rollback`
- `POST /api/v1/admin/knowledge/reload`
- `GET /api/v1/admin/metrics/overview`

知识文档发布或回滚后，调用 `POST /api/v1/admin/knowledge/reload` 会将当前已发布版本与内置种子一起重新写入 Redis。上传文档默认不声明科室元数据，因此可用于通用知识检索，但不会被可信分诊策略当作科室证据；只有带有受控科室元数据的证据才能生成分诊推荐。

角色为 `PATIENT`、`DOCTOR`、`ADMIN`。角色与用户 ID 都来自 JWT，客户端不能提交医生、患者或角色 ID；Web 端仅为 `DOCTOR` 显示“医生工作台”。预约状态按 `PENDING -> CONFIRMED -> COMPLETED` 流转，患者仅能取消 `PENDING`/`CONFIRMED`，医生可拒绝 `PENDING`/`CONFIRMED`；取消或拒绝只回补一次号源。

登录和注册接口分别为 `POST /api/v1/auth/login`、`POST /api/v1/auth/register`。密码以 BCrypt 哈希保存，后端以 JWT 确定当前用户；预约创建和取消使用事务及 MySQL 条件更新保证号源不会超卖。

## 后端启动

前置条件：JDK 17、Maven、MySQL 8、MongoDB、Redis。服务默认端口为 `5137`。

1. 创建数据库 `guiguxiaozhi`，首次运行执行 `ai-medical-care/src/main/resources/db/init-data.sql`。
2. 复制 `ai-medical-care/secrets.example.txt` 为 `ai-medical-care/secrets.local.txt`，填写本机的 `DASH_SCOPE_API_KEY`、`MYSQL_PASSWORD` 与至少 32 位的 `JWT_USER_SECRET_KEY`、`JWT_ADMIN_SECRET_KEY`。该文件已被 Git 忽略，不能提交。
3. 启动依赖服务后执行：

```powershell
cd ai-medical-care
mvn spring-boot:run
```

已有旧库时，先备份数据，按 `V2__secure_appointments.sql`、`V3__roles_and_doctor_accounts.sql`、`V4__appointment_lifecycle.sql`、`V5__triage_cases.sql`、`V6__waitlist_encounter_audit.sql`、`V7__knowledge_documents.sql` 的顺序执行迁移。迁移均为可重复执行的受保护脚本。V3 增加用户角色和医生账号映射，不会为旧医生创建登录账号；V4 将旧预约标记为 `LEGACY`，不进入新工作流。不要在已有数据的库中重复执行初始化脚本。

如需构建带 RediSearch 的 Redis 镜像，在项目根目录执行 `docker build -f deploy/redis/Dockerfile -t ai-medical-care-redis .`。

如果已有演示数据库中的排班日期已经过期，执行 `ai-medical-care/src/main/resources/db/refresh-demo-schedules.sql`。执行时指定 `utf8mb4`，例如 `mysql --default-character-set=utf8mb4 -uroot -p guiguxiaozhi -e "source ai-medical-care/src/main/resources/db/refresh-demo-schedules.sql"`。该脚本只补充今天起未来 7 天缺失的上午/下午排班，不删除历史预约，可重复执行。

## Web 端

```powershell
cd frontend
npm install
npm run dev
```

使用 `VITE_API_BASE_URL` 覆盖 API 地址；未设置时开发服务器通过代理访问本机后端。

## Nginx 集成模式

项目提供了类似 Sky-Delivery 的 Nginx 目录结构。Nginx 负责提供 `frontend/dist` 静态文件，并将 `/api/`、`/xiaozhi/` 反向代理到 Spring Boot 的 `5137` 端口，因此浏览器访问时前后端使用同一个来源，不需要额外配置跨域。

仓库不提交 Nginx 二进制文件。可以将 Windows 版 Nginx 解压到 `deploy/nginx`，使 `nginx.exe` 与 `conf` 目录同级；也可以直接复用本机已有的 Nginx，并通过 `-NginxExe` 指定路径。

```powershell
cd frontend
npm run build
cd ..\deploy\nginx
.\start-nginx.ps1 -NginxExe 'D:\Resume-Projects\Intelligent-Healthcare-System\deploy\nginx\nginx.exe'
```

启动后访问 `http://localhost:8088/`，刷新 `http://localhost:8088/chat`、`http://localhost:8088/department` 等 Vue 路由也会返回前端页面。`http://localhost:8088/api/v1/departments` 可用于确认 Nginx 已经代理到后端。停止服务：

```powershell
.\stop-nginx.ps1 -NginxExe 'D:\Resume-Projects\Intelligent-Healthcare-System\deploy\nginx\nginx.exe'
```

项目默认使用 `8088`，避免与其他项目常用的 `80` 端口冲突；如需改端口，修改 `deploy/nginx/conf/nginx.conf` 中的 `listen 8088` 后再启动，并使用对应端口访问。Nginx 集成模式要求先构建 `frontend/dist`，后端必须已经监听 `5137`。

## 验证命令

```powershell
cd ai-medical-care
mvn -Dtest=DepartmentControllerTest test
mvn test
mvn -DskipTests package
cd ..
cd frontend; npm run build
cd ..; pwsh -File .\scripts\evaluate-triage.ps1
```

离线评估数据位于 `evaluation/triage-cases.jsonl`，全部为合成样例。脚本默认只校验数据集完整性；传入 `-PredictionsPath` 后输出与人工标签的一致率，不将其包装为临床准确率。

### Docker Compose

复制 `ai-medical-care/secrets.example.txt` 到被 Git 忽略的 `ai-medical-care/secrets.local.txt`，或在启动前设置 `MYSQL_PASSWORD`、`JWT_USER_SECRET_KEY`、`JWT_ADMIN_SECRET_KEY` 环境变量，然后执行 `docker compose -f deploy/docker-compose.yml up --build`。Compose 提供 MySQL 8、Redis Stack、MongoDB 和应用四个服务，卷数据可用 `docker compose -f deploy/docker-compose.yml down -v` 清理。

真实模型、向量库、Mongo CRUD 和旧库演示测试标记为 `external`，默认不执行，避免消耗模型额度或污染本地数据。准备好 DashScope/Ollama/Pinecone、MongoDB，以及完成预约表迁移后，可显式执行：

```powershell
mvn -Pexternal-integration-tests test
```
