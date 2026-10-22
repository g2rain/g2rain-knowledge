# 知识库业务流程

本页定义知识库的领域用例和状态边界。基础表 CRUD 由生成器提供；本页中的流程由 Biz 层的专用应用服务实现，不能由通用 Controller 直接拼接。

主题、标签、任务、审核并发、检索可回答与反馈的详细约束见 [知识库领域规则](domain-rules.md)。

## 版本状态机

```text
DRAFT ─提交审核→ REVIEWING
REVIEWING ─审核拒绝→ DRAFT
REVIEWING ─审核通过→ APPROVED
APPROVED ─发起处理→ PROCESSING
PROCESSING ─成功→ READY_TO_PUBLISH
PROCESSING ─失败→ PROCESS_FAILED
READY_TO_PUBLISH ─发布→ PUBLISHED
PUBLISHED ─被新版本替换或下线→ DEPRECATED
DEPRECATED ─回滚恢复→ PUBLISHED
```

- `DRAFT` 可以编辑文件、FAQ、标签和描述；任何内容变更创建或更新草稿 revision，不直接修改已发布 revision。
- 一个知识主题同时最多保留一个 `DRAFT`；MySQL 函数唯一索引负责兜底，编辑冲突由 `version` 乐观锁处理。
- 审核**只针对**原始文件或 FAQ 与标签，不审解析/切片/向量中间产物；通过后进入 `APPROVED`。审核通过才允许创建 `PROCESS` 任务；该任务完成解析、切片、Embedding 与 Milvus 写入。审核拒绝回到 `DRAFT`。正式说明见 [DEV-001](deviations.md)。
- `READY_TO_PUBLISH` 的前置条件是审核通过、`PROCESS` 成功，且解析产物、chunk 清单、Embedding 和 Milvus Entity 均已自动对账完成，`index_status = INDEXED`。
- 同一 `knowledge_item` 同时只有 MySQL `knowledge_release.active_revision_id` 指向的一个 revision 对生产检索有效；旧版本保留以审计和回滚。
- 审核、发布、回滚、下线和删除均为明确业务命令，必须记录操作者、时间、原因和 trace。

## 创建与处理

```text
创建 knowledge_item
  → 创建 DRAFT revision
  → 受控保存文件或 FAQ 内容
  → 运营确认原文与主题标签/优先级并审核
  → 创建 PROCESS 任务
  → Docling + HybridChunker + Embedding
  → 创建 chunk 清单
  → Milvus staged Entity (searchable=false)
  → 校验完成，进入 READY_TO_PUBLISH
```

1. 创建知识主题时，服务以 `organ_id + knowledge_key` 确保稳定业务身份；ID 由 `IdGenerator` 生成。
2. 文件上传先执行大小、类型、MIME、内容嗅探、文件名净化与 SHA-256 去重检查。相同文件不自动替代已发布版本，运营人员必须选择复用草稿或创建新 revision。
3. 文档解析和索引处理在事务外由 `knowledge_task` 驱动；任务使用 `organ_id + idempotency_key` 防止重复执行。发布、回滚和下线不创建任务。
4. 任务只写入净化后的错误码和摘要，不保存文件原文、模型请求正文、认证头或 Secret。
5. 提交审核的前置条件是来源文件或 FAQ 内容已就绪，且标签与来源可审；审核发生在解析和切片之前。审核通过后才允许创建 `PROCESS` 任务，由该任务调用 Docling、Embedding 并写入 Milvus。
6. 索引阶段将 Milvus Entity 以 `searchable=false` 写入；在 MySQL 与 Milvus 的 Entity 数量、`chunk_id`（=`knowledge_chunk.id`）、revision ID、标签和 embedding profile 对账通过前，不得进入 `READY_TO_PUBLISH`，更不得发布。

## 审核、人工发布与回滚

```text
运营审核 REVIEWING
  → Biz 校验原始来源与标签
  → 审核通过后执行解析、Embedding 与索引
  → 索引校验完成后进入 READY_TO_PUBLISH
  → 运营人工发布：先启用目标 Milvus Entity
  → MySQL 事务：CAS 切换 active_revision_id
  → 事务提交后同步关闭旧 Milvus Entity
  → 检索侧 MySQL 再校验兜底
```

首期仅支持运营人员人工发布，不支持定时发布或自动到期下线，也不为发布、回滚或下线创建 `knowledge_task`。

人工发布按以下顺序执行：

1. 校验目标 revision 为 `READY_TO_PUBLISH`、`index_status = INDEXED`，且 chunk 清单和 Milvus Entity 对账完整。
2. 校验请求携带的 `knowledge_release.version`，并在 MySQL 事务外将目标 revision Entity 设为 `searchable=true`；Milvus 更新失败则不进入发布事务。
3. 在 MySQL 事务内以预期 `version` 做 CAS，切换 `active_revision_id`，将目标 revision 标记为 `PUBLISHED`，将被替代版本标记为 `DEPRECATED`。
4. 同步将旧 revision Entity 设为 `searchable=false`；检索始终以 MySQL 当前活动版本复核。

回滚使用独立接口，目标必须是同主题、已完成索引校验且处于 `DEPRECATED` 的历史 revision；它复用相同的 Milvus 可见性与 release/revision CAS 切换。回滚和下线都不创建任务。

## 客服检索

```text
Agent Runtime 请求
  → PrincipalContext 取得 organ_id
  → 校验 query、top_k、all_tags / any_tags
  → Embedding query
  → Milvus 召回：organ_id + searchable=true + tags
  → MySQL 批量复核 chunk 与 active_revision_id
  → Rerank 有效候选
  → 返回证据、引用与 answerable
```

- `organ_id` 只能来自可信上下文；标签是可选的业务筛选条件，不能作为数据隔离或授权依据。
- Milvus 只负责候选召回。服务必须批量复核候选的 `organ_id`、`knowledge_chunk.delete_flag` 和 `knowledge_release.active_revision_id`；无效候选丢弃并在有限次数内补召回。
- Rerank 仅接收已经通过组织和活动版本校验的有限候选集。
- 无足够证据或候选均失效时，返回 `answerable=false` 与 `NO_EVIDENCE`。必要下游依赖不可用或超出检索时间预算时返回依赖不可用错误，不能伪装成“无知识”；Agent Runtime 不得据此编造答案。

## 反馈、下线与回收

- Agent 或运营端可以对引用 revision/chunk 记录低质量、人工接管、错误答案和人工修正反馈；反馈不保存完整对话。
- 运营人员根据反馈创建新草稿 revision 或 FAQ，原 revision 不被就地篡改。
- 下线先在 MySQL 撤销活动版本，再同步关闭 Milvus Entity；任一索引异常都由 MySQL 活动版本复核兜底。
- 首期不自动物理回收原文件和解析产物，以保证历史 revision 可审计、可重建和可回滚。Milvus 派生索引可通过 `DELETE_INDEX` 任务受控回收。
