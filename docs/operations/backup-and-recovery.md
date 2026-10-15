# 备份、恢复与索引重建

## 恢复原则

MySQL 的知识元数据和受控本地文件是恢复权威；Milvus 是可重建索引。备份作业必须为 MySQL 备份和文件卷快照生成同一个外部备份批次标识，并保存文件清单、文件 SHA-256、知识 revision 数和 chunk 数。Milvus 数据目录可以通过停写窗口或经验证的一致性快照备份以缩短恢复时间；普通文件复制不视为有效备份，且 Milvus 始终不作为发布状态来源。

上线前由运行团队定义 RPO、RTO、备份频率、保留周期、加密和异地副本策略；未定义并演练这些目标不得宣称具备灾备能力。

## 恢复顺序

1. 恢复 MySQL，并校验 `knowledge_release`、`knowledge_revision`、`knowledge_chunk`、任务和审计记录的组织一致性。
2. 恢复本地文件卷，依据备份文件清单和 `source_sha256` 校验活动 revision 的原文件与解析产物。
3. 缺失或损坏的原文件/解析产物保持对应 revision 不可发布，生成运营告警；不得以 Milvus Entity 中的文本反向构造原始知识。
4. 恢复 Milvus 数据目录，或按 MySQL 当前活动 revision、chunk 清单和解析产物在新 collection 中重建索引。
5. 对每个活动 revision 校验 Entity 数、`chunk_id`、`revision_id`、`organ_id`、tags、embedding profile 和 `searchable` 状态。
6. 恢复 `PENDING` 和租约超时的 `RUNNING` 解析/索引任务；发布、回滚和下线没有待恢复的异步任务。
7. 对每个发布记录执行索引对账，以 MySQL `active_revision_id` 修正 Milvus `searchable`。
8. 使用跨组织拒绝、当前版本检索、无证据和引用定位测试验证恢复结果后，才恢复服务流量。

## 索引重建与切换

- 模型版本、输出维度、归一化方式或 Entity Schema 变化时创建新 embedding profile 与新 collection。
- 重建只处理 MySQL 当前有效且文件/解析产物完整的 revision；旧 collection 保留至新 collection 完成计数、抽样召回、组织过滤和引用校验。
- 服务通过内部配置切换 collection；切换前后记录 profile、collection、Entity 数、开始/结束时间和验证结果。
- 重建失败时保留现有可用 collection，修复后从失败批次继续。禁止用空 collection 替换线上 collection。

## 演练验收

- 恢复到隔离环境后，活动 revision、文件校验和、chunk 数和 release 审计可对账。
- Milvus 重建后，旧 revision 和跨组织 Entity 不会作为最终证据返回。
- 解析/索引任务的租约到期和重复投递可安全恢复；人工发布后的 Milvus 派生状态可通过索引对账恢复。
- 演练记录实际恢复时长、数据缺失、错误处理和未满足的 RPO/RTO 目标。
