# 需求入口

## 当前实现基线

- [RQ-005：页面接入所需的保存接口升级方案（后端已实现）](RQ-005-knowledge-save-contract-upgrade.md)
- [RQ-006：原文件受控下载方案（后端已实现）](RQ-006-knowledge-source-download.md)
- [RQ-008：基于共享范围的公共知识升级方案（已实现）](RQ-008-public-knowledge-sharing.md)

## 需求清单

- [RQ-008：基于共享范围的公共知识升级方案（已实现）](RQ-008-public-knowledge-sharing.md)
- [RQ-007：知识内容按业务团队管理的权限升级方案（方案草案）](RQ-007-knowledge-management-scope.md)
- [RQ-003：知识库领域规则与管理接口](RQ-003-knowledge-domain-rules.md)
- [RQ-001：知识库 MySQL 数据结构](RQ-001-knowledge-mysql-schema.md)
- [RQ-002：知识库服务架构与业务流程](RQ-002-knowledge-service-architecture.md)
- [RQ-004：知识检索契约、任务可靠性与恢复](RQ-004-knowledge-reliability-and-contract.md)

开始实现前，每个需求应记录范围、非目标、接口/数据/安全影响、验收标准、迁移与回滚方案、依赖和验证命令。选定唯一活动需求后，在 `docs/project.yaml` 的 `documentation.activeRequirement` 中引用它。
