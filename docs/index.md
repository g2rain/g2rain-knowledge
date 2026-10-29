# g2rain-knowledge 工程文档

本仓库已实现 API/Biz/Startup 三模块工程、九张表的生成 CRUD、知识生命周期、处理、检索和受控问答，以及 RQ-005 / RQ-006 / RQ-008（共享范围与公共知识）后端与配套 app 主路径。存量环境升级须按文档完成 MySQL 列与 Milvus `visibility` 原地加字段后再发版。

## 入口

- [项目事实与状态](project.yaml)
- [Agent 工作约束](../AGENTS.md)
- [需求入口](requirements/README.md)
- [公共知识共享升级方案（RQ-008，已实现）](requirements/RQ-008-public-knowledge-sharing.md)

## 架构

- [总体架构与职责](architecture/overview.md)
- [模块与依赖边界](architecture/modules.md)
- [G2rain 公共基线与代码生成约束](architecture/g2rain-foundation.md)
- [知识库业务流程](architecture/business-flows.md)
- [知识库领域规则](architecture/domain-rules.md)
- [架构例外](architecture/deviations.md)
- [知识生命周期与数据模型](architecture/knowledge-lifecycle.md)
- [MySQL 数据模型](architecture/mysql-schema.md)
- [Milvus 向量索引设计](architecture/milvus-design.md)
- [检索与发布流程](architecture/runtime-flows.md)

## 契约与治理

- [接口与集成契约](api/integration-contracts.md)
- [Docling Serve HybridChunker 契约（v1.29）](api/docling-serve-chunk-v1.29.md)
- [安全边界](security/security-boundaries.md)
- [架构决策记录](decisions/README.md)
- [社区与贡献](community.md)

## 交付与运行

- [代码分析与开发执行计划（具体类、方法及验收）](development/implementation-plan.md)
- [本地开发准备](development/local-development.md)
- [测试策略](development/testing.md)
- [完成定义](development/definition-of-done.md)
- [配置](operations/configuration.md)
- [部署与运行要求](operations/deployment.md)
- [Milvus visibility 原地加字段（RQ-008）](operations/milvus-visibility-inplace.md)
- [排障与恢复](operations/troubleshooting.md)
- [备份、恢复与索引重建](operations/backup-and-recovery.md)


## 发布

- [发布记录与流程](releases/README.md)
- [v1.0.0：配套、验证与已知限制](releases/v1.0.0.md)
