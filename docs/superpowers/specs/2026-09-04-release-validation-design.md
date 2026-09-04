# 发布验证设计

## 目标

将受信任分诊和预约增强从“本地可编译的演示”提升为可重复执行的发布前检查：迁移能够在隔离 MySQL 数据库中重复执行，分诊证据写入失败能够验证事务回滚，管理员知识重载能够在隔离 Redis/RediSearch 索引中真实写入并检索。

## 范围与非目标

- 覆盖 `V2` 至 `V5` 的可移植性、幂等性及索引存在性。
- 使用真实 MySQL 8 验证 `TriageServiceImpl#create` 中证据插入失败时父 `triage_case` 与子 `triage_evidence` 均不落库。
- 使用真实 Redis Stack/RediSearch 的唯一临时索引验证 `KnowledgeSeedLoader#reload`。现有 `xiaozhi-index` 和 `langchain4j:vector:xiaozhi:` 永不作为验证目标。
- MongoDB 本次仅做服务连通性前置检查；发布验证逻辑不读写聊天记忆，避免与本地会话数据混用。
- 不引入 Flyway，不修改已有演示数据库，不执行远程发布。

## 设计决策

### 隔离数据库与迁移

`V2__secure_appointments.sql` 删除硬编码的 `USE guiguxiaozhi`。迁移始终作用于调用方已经选定的数据库，验证脚本创建的数据库名称必须匹配 `ai_medical_care_release_validation_yyyyMMddHHmmss`，并在结束时只删除该名称的数据库。

脚本用最小遗留表结构建立基线，顺序执行 V2--V5 两轮；第二轮不得失败。随后查询 `information_schema` 断言字段、唯一索引和普通索引均存在。

### 事务回滚验证

真实数据库测试使用应用的 MyBatis mapper 与 `TriageServiceImpl`。它在临时数据库中建立一个仅针对 `triage_evidence` 的 `BEFORE INSERT` 触发器，触发器主动抛错。测试调用常规、有证据的分诊请求，并断言服务抛错后 `triage_case` 和 `triage_evidence` 计数均为零。这验证的是 Spring 事务与实际 InnoDB 行为，不是 mock 的交互次数。

### 知识重载验证

`KnowledgeSeedLoader` 显式接收 Spring 管理的 `EmbeddingModel`，并用 `EmbeddingStoreIngestor.builder()` 构造 ingestor。这样重载使用配置中的模型和维度，不会隐式通过 SPI 选择另一个嵌入模型。

Redis 存储的主机、端口、索引名、键前缀和维度由 `app.knowledge.redis.*` 配置。生产默认值保持当前 `xiaozhi-index` 行为；外部测试直接用每次生成的索引和前缀、8 维确定性测试模型，重载后执行真实向量检索。最后只删除该唯一测试索引及其文档。

### 运行入口与证据

`scripts/release-validation.ps1` 只接受显式的验证凭据环境变量，创建随机临时数据库和 Redis 索引，再运行 `external-integration-tests` profile 的测试。它在 `docs/verification/runs/` 生成无敏感信息的 Markdown 结果。生成目录被 Git 忽略，版本库保留验证说明和脚本本身。

## 安全约束

- 不从 Git 读取、打印或写入密钥；脚本只消费环境变量。
- 当 JDBC URL 的数据库名不匹配临时数据库前缀时，外部 MySQL 测试立即失败。
- 删除操作只针对本次脚本创建且匹配校验格式的临时数据库、临时 Redis 索引。
- 外部集成测试由 JUnit `external` tag 隔离；默认 `mvn test` 永不执行。

## 验收标准

1. 默认单元测试覆盖迁移脚本可移植性与知识加载器所使用的嵌入模型。
2. 发布脚本对临时 MySQL 执行 V2--V5 两轮并断言预期索引。
3. `external-integration-tests` profile 通过真实数据库触发器证明事务回滚。
4. 同一 profile 通过真实 Redis/RediSearch 的唯一索引完成知识重载和检索。
5. 运行产物记录运行时间、命令、临时资源标识、成功/失败状态，但不记录密码或 API Key。
