# RQ-002：知识库服务架构与业务流程

## 状态

规划需求，尚未选为实施中的唯一活动需求。

## 范围

定义 `g2rain-knowledge` 的 Java 服务模块、知识版本状态机、异步处理、发布/回滚、客服检索、反馈闭环，以及与 MySQL、Milvus、Docling、Embedding 和 Rerank 的职责边界。

## 架构基线

- 目标采用 G2rain `java-domain-service 1.0.0`：`startup → biz → api`。
- API 发布查询 VO、SelectDto 和明确的受信业务命令；不得发布 PO、Biz Service 或宽泛远程 CRUD。
- Biz 拥有事务、状态机、幂等、组织隔离和数据访问；Startup 只负责配置、运行时组装、健康检查和镜像。
- `g2rain-common` 与 `g2rain-generator-maven-plugin` 是强制基础设施，详见 [`g2rain-foundation.md`](../architecture/g2rain-foundation.md)。

## 非目标

- 不实现 Agent Runtime 的多轮会话编排与会话状态（本服务可提供无会话的受控问答 `POST /knowledge/answer`）。
- 不实现 Docling、Milvus、Embedding 或 Rerank 的基础设施生命周期。
- 不将 Milvus 作为发布、权限或元数据权威。

## 业务与安全影响

- 所有组织数据从 `PrincipalContext` 取得 `organ_id`，并贯穿 MySQL、本地文件与 Milvus。
- 版本状态机权威顺序为 `DRAFT → REVIEWING → APPROVED → PROCESSING → READY_TO_PUBLISH`：人工**只审核源文件/FAQ 与标签**；**审核通过后才**由 `PROCESS` 任务创建 Embedding/Milvus 写入；详见 [`business-flows.md`](../architecture/business-flows.md) 与 [`deviations.md`](../architecture/deviations.md) DEV-001。
- 解析、切片与索引网络调用由审核后的 `PROCESS` 任务在事务外执行。首期仅支持人工同步发布：Milvus 预启用和旧 Entity 关闭均在 MySQL 短事务外执行，发布不创建任务。
- 检索以 Milvus `organ_id + searchable=true + tags` 召回，再经 MySQL 活动版本复核；不在生产查询中常态携带大量 `revision_id`。标签仅参与业务筛选。
- 新增或修改对外 API、任务语义、状态枚举、错误码和事件前，必须评估 API 兼容性与 Agent Runtime 影响。

## 验收标准

1. Maven 模块依赖保持 `startup → biz → api`，且不存在反向依赖。
2. 基础 CRUD 由生成器生成并保留 `organ_id`、`delete_flag` 和公共基类契约；领域命令不直接暴露通用 CRUD。
3. 文件/FAQ 能按「源内容审核 → PROCESS（解析/切片/Embedding/Milvus）→ 可发布」走到 `READY_TO_PUBLISH`；审核前不写 Milvus；外部调用失败可重试且不产生部分发布。
4. 人工发布、回滚和下线满足 release 与 revision 的预期版本 CAS、Milvus 可见性维护和 MySQL 权威状态要求；不存在发布任务。
5. 检索、Rerank、反馈和恢复路径均有组织隔离、活动版本与敏感信息保护测试。

## 依赖与验证

- 依赖 MySQL 8.0.13+、Docling Serve、Milvus、外部 Embedding/Rerank API、`g2rain-common` 和代码生成插件。
- 实现后执行 `mvn clean verify`，并补充真实 MySQL 事务、代码生成、Milvus 过滤和外部依赖失败的集成测试。
