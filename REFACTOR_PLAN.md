# 智能医疗系统 项目结构重构执行计划

> 本文件是交给编程代理（Codex）执行的重构计划。请**严格按 Phase 顺序**执行，
> **每个 Phase 用 git 单独提交一次**，并在进入下一 Phase 前完成该 Phase 末的「验证」。
> 全程**只做结构整理，不改变任何业务行为**；如某条命令与实际情况冲突，先停下来报告，不要猜。

## 重要前提

- **仓库根目录**：`ai-medical-care/`（本文件所在目录，即 `.git` 所在目录）。
  下文所有相对路径一律相对于此目录。
- **外壳目录**：`ai-medical-care` 的上级目录 `Intelligent-Healthcare-System/` 只是容器文件夹，
  里面混进了缓存在 `.pnpm-store/` 和一个多余的 `.idea/`，见 Phase 1 与 Phase 6。
- **技术栈**：Spring Boot 3 + Java 17 + Maven + MyBatis-Plus + langchain4j（后端），
  Vite + Vue 3（前端）。包名当前为 `com.Liang.java.ai.langchain4j`。
- **开始前**：确认 `git status` 干净（所有未提交改动先提交或 stash），并新建一个执行分支：
  ```powershell
  git checkout -b refactor/project-structure
  ```

---

## 0. 背景与目标

项目功能已较完整，但目录结构存在冗余嵌套、包名不合规、后端分包风格不一致、
部署/文档散落等问题。本次重构目标是：**在不改动业务逻辑的前提下**，让目录命名规范、
层次清晰、便于他人与 CI 理解维护。

---

## 1. 现状诊断（问题清单）

| # | 问题 | 影响 | 对应 Phase |
|---|------|------|-----------|
| A | 外壳目录混入 `Intelligent-Healthcare-System/.pnpm-store`（约 160MB）与多余 `.idea` | 磁盘浪费、语义混乱 | Phase 1 / 6 |
| B | `.gitignore` 未覆盖 `.pnpm-store/`、`.superpowers/` | 可能误提交缓存/工具目录 | Phase 2 |
| C | 部署文件散落：`docker-compose.yml`、`Dockerfile.redis` 在根，`frontend/nginx-1.20.2/` 塞在前端目录 | 部署语义混乱 | Phase 3 |
| D | `docs/` 含中文文件名 | 跨平台/CI 有编码隐患 | Phase 3 |
| E | Java 包名 `com.Liang.java.ai.langchain4j`：首段大写、含 `java` 段、`ai.langchain4j` 有误导 | 不合 Java 规范 | Phase 4 |
| F | 后端「按层」与「按领域」分包并存且不一致 | 同一业务代码散落多处 | Phase 5 |
| G | 外壳多一层 `ai-medical-care` 嵌套，名字与项目不对应 | 路径冗余 | Phase 6（可选） |

---

## 2. 目标结构

```
ai-medical-care/                        # 仓库根（保持不动）
├── pom.xml                             # 后端 Maven 根
├── src/
│   ├── main/java/com/liang/medical/    # 包名重写（见 Phase 4/5）
│   ├── main/resources/
│   └── test/
├── Dockerfile                          # 后端镜像构建
├── frontend/                           # Vite + Vue3 前端（保持）
├── deploy/                             # 统一部署归置（Phase 3）
│   ├── docker-compose.yml
│   ├── Dockerfile.redis
│   ├── nginx/                          # 原 frontend/nginx-1.20.2
│   └── redis/
├── docs/                               # 英文文件名（Phase 3）
├── scripts/
├── evaluation/
├── .github/
├── .gitignore
├── README.md
└── secrets.example.txt                 # 仅保留模板
```

---

## 3. 分阶段执行

### Phase 1 —— 清理冗余目录与缓存（低风险）

**目标**：删除外壳目录中的 pnpm 全局缓存和多余 IDE 配置。

**操作**：
```powershell
# 在仓库根 ai-medical-care 下执行，外壳目录即「..」
Remove-Item -Recurse -Force ..\.pnpm-store
Remove-Item -Recurse -Force ..\.idea
```

**验证**：外层目录只剩 `ai-medical-care` 一个子项。

**风险**：无。`.pnpm-store` 是可重建的全局缓存，删除不影响构建。

---

### Phase 2 —— 补充 .gitignore（低风险）

**目标**：覆盖未纳入忽略的缓存/工具目录。

**操作**：在 `.gitignore` 末尾追加：
```gitignore
### Global caches / tool dirs ###
.pnpm-store/
.superpowers/
.directory
```

**验证**：`git status` 中不出现 `.pnpm-store/`、`.superpowers/` 相关未跟踪条目。

---

### Phase 3 —— 归置部署文件 + 文档改名（低-中风险）

**目标**：把部署相关文件统一收进 `deploy/`，nginx 目录迁出前端，文档改英文名。

**3.1 移动 docker-compose 与 redis 构建文件**
```powershell
git mv docker-compose.yml deploy/docker-compose.yml
git mv Dockerfile.redis    deploy/Dockerfile.redis
```

> 连带修改：`deploy/docker-compose.yml` 中 `app.build` 的
> `context: .` 必须改为 `context: ..`（compose 文件 Ta 现在位于 `deploy/`，`..` 才指回仓库根）。
> 其余（`dockerfile: Dockerfile`）不变。

**3.2 迁移 nginx 目录**
nginx 目录目前只有 5 个文件被 git 跟踪（`conf/nginx.conf`、`logs/.gitkeep`、
`start-nginx.ps1`、`stop-nginx.ps1`、`temp/.gitkeep`），`nginx.exe` 等本地产物已被忽略。
```powershell
git mv frontend/nginx-1.20.2/conf/nginx.conf        deploy/nginx/conf/nginx.conf
git mv frontend/nginx-1.20.2/start-nginx.ps1        deploy/nginx/start-nginx.ps1
git mv frontend/nginx-1.20.2/stop-nginx.ps1         deploy/nginx/stop-nginx.ps1
git mv frontend/nginx-1.20.2/logs/.gitkeep          deploy/nginx/logs/.gitkeep
git mv frontend/nginx-1.20.2/temp/.gitkeep          deploy/nginx/temp/.gitkeep
Remove-Item -Recurse -Force frontend/nginx-1.20.2   # 清理残留空目录/未跟踪文件
```

> 连带修改（重要）：`deploy/nginx/start-nginx.ps1` 依赖「nginx 目录的父目录是 frontend」
> 来定位 `dist`，迁移后该假设失效，需同步改：
> - 脚本中 `Join-Path (Split-Path $nginxHome -Parent) 'dist'` 改为直接指向仓库根的
>   `frontend/dist` 绝对/相对路径（例如基于仓库根计算）。
> - 报错文案中的 `frontend/nginx-1.20.2/nginx.exe`、`frontend/dist` 一并改为新路径
>   `deploy/nginx/nginx.exe`、`frontend/dist`。
> - 若 `stop-nginx.ps1` 也有同样硬编码，同步修改。
> 迁移后需人工跑一遍确认能正确定位 `frontend/dist` 并启动校验。

**3.3 文档改英文名**
```powershell
git mv "docs/技术栈文档.md"    docs/tech-stack.md
git mv "docs/向量大模型搭建笔记.md" docs/vector-llm-notes.md
```
> 连带修改：全局检索（README、`docs/` 内、`src` 内）是否引用了这两个中文文件名或相对链接，
> 若有则同步替换。

**验证**：
- `git status` 显示全部为 rename + 少量修改；
- `deploy/docker-compose.yml` 的 `build.context` 已改为 `..`；
- nginx 脚本不再引用 `nginx-1.20.2`。

**风险**：中。docker-compose 的 build context 与 nginx 脚本路径是两处最容易断的连带项，务必改。

---

### Phase 4 —— 重命名 Java 包与 groupId（高风险，核心）

**目标**：将 `com.Liang.java.ai.langchain4j` → `com.liang.medical`，groupId `com.Liang` → `com.liang`。

**背景确认（已核实）**：启动类为 `MedicalCareApp.java`（`@SpringBootApplication`，无显式
`@MapperScan`）；`application.properties` 未配置 `mapper-locations` 或类型别名包；
唯一手写 Mapper XML 为 `src/main/resources/mapper/AppointmentMapper.xml`，
其 `namespace` 和 `resultType` 硬编码了完整类名。

**操作步骤**（顺序不能乱）：

**4.1 文本替换包名**（先改内容，再动目录）
```powershell
# 覆盖 java 源码与 XML（含 mapper XML），仅精确替换本项目的包名，不会误伤第三方 langchain4j 配置
Get-ChildItem -Recurse -Path src -Include *.java,*.xml -File | ForEach-Object {
  $c = Get-Content $_.FullName -Raw
  $c = $c -replace 'com\.Liang\.java\.ai\.langchain4j', 'com.liang.medical'
  Set-Content -NoNewline -Path $_.FullName -Value $c
}
```

**4.2 修改 pom.xml 的 groupId**
将 `pom.xml` 中默认坐标 `<groupId>com.Liang</groupId>` 改为 `<groupId>com.liang</groupId>`
（只改**本项目自身的**这条，不要动 Spring Boot 等第三方依赖的 groupId）。

**4.3 移动物理目录**
```powershell
New-Item -ItemType Directory -Force src/main/java/com/liang
Move-Item src/main/java/com/Liang/java/ai/langchain4j src/main/java/com/liang/medical
Remove-Item -Recurse -Force src/main/java/com/Liang

New-Item -ItemType Directory -Force src/test/java/com/liang
Move-Item src/test/java/com/Liang/java/ai/langchain4j src/test/java/com/liang/medical
Remove-Item -Recurse -Force src/test/java/com/Liang
```

**4.4 检查确认点**
- [ ] 全局检索 `com.Liang` 应**零残留**（排除 `.git` 历史）：
  ```powershell
  Get-ChildItem -Recurse -Path src,pom.xml -File | Select-String 'com\.Liang'
  ```
- [ ] Mapper XML：`src/main/resources/mapper/AppointmentMapper.xml` 的 `namespace` 与 `resultType`
      已变为 `com.liang.medical.mapper.AppointmentMapper`、`com.liang.medical.entity.Appointment`。
- [ ] MyBatis-Plus 扫描：确认所有 Mapper 接口上**有 `@Mapper` 注解**。若**没有**且此前依赖
      包扫描自动注册，则需在 `MedicalCareApp` 上加
      `@MapperScan("com.liang.medical.mapper")`（因为 MyBatis-Plus 不会自动改扫描路径）。

**4.5 编译验证**
```powershell
mvn -B -q clean compile          # 主代码编译
mvn -B -q test-compile            # 测试代码编译
mvn -B test                       # 全量测试（至少关键测试通过）
```

**风险**：高。必须单开分支 + 单独 commit。若 `mvn test` 失败，切勿继续 Phase 5。
**回滚**：`git reset --hard refactor/project-structure 前一次提交`。

---

### Phase 5 —— 统一后端分包风格（中高，建议一并做）

**目标**：把「按层」与「按领域」两套并存的结构统一为**按领域（feature）**分包。

**当前状态**：顶层同时存在
- 按层：`controller/ dto/ entity/ mapper/ service(+impl)/ common/ config/ util/ store/ tools/ bean`
- 按领域：`appointment/ assistant/ audit/ auth/ knowledge/ triage/ waitlist`

**推荐目标**（保留纯横切包 `common/config/util`，业务按领域收敛）：
```
com/liang/medical
├── MedicalCareApp.java
├── common/  config/  util/                 # 横切（保持）
├── appointment/{controller,service,dto,entity,mapper}
├── triage/      {controller,service,dto,entity,mapper}
├── waitlist/    {controller,service,dto,entity,mapper}
├── auth/        {controller,service,dto,entity,mapper}
├── knowledge/   {controller,service,dto,entity,mapper}
├── audit/       {controller,service,dto,entity,mapper}
└── assistant/   {controller,service,dto,entity,mapper}
```

**执行方式**：用 IDE 的 `Refactor > Move Class`（若可用）逐个字段搬移，或手动 `git mv` 单个
`.java` 文件到目标领域包，再改其 `package` 声明与相关 `import`。

> 注意：`dto/` 下目前已按领域拆了 `appointment/auth/doctor/encounter/knowledge/metrics/triage/waitlist`，
> 但 `entity/mapper/service/controller` 是平铺的——迁移时以「领域」为唯一维度对齐。

**验证**：重复 Phase 4.5 的编译与测试；`git status` 中仅见文件移动（rename 检测）。

**风险**：中高。**必须先完成 Phase 4 并全绿**再执行；每迁移一个领域 commit 一次。

---

### Phase 6 —— 清理外壳目录（可选，中风险）

**目标**：去掉 `ai-medical-care` 这层无意义嵌套，让项目根名字与项目一致。

**可选方案（二选一，非必须）**：
- **方案 A（推荐，动 `.git` 少）**：不做目录上提，仅把外壳目录
  `Intelligent-Healthcare-System` 的脏内容清干净（Phase 1 已做），并保持现状。
  项目根仍是 `ai-medical-care`，接受这一层命名。
- **方案 B**：把仓库根目录 `ai-medical-care` 重命名为 `Intelligent-Healthcare-System`，
  外壳目录删除或改为普通父目录。

**若执行方案 B**：
```powershell
# 在 d:\Resume-Projects 下
git mv Intelligent-Healthcare-System/ai-medical-care Intelligent-Healthcare-System-tmp
Remove-Item -Recurse -Force Intelligent-Healthcare-System
Rename-Item Intelligent-Healthcare-System-tmp Intelligent-Healthcare-System
```
> 牵动项：`.github/workflows` 中若有 `working-directory`、README 中的相对路径、
> 文档中的绝对路径引用，需全局核对。

**风险**：中。**必须在 Phase 1–5 全部完成且提交后再做**；改完跑一遍 CI 或至少 `mvn test`。

---

## 4. 验证命令汇总

```powershell
# 干净状态检查
git status

# 无旧包名残留（排除 .git）
Get-ChildItem -Recurse -Path src,pom.xml -File | Select-String 'com\.Liang'

# 后端全量验证
mvn -B clean test

# 前端构建验证（可选，验证 nginx 迁移未破坏引用前可不跑）
cd frontend; npm install; npm run build; cd ..
```

## 5. Definition of Done（验收清单）

- [ ] `Intelligent-Healthcare-System/` 下无 `.pnpm-store` 与多余 `.idea`
- [ ] `.gitignore` 覆盖 `.pnpm-store/`、`.superpowers/`
- [ ] `deploy/` 内包含 `docker-compose.yml`、`Dockerfile.redis`、`nginx/`、`redis/`，
      且 compose 的 `build.context` 正确指向仓库根
- [ ] `frontend/` 内不再有 `nginx-1.20.2/`
- [ ] `docs/` 无中文文件名，且无失效内链
- [ ] 全仓检索 `com.Liang` / `com.Liang.java.ai.langchain4j` 为零
- [ ] `pom.xml` groupId 为 `com.liang`，代码包为 `com.liang.medical`
- [ ] `mvn clean test` 全绿
- [ ] 每个 Phase 独立提交，git log 可追溯

## 6. 常见坑与注意事项

1. **别用全局无脑替换**：`langchain4j.*`、`dev.langchain4j.*` 等第三方配置/类名**不能动**，
   只替换精确前缀 `com.Liang.java.ai.langchain4j`。
2. **nginx 脚本路径**：迁移后 `start-nginx.ps1` 里靠「父目录=frontend」定位 `dist` 的逻辑最易漏改，务必改并实测。
3. **docker-compose build context**：compose 文件移进 `deploy/` 后 `context` 必须从 `.` 变 `..`。
4. **secret 文件**：`secrets.local.txt` 已被 `.gitignore` 排除，全程**不要**纳入提交；
   `secrets.example.txt` 作为模板保留。
5. **业务行为零变更**：本次只整理结构，不改接口、不改 SQL、不改业务逻辑；改动若超过结构范畴，停下确认。