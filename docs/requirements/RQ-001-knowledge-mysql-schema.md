# RQ-001：知识库 MySQL 数据结构

## 范围

定义 `g2rain-knowledge` 首版 MySQL 表结构，覆盖知识分类、标签字典、知识主题、版本、切片清单、异步任务、发布选择、回复模板和质量反馈。

DDL 位于 [`scripts/g2rain-knowledge.sql`](../../scripts/g2rain-knowledge.sql)。它与 `g2rain-department` 等同类服务一致：创建独立库 `g2rain_knowledge`，仅用于首次初始化；会删除并重建本库知识表，不写入示例数据，禁止在已有业务数据环境重复执行。

## G2rain 数据规范映射

- `id` 为服务生成的 `BIGINT`，不使用 `AUTO_INCREMENT`。
- `organ_id` 是每张租户数据表的隔离键；业务代码必须从认证上下文取得，不接受客户端伪造值。
- 所有主业务表带有 `create_time`、`update_time`、`version`、`delete_flag`。
- 采用小写下划线表名和字段名、`utf8mb4_0900_ai_ci`、InnoDB。
- 不建立数据库外键；跨表一致性由应用服务事务控制，并通过复合索引保证查询路径。
- 含逻辑删除条件的索引使用 MySQL 函数键 `(IF(delete_flag = 0, 0, NULL))`，不将 `delete_flag` 作为普通索引列；因此要求 MySQL 8.0.13+。
- 文件内容、解析正文和向量不进入 MySQL；只保存受控相对路径、内容哈希和索引定位信息。
- `knowledge_category` 为首期扁平运营分类字典，不使用 `parent_id` 树结构；`knowledge_tag` 以 `tag_code` 为组织内唯一身份，`tag_name` 仅展示；主题已选标签以 `tag_code` 数组保存在 `knowledge_item.tags_json`（已从 revision 迁出），首版不建立标签关联表。
- `knowledge_revision.embedding_profile` 记录向量配置键；`knowledge_item.priority` 仅用于运营排序，不作为 Milvus 过滤字段。
- `knowledge_chunk.id` 直接作为 Milvus Entity 的 `chunk_id` 主键，不维护冗余的向量记录 ID。
- 所有 `knowledge_*` 表必须可被 `g2rain-generator-maven-plugin` 识别并生成基础 CRUD；`organ_id` 与 `delete_flag` 分别遵循生成器的隔离与逻辑删除规则。

## 非目标

- 不定义 Java 实体、Mapper、迁移工具或初始化容器。
- 不将 Milvus collection、Embedding/Rerank 密钥或 Docling 请求内容写入数据库。
- 不替代 G2rain Basis 的用户、机构、审计事件和权限资源表。

## 数据与安全影响

- 发布状态唯一以 `knowledge_release.active_revision_id` 为准；Milvus 不能作为发布真相来源。
- 标签不承担数据隔离或权限职责；检索必须固定校验 `organ_id` 与发布状态。`PROCESS` 任务将版本 `tag_code` 数组复制到 Milvus Entity 的 `tags` ARRAY 字段。
- 每次业务读写必须条件化 `organ_id` 与 `delete_flag = 0`。
- `knowledge_task.error_summary`、`knowledge_feedback.feedback_summary` 不得写入原文、认证头或密钥。
- `corrected_answer` 仅保存经人工提交的修正内容，并按知识运营权限访问。

## 验收标准

1. MySQL 8.0.13+ 可在空库或可丢弃环境执行本初始化脚本，创建 `g2rain_knowledge` 并建齐全部知识表；不得影响其他 G2rain 业务库。
2. 每张表使用 InnoDB、`utf8mb4_0900_ai_ci` 与 G2rain 通用审计列。
3. 发布查询、版本查询、标签管理、任务查询和反馈查询均有对应的复合索引。
4. DDL 与架构文档中的 MySQL 权威状态、Milvus 派生索引边界一致。
5. 在开发数据库执行 DDL 后，代码生成器能够以 `organ_id` 隔离、`delete_flag` 逻辑删除和 `tables.overwrite=false` 生成基础 CRUD；生成产物经人工审查后再进入领域实现。

## 迁移与回滚

- 首次环境用本初始化脚本建库建表；不得对已有知识业务数据环境重复执行（脚本含 `DROP TABLE`）。
- 生产结构变更须另增增量迁移脚本，与初始化脚本分离；生产数据存在后禁止用 `DROP TABLE` 回滚。
- 删除知识使用 `delete_flag` 与受控文件回收任务；Milvus 删除为可重试的派生动作。
