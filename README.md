# 智能医疗助手

面向在线医疗服务场景的 Java 全栈项目：提供 AI 问诊、科室和医生查询、排班查询，以及经过登录鉴权的预约和取消预约流程。Web 端使用 Vue 3，微信小程序使用 uni-app，后端使用 Spring Boot、MyBatis-Plus、MySQL、MongoDB、Redis 和 LangChain4j。

## 项目结构

```text
ai-medical-care/
├── src/                            # Spring Boot 后端源码、资源和测试
│   ├── main/java/                  # 业务、鉴权和 AI Agent
│   ├── main/resources/
│   │   ├── application.properties  # 服务、数据库和模型配置
│   │   ├── db/                     # 初始化、迁移和演示排班脚本
│   │   └── knowledge/              # 运行时加载的医疗 Markdown
│   └── test/                       # 核心测试和外部集成测试资源
├── frontend/                       # Vue 3 Web 客户端
│   └── nginx-1.20.2/                # Nginx 配置和 Windows 启停脚本（不提交 nginx.exe）
├── miniprogram/                    # uni-app 微信小程序
│   └── dist/build/mp-weixin/       # 构建产物，导入微信开发者工具
├── deploy/redis/Dockerfile         # 可选的 Redis/RediSearch 镜像构建文件
├── secrets.example.txt             # 密钥配置示例
└── docs/技术栈文档.md               # 技术实现说明
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

登录和注册接口分别为 `POST /api/v1/auth/login`、`POST /api/v1/auth/register`。密码以 BCrypt 哈希保存，后端以 JWT 确定当前用户；预约创建和取消使用事务及 MySQL 条件更新保证号源不会超卖。

## 后端启动

前置条件：JDK 17、Maven、MySQL 8、MongoDB、Redis。服务默认端口为 `5137`。

1. 创建数据库 `guiguxiaozhi`，首次运行执行 `src/main/resources/db/init-data.sql`。
2. 复制 `secrets.example.txt` 为 `secrets.local.txt`，填写本机的 `DASH_SCOPE_API_KEY`、`MYSQL_PASSWORD` 与至少 32 位的 `JWT_SECRET`。该文件已被 Git 忽略，不能提交。
3. 启动依赖服务后执行：

```powershell
mvn spring-boot:run
```

已有旧库时，先备份数据并执行 `src/main/resources/db/migration/V2__secure_appointments.sql`，再按实际业务回填历史预约的用户和排班归属；该脚本可重复执行，旧预约的归属字段会暂时保持为空。不要在已有数据的库中重复执行初始化脚本。

如需构建带 RediSearch 的 Redis 镜像，在项目根目录执行 `docker build -f deploy/redis/Dockerfile -t ai-medical-care-redis .`。

如果已有演示数据库中的排班日期已经过期，执行 `src/main/resources/db/refresh-demo-schedules.sql`。执行时指定 `utf8mb4`，例如 `mysql --default-character-set=utf8mb4 -uroot -p guiguxiaozhi -e "source src/main/resources/db/refresh-demo-schedules.sql"`。该脚本只补充今天起未来 7 天缺失的上午/下午排班，不删除历史预约，可重复执行。

## Web 端

```powershell
cd frontend
npm install
npm run dev
```

使用 `VITE_API_BASE_URL` 覆盖 API 地址；未设置时开发服务器通过代理访问本机后端。

## Nginx 集成模式

项目提供了类似 Sky-Delivery 的 Nginx 目录结构。Nginx 负责提供 `frontend/dist` 静态文件，并将 `/api/`、`/xiaozhi/` 反向代理到 Spring Boot 的 `5137` 端口，因此浏览器访问时前后端使用同一个来源，不需要额外配置跨域。

仓库不提交 Nginx 二进制文件。可以将 Windows 版 Nginx 解压到 `frontend/nginx-1.20.2`，使 `nginx.exe` 与 `conf` 目录同级；也可以直接复用本机已有的 Nginx，并通过 `-NginxExe` 指定路径。

```powershell
cd frontend
npm run build
cd nginx-1.20.2
.\start-nginx.ps1 -NginxExe 'D:\Resume-Projects\Sky-Delivery\frontend\nginx-1.20.2\nginx.exe'
```

启动后访问 `http://localhost:8088/`，刷新 `http://localhost:8088/chat`、`http://localhost:8088/department` 等 Vue 路由也会返回前端页面。`http://localhost:8088/api/v1/departments` 可用于确认 Nginx 已经代理到后端。停止服务：

```powershell
.\stop-nginx.ps1 -NginxExe 'D:\Resume-Projects\Sky-Delivery\frontend\nginx-1.20.2\nginx.exe'
```

项目默认使用 `8088`，避免与其他项目常用的 `80` 端口冲突；如需改端口，修改 `frontend/nginx-1.20.2/conf/nginx.conf` 中的 `listen 8088` 后再启动，并使用对应端口访问。Nginx 集成模式要求先构建 `frontend/dist`，后端必须已经监听 `5137`。

## 微信小程序

```powershell
cd miniprogram
npm install
npm run build:mp-weixin
```

该小程序使用历史 alpha 工具链，项目内已禁用 npm lockfile 生成以兼容 npm 11；安装时出现弃用或历史依赖漏洞提示属于工具链已知风险，当前不执行破坏性升级。

在微信开发者工具中导入 `miniprogram/dist/build/mp-weixin`。本地开发可在项目设置中关闭“校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。真机调试时，在构建前设置电脑的局域网地址，例如：

```powershell
$env:VITE_API_BASE_URL = 'http://192.168.1.10:5137'
npm run build:mp-weixin
```

手机和电脑需要位于同一局域网，且防火墙需允许 `5137` 端口。生产环境必须配置 HTTPS 合法域名，不能关闭域名校验。

## 验证命令

```powershell
mvn -Dtest=DepartmentControllerTest test
mvn test
mvn -DskipTests package
cd frontend; npm run build
cd ../miniprogram; npm run build:mp-weixin
```

真实模型、向量库、Mongo CRUD 和旧库演示测试标记为 `external`，默认不执行，避免消耗模型额度或污染本地数据。准备好 DashScope/Ollama/Pinecone、MongoDB，以及完成预约表迁移后，可显式执行：

```powershell
mvn -Pexternal-integration-tests test
```
