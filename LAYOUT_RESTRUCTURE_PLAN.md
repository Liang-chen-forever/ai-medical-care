# 医疗系统 布局重构执行计划（第 1 档：端隔离）

> 本文件交给编程代理（Codex）执行。目标：参照「苍穹外卖」的顶层布局，把 `frontend`、`deploy/`、`docs/`、`scripts/`、`evaluation/` 从后端目录 `ai-medical-care/` 中上提到顶层，与后端平级，同时把 git 仓库根上提到 `Intelligent-Healthcare-System/`。
>
> **核心原理**：`frontend` 一旦移到 `ai-medical-care` 之外，就脱离了当前 git 仓库（`.git` 在 `ai-medical-care`），所以必须先把 `.git` 上提到顶层，让整个 `Intelligent-Healthcare-System/` 成为新的仓库根。
>
> **全程只做目录与路径整理，不改任何业务代码。** 遇到与计划冲突的情况，停下来报告，不要猜。

---

## 0. 目标结构（改造后）

```
Intelligent-Healthcare-System/              # 【新】git 仓库根（.git 上提到此）
├── .git/
├── .gitignore
├── README.md
├── REFACTOR_PLAN.md                         # 上一轮结构重构记录
├── LAYOUT_RESTRUCTURE_PLAN.md               # 本计划
├── .github/workflows/ci.yml
│
├── ai-medical-care/                         # 后端（Maven 单体，保持内聚）
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .mvn/
│   ├── src/
│   ├── .idea/                               # IDE 配置（后被 gitignore 忽略）
│   ├── secrets.example.txt
│   ├── secrets.local.txt                    # 已被 gitignore，仅本地
│   └── target/                              # 构建产物（忽略）
│
├── frontend/                                # 前端（Vite + Vue3）
├── deploy/                                  # docker-compose / nginx / redis
├── docs/
├── scripts/
└── evaluation/
```

---

## 1. 前置准备（必须先做，且有两处需人工确认）

**1.1 工作目录与状态**
```powershell
cd D:\Resume-Projects\Intelligent-Healthcare-System\ai-medical-care
git status --short          # 必须为空（干净）
git branch --show-current   # 当前为 refactor/project-structure
```
新建执行分支：
```powershell
git checkout -b refactor/restructure-layout
```

**1.2 处理 git worktree（Codex 自主判断，按下列规则执行，不再停下来等用户）**
当前存在 3 个并行工作树分支，`.git` 上提后它们的元数据会失效：
```
feat/backend-auth-safety
chore/release-validation
integration/trusted-triage-and-master-wip
```

执行前先复核各分支的合并状态：
```powershell
git branch --merged HEAD      # 已合并分支
git branch --no-merged HEAD   # 有未合并提交的分支
git log --oneline -1 <branch> # 记录最后提交 hash 作为可追溯依据
```

判断与处理规则：
- **已合并**（出现在 `git branch --merged HEAD` 列表）→ 可安全删除，执行
  `git worktree remove .worktrees/<name>`，删除前记录其最后 commit hash。
- **有未合并提交**（出现在 `git branch --no-merged HEAD` 列表）→ **禁止删除**，
  改用 `git worktree move .worktrees/<name> <仓库外路径>` 将其移出仓库后继续，
  完成主流程后把该分支的未合并情况报告给用户决定。

已核实的事实（供判断参考）：本计划编写时，这 3 个分支均已合并进
`refactor/project-structure`（`git branch --no-merged HEAD` 为空，且各分支相对 HEAD
的领先/落后提交均为空），预计可直接 `git worktree remove`。执行时仍须按上述命令
**重新复核一次**；若状态发生变化（出现未合并提交），改走"禁止删除"分支。

**1.3 备份**
移动 `.git` 属于难逆操作，执行前至少满足其一：
```powershell
git tag pre-layout-rework      # 打标签
# 或（更稳妥）对整个外层目录做一次文件系统级复制备份
```

---

## 2. 核心操作：上提 .git 并重组目录

> 说明：以下 PowerShell 命令的当前目录（cwd）为外层
> `D:\Resume-Projects\Intelligent-Healthcare-System`。
> 移动到这一步前，请确保**没有任何终端正 cd 在 `ai-medical-care` 内部**
> （否则 `.git` 会被占用而移动失败）。

**Step 2.1 — 上提 .git**
```powershell
cd D:\Resume-Projects\Intelligent-Healthcare-System
Move-Item ai-medical-care\.git .git
```
> 此时 `git` 的工作树根已变为外层，`git status` 会显示所有已跟踪文件为「deleted」（旧路径 `pom.xml`、`src/...`、`frontend/...` 现在位于 `ai-medical-care/...` 或顶层），这是预期现象，勿惊慌。

**Step 2.2 — 上提跨端目录到顶层**
```powershell
Move-Item ai-medical-care\frontend    .\frontend
Move-Item ai-medical-care\deploy      .\deploy
Move-Item ai-medical-care\docs        .\docs
Move-Item ai-medical-care\scripts     .\scripts
Move-Item ai-medical-care\evaluation  .\evaluation
Move-Item ai-medical-care\.github     .\.github
Move-Item ai-medical-care\.gitignore  .\.gitignore
Move-Item ai-medical-care\README.md   .\README.md
Move-Item ai-medical-care\REFACTOR_PLAN.md  .\REFACTOR_PLAN.md
Move-Item ai-medical-care\LAYOUT_RESTRUCTURE_PLAN.md  .\LAYOUT_RESTRUCTURE_PLAN.md
```
> `ai-medical-care/` 内**保留不动**：`pom.xml`、`Dockerfile`、`.mvn/`、`src/`、`.idea/`、`secrets.example.txt`、`secrets.local.txt`、`target/`，以及（若前置未移除的）`.worktrees/`、`.superpowers/`、`.vscode/`。

**Step 2.3 — 重建索引（靠 rename 检测保留历史）**
```powershell
cd D:\Resume-Projects\Intelligent-Healthcare-System
git add -A
git status
```
> 用 `git add -A`（不是 `git add .`），确保「删除」也被 stage；git 会按内容相似度把旧路径→新路径识别为 rename，历史仍可用 `git log --follow <file>` 追溯。

**Step 2.4 — 提交本次移动**
```powershell
git commit -m "refactor: lift frontend and shared dirs to top level"
```

---

## 3. 连带修改（移动后的路径修复，逐个改并验证）

### 3.1 `.github/workflows/ci.yml`（必改）
后端 job 的 `mvn` 命令原在仓库根执行；仓库根变为顶层后，需指向 `ai-medical-care`。给 `backend` job 增加默认工作目录：
```yaml
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ai-medical-care
    steps:
      - uses: actions/checkout@v4
      ...
```
`web` job 的 `working-directory: frontend` 与 `cache-dependency-path: frontend/package-lock.json` **保持不变**（frontend 仍在顶层）。

### 3.2 `deploy/docker-compose.yml`（必改）
`app.build.context` 原来指向仓库根（`..`），现在 compose 位于顶层 `deploy/`，需指向后端：
```yaml
  app:
    build:
      context: ../ai-medical-care
      dockerfile: Dockerfile
```
其余（`Dockerfile.redis`、`deploy/nginx/`、`deploy/redis/`）位置已在原相对路径内，无需改。若 compose 依赖了与 `docker-compose.yml` 同目录的 `.env`，确认其位置一并处理。

### 3.3 `README.md`（必改，需全文核对）
至少处理以下引用：
- 顶部目录树重画为第 0 节的目标结构；
- 两处硬编码绝对路径 `D:\Resume-Projects\Intelligent-Healthcare-System\ai-medical-care\deploy\nginx\nginx.exe`
  → 去掉中间的 `\ai-medical-care`，改为 `...\Intelligent-Healthcare-System\deploy\nginx\nginx.exe`；
- 后端 SQL 相对路径 `src/main/resources/db/...`（若文档默认「在项目根执行」）→ 前缀加 `ai-medical-care/`；
- `cd frontend`、`docker build -f deploy/redis/Dockerfile` 这类以顶层为基准的路径一般不变，逐条确认。

### 3.4 `deploy/nginx/start-nginx.ps1`（验证即可，预期已正确）
上提后 `deploy/nginx` 位于顶层 `deploy/` 下，脚本里
`$repositoryRoot = ... $nginxHome '..\..'` + `frontend\dist` 恰好指向「顶层\frontend\dist」，与新结构一致。**确认不需改**；若脚本内仍有旧绝对路径残留，一并更新。

### 3.5 `.gitignore`（核对，不一定改）
`.gitignore` 上提到顶层后，核对以下规则在新位置是否仍命中：
- `target/`、`frontend/node_modules/`、`frontend/dist/` 仍匹配新位置；
- `deploy/nginx/nginx.exe`、`deploy/nginx/logs/*.log` 等仍匹配；
- `.worktrees/`、`.superpowers/`、`.pnpm-store/` 等无前缀规则仍生效。
> 若某条规则此前带路径、现已失效，改为无前缀或按新相对路径修正。

---

## 4. 验证

```powershell
cd D:\Resume-Projects\Intelligent-Healthcare-System

# 1) 工作区干净、无遗漏
git status
git log --oneline -3

# 2) 历史可追溯（rename 检测生效）
git log --follow --name-status -- README.md

# 3) 后端编译（外部依赖如 mysql/redis/mongo 未启动时，仅需 compile 通过）
cd ai-medical-care
mvn -B -q clean compile

# 4) 前端构建
cd ..\frontend
npm ci
npm run build

# 5) compose 语法校验
docker compose -f ..\deploy\docker-compose.yml config -q
```

---

## 5. 风险与回滚

- **难逆点**：`.git` 位置已改变。检视/回滚前先确认没有其他进程占用目录。
- **回滚步骤**（如需还原到改造前）：
  ```powershell
  cd D:\Resume-Projects\Intelligent-Healthcare-System
  Move-Item .git ai-medical-care\.git
  cd ai-medical-care
  git reset --hard pre-layout-rework   # 或改造前 commit hash
  ```
  注意：`reset` 不会清理 `ai-medical-care` 之外因移动产生的 untracked 目录，需自行核对后 `git clean -fd`（慎用）。
- **边界**：`.worktrees/`、`.superpowers/`、`.idea/`、`.vscode/`、`target/`、`node_modules/`、`dist/` 属工具/构建产物，不纳入结构整理范畴；`secrets.local.txt` 全程不得提交。

---

## 6. 完成标准（Definition of Done）

- [ ] 顶层出现 `.git`，`git` 认 `Intelligent-Healthcare-System` 为仓库根
- [ ] `ai-medical-care/` 内仅剩后端（`pom.xml`、`src/`、`Dockerfile`、`.mvn/` 等）
- [ ] `frontend/`、`deploy/`、`docs/`、`scripts/`、`evaluation/`、`.github/` 均位于顶层
- [ ] `ci.yml` 后端 step 指向 `ai-medical-care`，前端 step 指向 `frontend`
- [ ] `docker-compose.yml` 的 `build.context` 为 `../ai-medical-care`
- [ ] `README.md` 无失效路径、无旧绝对路径
- [ ] `git status` 干净；`mvn clean compile` 与 `npm run build` 通过
- [ ] 每个改动独立 commit，历史可追溯