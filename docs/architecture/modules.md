# 模块与依赖边界

本服务目标采用 G2rain `java-domain-service 1.0.0` 基线：

```text
g2rain-knowledge-startup → g2rain-knowledge-biz → g2rain-knowledge-api
```

| Maven 模块 | 平台职责 | 知识库职责 |
| --- | --- | --- |
| `api` | 查询契约、VO、SelectDto、稳定枚举与明确的受信命令 | 检索证据 VO、知识只读查询、具有明确语义的受信命令契约 |
| `biz` | Controller、Service、Domain、DAO、Converter 和本地写入 DTO | 管理用例、状态机、任务、发布事务、索引适配器和检索编排 |
| `startup` | 启动、运行配置、Actuator、镜像组装 | Bean 装配、外部客户端配置、任务 worker、健康检查 |

`api` 不依赖 `biz` 或 `startup`，不发布 PO、Biz Service 或宽泛远程 CRUD；`startup` 不承载领域逻辑。

| 逻辑模块 | 责任 | 允许依赖 | 不应承担 |
| --- | --- | --- | --- |
| 管理 API | 文件/FAQ、审核、发布和反馈请求 | 应用服务、鉴权与审计 | 直接操作 Milvus 或磁盘路径 |
| 知识应用服务 | 生命周期编排、版本裁决、发布事务 | 元数据仓储、任务、索引适配器 | 回答生成或对话状态 |
| 解析与切片适配器 | 调用 Docling，规范化结构化结果 | Docling HTTP API | 发布状态切换 |
| 索引适配器 | Embedding、Rerank、Milvus 写入/检索 | 外部模型、Milvus | 作为元数据真相来源 |
| 文件存储适配器 | 原文件与解析产物的受控读写 | 本地持久化卷 | 接受未净化的用户路径 |
| 检索 API | 检索条件校验、证据与引用输出 | 应用服务 | 返回未生效或跨租户数据 |
| 审计与任务 | 状态迁移、失败记录、可观测性 | MySQL、日志/指标 | 记录原文或 Secret |

Biz 内部依赖方向应保持为 Controller → Service → DAO，Service 可调用 Domain / Client，Converter 不发起数据库或网络调用。MySQL 持有知识元数据和发布真相；Milvus 仅持有可重建的已发布索引。

所有 `knowledge_*` 表的基础 CRUD 必须通过 `g2rain-generator-maven-plugin` 生成，并复用 `g2rain-common` 的基类、ID、主体上下文、校验、异常和返回契约。领域模块只能在生成代码之上扩展，详细规则见 [G2rain 公共基线与代码生成约束](g2rain-foundation.md)。
