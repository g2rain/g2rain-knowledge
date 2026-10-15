# 测试策略

验证命令：`mvn clean verify`（根 POM）。

## 本轮已覆盖（单元）

- 状态机、标签策略、本地存储、Docling/Embedding 客户端适配。
- Retrieve：`Deadline`、content SHA-256 匹配丢弃、`KnowledgeRetrieveExceptionAdvice` → HTTP 503。
- Task：PENDING `cancel` 成功 / 非 PENDING 拒绝。

## 仍待验证（未跑或延期）

- 真实 MySQL 事务 / `@DataIsolation` IT、Milvus 联调、发布 CAS 顺序与 offline/reconcile。
- `replaceFile` multipart 端到端、RUNNING 协作取消、任务租约 fencing。
- Answer 与 Runtime 契约的浏览器/联调冒烟；P6/P7 恢复演练。

## 基线清单（长期）

- 单元：状态迁移、标签、路径净化、引用、失败处理、503/Deadline/hash。
- 集成：发布/回滚、Milvus staged、Docling、Embedding/Rerank。
- 安全：跨组织拒绝、路径穿越、Secret 脱敏、VO 路径不外泄。
- 契约：未知 tag 拒绝、`NO_EVIDENCE`、依赖 503、总预算、引用字段。
