# RQ-003：知识库领域规则与管理接口

## 状态

当前实施需求。当前工程已落地部分领域主路径；其余能力与验收证据仍按[开发执行计划](../development/implementation-plan.md)推进，不应因已有代码而视为全部完成。

## 范围

落实知识主题、版本、标签、任务、审核发布、检索可回答判定与反馈闭环的领域规则。权威规则见 [`domain-rules.md`](../architecture/domain-rules.md)；见 [`deviations.md`](../architecture/deviations.md) DEV-001/002/003。

## 非目标

- 不在本服务内实现细粒度 IAM 能力点门禁（访问 = Gateway 可达 + Starter 租户隔离）。
- 不把生成 CRUD 当作审核、发布、检索或任务处理的唯一语义来源；审核/process/publish 使用专用命令，主题/草稿元数据可走 `save`。
- 不自动依据反馈直接修改已发布知识。
- 不以解析产物、chunk 清单或向量 Entity 作为人工审核对象。

## 验收标准

1. 领域命令校验 revision 状态、`organ_id` 与必要的乐观锁/幂等条件（无服务内能力点）。
2. 已发布 revision 不可直接修改；内容或标签修改可追溯为新 revision。
3. 审核仅针对源文件/FAQ 与标签；审核通过前不得 Embedding/写 Milvus；通过后才允许 `PROCESS`。
4. 检索只返回通过组织、MySQL 活动版本与引用完整性复核的证据；未知 tag 拒绝。
5. 标签、反馈和管理接口不扩大数据隔离边界（仅 Starter 隔离）。
