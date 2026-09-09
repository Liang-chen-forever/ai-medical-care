# 向量大模型（Embedding）搭建笔记

## 概述

本文档记录了在 Spring Boot + LangChain4j 项目中集成向量嵌入模型（Embedding Model）时遇到的难点和解决方案。

**技术栈：**
- Spring Boot 3.x
- LangChain4j 1.0.0-beta3
- 阿里云百炼 DashScope（OpenAI 兼容接口）
- Pinecone 向量数据库（暂未启用）

---

## 难点一：EmbeddingModel Bean 缺失

### 现象

```
NoSuchBeanDefinitionException: No qualifying bean of type
'dev.langchain4j.model.embedding.EmbeddingModel' available
```

Spring 容器启动时找不到 `EmbeddingModel` 类型的 Bean，导致 `@Autowired` 注入失败。

### 原因

项目中只配置了 `chat-model`（聊天模型），没有配置 `embedding-model`（嵌入模型），LangChain4j 的自动配置无法自动创建 `EmbeddingModel` Bean。

### 解决方案

在 `application.properties` 中添加 Embedding 模型配置。本项目使用阿里云百炼的 OpenAI 兼容接口：

```properties
langchain4j.open-ai.embedding-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.embedding-model.api-key=你的API-KEY
langchain4j.open-ai.embedding-model.model-name=text-embedding-v3
```

**注意点：**
- `base-url` 和 `api-key` 可以与聊天模型共用同一个阿里云百炼端点
- `model-name` 推荐使用 `text-embedding-v3`，支持中英文，向量维度 1024~3072
- 配置前缀是 `langchain4j.open-ai.embedding-model.*`，不要误写成 `chat-model`

---

## 难点二：Pinecone 向量数据库连接失败

### 现象

```
SSLHandshakeException: Remote host terminated the handshake
PineconeUnmappedHttpException
```

Spring 容器启动时，`EmbeddingStoreConfig` 尝试创建 `PineconeEmbeddingStore` Bean，连接 Pinecone 云服务失败。

### 原因

Pinecone 是国外云服务，国内网络环境无法直接访问（被墙），SSL 握手被远程主机中断。

### 解决方案

使用 `@Profile` 注解隔离 Pinecone 配置，仅在需要时激活：

```java
@Configuration
@Profile("pinecone")  // 仅在激活 pinecone Profile 时加载
public class EmbeddingStoreConfig {
    // ...
}
```

**效果：**
- 默认 Profile 下不会加载 Pinecone 配置，避免启动失败
- 生产环境部署到海外服务器时，通过 `--spring.profiles.active=pinecone` 激活
- 本地开发可使用 `InMemoryEmbeddingStore`（内存向量存储）替代

**注意点：**
- 如果项目中有多个向量数据库配置（如 Pinecone、Milvus、Weaviate），建议都用 `@Profile` 隔离
- 内存向量存储 `InMemoryEmbeddingStore` 仅适合开发测试，重启后数据丢失

---

## 难点三：阿里云百炼 Embedding API 批量大小限制

### 现象

```
InvalidRequestException: 400
"batch size is invalid, it should not be larger than 10.: input.contents"
```

调用 `EmbeddingStoreIngestor.ingest()` 批量上传知识库文档时，API 返回 400 错误。

### 原因

阿里云百炼的 Embedding API 限制每次请求最多只能传 **10 个文本片段**。而 `OpenAiEmbeddingModel` 默认的批量大小可能超过此限制，导致请求被拒绝。

### 解决方案

在 `application.properties` 中限制单次批量大小：

```properties
langchain4j.open-ai.embedding-model.max-segments-per-batch=10
```

**效果：**
- `OpenAiEmbeddingModel` 会自动将大量文本片段拆分为每批最多 10 个，逐批发送
- 文档再多也不会触发 API 限制

**注意点：**
- 不同 Embedding 服务商的批量限制不同，接入前务必查阅官方文档
- OpenAI 官方 API 限制通常是 2048 条/批，但阿里云百炼限制为 10 条/批
- 批量越小，请求次数越多，耗时越长，可根据实际情况调整

---

## 完整配置参考

以下是本项目 `application.properties` 中与 Embedding 相关的最终配置：

```properties
# OpenAI Embedding 模型配置（阿里云百炼）
langchain4j.open-ai.embedding-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.embedding-model.api-key=你的API-KEY
langchain4j.open-ai.embedding-model.model-name=text-embedding-v3
langchain4j.open-ai.embedding-model.max-segments-per-batch=10
```

---

## 总结

| 难点 | 根因 | 解决方式 |
|------|------|----------|
| Bean 缺失 | 未配置 embedding-model 属性 | 添加 `langchain4j.open-ai.embedding-model.*` 配置 |
| Pinecone 连接失败 | 国内网络无法访问国外云服务 | 使用 `@Profile` 隔离，本地用内存存储 |
| API 批量超限 | 阿里云百炼限制每次最多 10 条 | 设置 `max-segments-per-batch=10` |

**核心经验：**
1. 使用 LangChain4j Spring Boot Starter 时，`chat-model` 和 `embedding-model` 需要分别配置
2. 国内开发优先选择国内可访问的服务（阿里云百炼、Ollama 本地部署等）
3. 接入第三方 API 前务必确认其限制参数（批量大小、速率限制、向量维度等）