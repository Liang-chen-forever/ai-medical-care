# 智能医疗助手企业化改造设计

## 目标与边界

面向 Java 后端与全栈实习，将现有项目升级为可稳定演示的“智能导诊与预约挂号平台”。项目采用 Spring Boot 单体应用和 Vue 3 Web 管理端；重点展示分层设计、鉴权、事务并发控制、缓存一致性、AI 应用安全与部署能力。

项目不是诊断系统。AI 仅提供健康科普、科室推荐和就医流程引导，不生成诊断结论或治疗方案。

## 架构

```text
Vue 3 Web
          |
          | REST API（/api/v1） + Bearer JWT
          v
Spring Boot 3
  ├── auth：注册、登录、JWT 鉴权、当前用户
  ├── department：科室、医生、排班查询
  ├── appointment：预约、取消、我的预约
  └── consultation：安全导诊、RAG 问答、降级
          |
          ├── MySQL：用户、医生、排班、预约（业务事实来源）
          ├── Redis：医生与号源的短期读取缓存
          └── MongoDB / 向量库：对话记忆与知识检索
```

Controller 只完成请求/响应转换；Service 负责业务规则与事务；Mapper 负责数据访问。跨接口统一使用 `ApiResponse<T>`、业务错误码、全局异常处理和 Jakarta Validation。

## 安全与配置

- 密码以 BCrypt 哈希保存，登录成功签发短期 JWT；需要用户身份的接口通过 `Authorization: Bearer <token>` 访问。
- `UserPrincipal` 从 JWT 中读取用户 ID；预约、查询和取消均按用户 ID 授权，客户端不再传身份证或姓名作为身份凭据。
- 模型密钥、数据库密码与 JWT 密钥仅从环境变量或被 Git 忽略的 `secrets.local.txt` 读取。仓库提供 `secrets.example.txt`，不含任何真实值。
- API 仅允许受控的跨域来源；生产环境通过 HTTPS 部署。

## 预约一致性与并发

预约请求仅传 `scheduleId`。后端按如下顺序处理：

1. 校验登录用户和排班存在。
2. 在事务内执行条件更新：`booked_slots = booked_slots + 1`，条件为 `booked_slots < total_slots`。
3. 条件更新成功后插入带 `userId` 和 `scheduleId` 的预约记录；若重复预约或插入失败，事务回滚并恢复号源。
4. 成功后删除该排班查询对应的 Redis 缓存。

数据库条件更新是最终并发保障：多个请求同时抢最后一个号时，只有一个能更新成功。Redis 不参与库存判定。

取消预约需校验预约所属用户；在事务内删除预约并将关联排班 `booked_slots` 减一（下限为零），然后删除号源缓存。取消不允许越权操作。

## API 契约

所有业务接口收敛到 `/api/v1`：

| 模块 | 接口 | 鉴权 | 说明 |
|---|---|---:|---|
| 认证 | `POST /auth/register` | 否 | 注册用户 |
| 认证 | `POST /auth/login` | 否 | 返回 token 与用户资料 |
| 医生 | `GET /departments` | 否 | 科室列表 |
| 医生 | `GET /departments/{name}/doctors` | 否 | 医生列表，Redis 缓存 |
| 排班 | `GET /schedules?department=&date=&period=` | 否 | 可预约排班，Redis 缓存 |
| 预约 | `POST /appointments` | 是 | 仅接收 `scheduleId` |
| 预约 | `GET /appointments/me` | 是 | 当前用户预约列表 |
| 预约 | `DELETE /appointments/{id}` | 是 | 当前用户取消预约 |
| 导诊 | `POST /consultations` | 可选 | 普通响应；Web 可加流式接口 |

响应格式固定为 `code`、`message`、`data`；验证失败、未认证、无权限、号源不足和重复预约有稳定错误码。Web 客户端统一使用此契约。

## AI 与缓存策略

导诊服务先检查高风险词（如胸痛、呼吸困难、自伤等）。命中时直接返回紧急就医提示并跳过模型；普通回复统一附带医疗免责声明。模型、向量库或对话记忆异常时，返回科室查询和预约入口的降级文案，核心挂号功能不受影响。

Redis 采用 Cache Aside：医生列表缓存 30 分钟，未来号源查询缓存 5 分钟。预约或取消后主动删除该排班相关缓存。MySQL 始终为业务真实来源。

## 验证与交付

- 单元/集成测试覆盖：密码哈希、JWT 鉴权、未登录访问、满号、并发抢号、重复预约、取消回补、越权取消、高风险导诊拦截。
- Docker Compose 启动 MySQL、Redis、MongoDB，应用依赖通过环境变量配置。
- README 提供环境准备、密钥配置、数据库初始化以及后端/Web 启动步骤。
- 交付时补充 API 文档、核心业务时序说明、简历描述和高频面试问答。

## 非目标

- 不接入真实医院、支付、医保、处方、电子病历或真实医疗诊断。
- 不把 Redis 锁作为号源最终一致性的前提。
- 不引入微服务、消息队列等与实习项目规模不匹配的复杂度。
