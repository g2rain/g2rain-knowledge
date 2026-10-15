# g2rain-knowledge 工程文档

本仓库已实现 API/Biz/Startup 三模块工程、九张表的生成 CRUD，以及知识生命周期、处理、检索和受控问答的主要代码路径。`mvn clean verify` 已在 2026-10-08 通过（Biz 19 个单元测试）；真实 MySQL/Milvus、外部模型与部署环境的集成验证仍未完成。代码盘点、剩余整改项和验收顺序见开发执行计划。

## 入口

- [项目事实与状态](project.yaml)
- [Agent 工作约束](../AGENTS.md)
- [需求入口](requirements/README.md)

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
- [安全边界](security/security-boundaries.md)
- [架构决策记录](decisions/README.md)

## 交付与运行

- [代码分析与开发执行计划（具体类、方法及验收）](development/implementation-plan.md)
- [本地开发准备](development/local-development.md)
- [测试策略](development/testing.md)
- [完成定义](development/definition-of-done.md)
- [配置](operations/configuration.md)
- [部署与运行要求](operations/deployment.md)
- [排障与恢复](operations/troubleshooting.md)
- [备份、恢复与索引重建](operations/backup-and-recovery.md)
