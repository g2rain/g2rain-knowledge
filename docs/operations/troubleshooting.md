# 排障与恢复

| 症状 | 首先检查 | 安全处理 |
| --- | --- | --- |
| 文档一直处于解析失败 | 文件校验结果、Docling 可用性、任务错误分类 | 不输出原文件或完整解析内容 |
| 新版本无法检索 | MySQL 活动 revision、Milvus 索引完成状态、机构与标签筛选条件 | 不临时移除 revision 或 `organ_id` 过滤 |
| 发布后结果仍是旧版本 | `knowledge_release.active_revision_id`、目标 revision 索引状态与 Milvus `searchable` | 以 MySQL 当前活动 revision 执行同步的索引对账；不绕过 MySQL 复核 |
| 人工发布冲突 | 请求携带的预期 `knowledge_release.version` 与当前版本 | 刷新发布状态后由运营人员重新确认；不覆盖新版本 |
| 出现跨租户证据 | 认证上下文、MySQL 查询与 Milvus filter | 停止受影响检索、保留审计线索、优先修复隔离 |
| 模型依赖超时 | 超时、重试、熔断与供应商状态 | 不记录完整查询或候选原文 |
| 恢复后索引不一致 | MySQL release、文件校验和、Milvus Entity 字段与数量 | 以 MySQL 和文件存储为依据重建索引 |

恢复顺序：恢复 MySQL 元数据与 release 状态、恢复文件卷、校验文件完整性，再按活动 revision 重建或校验 Milvus 索引。禁止以 Milvus 单独恢复发布真相。

详细恢复流程见 [备份、恢复与索引重建](backup-and-recovery.md)。
