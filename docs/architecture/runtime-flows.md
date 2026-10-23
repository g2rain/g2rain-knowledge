# 检索与发布流程

本页与 [知识库业务流程](business-flows.md)、[Milvus 向量索引设计](milvus-design.md) 保持同一路径；细节以那两页为准。

## 检索

1. 调用方携带已认证的机构上下文和可选标签条件请求检索。
2. 服务从认证上下文取得 `organ_id`，拒绝客户端伪造机构上下文；标签不参与权限或数据隔离判断。
3. 服务对查询做 Embedding，并向 Milvus 发起向量召回；过滤条件固定为 `organ_id`、`searchable = true` 与已校验的 `tag_code` 条件。不得由调用方指定 collection 或 revision，也**不**在生产查询中常态化携带大量 `revision_id IN (...)`。
4. 服务批量校验 Milvus 候选仍映射到 MySQL 中同一机构、未删除、且 `revision_id` 等于对应 `knowledge_release.active_revision_id` 的 `knowledge_chunk`，丢弃失效候选；不足时可在预算内分页补召回。
5. 服务可调用 Rerank API 对有效候选重排，并生成稳定的来源引用。同主题多证据时可用 MySQL 中的 `priority` 做稳定并列排序，但不作为 Milvus 过滤条件。
6. 证据不足时返回 `answerable: false` 与 `NO_EVIDENCE`；必要依赖超时或不可用时返回依赖错误。Agent Runtime 不得据此编造答案。

## 人工发布与回滚

1. 运营先审核原始文件或 FAQ 与主题侧标签/优先级。审核通过后 revision 进入 `APPROVED`；此时才允许发起 `PROCESS` 任务。
2. `PROCESS` 任务完成 Docling、切片、Embedding 与 Milvus staged 写入。它将版本 `tag_code` 数组同步到 Entity 的 `tags` ARRAY 字段，并以 `searchable = false` 写入；全字段对账通过后 revision 进入 `READY_TO_PUBLISH`（`index_status = INDEXED`）。
3. 人工发布命令校验目标 revision、发布记录和二者的预期 `version`，先将目标 Entity 设为 `searchable = true` 并确认写入结果。
4. 服务使用已有 release/revision 的乐观锁更新，在一个 MySQL 事务内切换 `active_revision_id`，将目标置为 `PUBLISHED`，并将被替换版本置为 `DEPRECATED`。
5. 服务同步将旧 revision Entity 设为 `searchable = false`。检索侧的 MySQL 活动版本复核始终排除非活动 revision。
6. 回滚使用独立动作，目标必须是同主题、已完成索引校验且处于 `DEPRECATED` 的历史 revision；其切换步骤与发布相同。

首期不提供定时发布、自动到期下线或任何发布任务。发布不依赖 Milvus 作为事务协调者；索引失败、模型调用失败或状态不完整均不得触发活动版本切换。
