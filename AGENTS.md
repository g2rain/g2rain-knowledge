# AGENTS.md

`g2rain-knowledge` 是 G2rain 知识检索与受控问答服务。已具备 `g2rain-knowledge-api` → `g2rain-knowledge-biz` → `g2rain-knowledge-startup` 三模块工程与生成 CRUD 骨架。开始修改前阅读 [docs/project.yaml](docs/project.yaml)、[docs/architecture/deviations.md](docs/architecture/deviations.md)，以及任务相关的架构、安全、API、运维与需求文档。

中央基线为 `java-domain-service 1.0.0`（见 project.yaml 的 architectureBaseline）。模块依赖方向为 `startup → biz → api`；偏离须先登记 deviations。

## Boundaries

- 本服务拥有知识接入、版本、审核、发布、检索编排、引用，以及基于可信证据的受控问答（`POST /knowledge/answer`）。
- MySQL 是元数据与发布状态权威；Milvus 是可重建投影。
- 不得向客户端暴露存储路径、Docling、Milvus collection 或模型供应商原始接口。
- 租户隔离统一依赖 Starter `@DataIsolation` / `organ_id`；入口鉴权假定由 Gateway 完成。凡经 Gateway 可达的调用方可访问本租户数据；不在本服务内实现细粒度能力点门禁或第二套隔离。
- 检索须带租户过滤，且仅使用 MySQL 裁决的活动 revision。
- 源文件、解析正文、Secret、Token 与租户数据不得进入 Git、日志、异常或测试快照。
- Java 实现遵循 [G2rain 公共基线与代码生成约束](docs/architecture/g2rain-foundation.md)：复用 common 契约，用生成器生成 knowledge_* CRUD。

## Documentation status

工程与领域主路径已部分实现；部署清单、真实外部依赖集成与部分可靠性能力仍见 [docs/development/implementation-plan.md](docs/development/implementation-plan.md) §10 与 [docs/architecture/deviations.md](docs/architecture/deviations.md)。变更前确认 `docs/project.yaml` 的 `activeRequirement`。
