# 架构例外

> 发布补充（2026-10-19）：维护者确认服务器端验证通过，详见 [v1.0.0 发布记录](../releases/v1.0.0.md)。该验证不自动关闭下列代码与可靠性延期项；历史核对日期保留。

## 当前状态

工程已具备 `g2rain-knowledge-api` / `g2rain-knowledge-biz` / `g2rain-knowledge-startup` 三模块与生成 CRUD 骨架；`java-domain-service 1.0.0` **尚无**已批准的中央 Profile 例外（跨模块同步写入、绕过 `organ_id`、直接对外暴露通用 CRUD 写契约、混合 embedding profile、将外部网络调用放入发布事务等仍须先登记再实施）。

下列条目为本项目**已批准的领域规则/实现基线调整**（相对早期设计草案），不是中央 Profile 例外。

## DEV-001：审核仅针对源文件，处理在审核之后

| 项 | 内容 |
| --- | --- |
| 状态 | 已批准（2026-10-07） |
| 变更 | 权威顺序由早期草案 `DRAFT → PARSING → REVIEWING → INDEXING → …` 调整为 `DRAFT → REVIEWING → APPROVED → PROCESSING(PROCESS) → READY_TO_PUBLISH → PUBLISHED` |
| 原因 | 人工审核应校验可阅读的原始文件或 FAQ 与标签；切片与向量中间产物审核人无法有效接入 |
| 后果 | 审核通过前不得 Embedding/写 Milvus；通过后由 `PROCESS` 完成解析/切片/索引 |
| 残留差距 | `scripts/g2rain-knowledge.sql` 中 `revision_status` COMMENT 仍列举旧状态词；运行时以 Java 枚举为准 |

## DEV-002：检索未知标签拒绝

| 项 | 内容 |
| --- | --- |
| 状态 | 已批准（2026-10-07） |
| 变更 | 检索请求中未知/不可用 `tag_code` **拒绝请求**，不按「无匹配 → NO_EVIDENCE」处理 |
| 原因 | 与当前实现及 [integration-contracts](../api/integration-contracts.md) 一致，避免调用方拼写错误被静默当成无知识 |
| 残留 | 早期草案「未知→无匹配」已废弃 |

## DEV-003：访问模型 = Gateway 准入 + Starter 租户隔离

| 项 | 内容 |
| --- | --- |
| 状态 | 已批准（2026-10-07） |
| 变更 | 凡经 Gateway 可达本服务的调用方可访问本租户全部知识接口；隔离仅用 Starter `@DataIsolation` / `organ_id` |
| 不做 | 服务内五能力点门禁、第二套数据隔离；业务路径默认不使用 `WithoutIsolation` |
| 说明 | 细粒度 IAM 若需要由 Gateway/管理端入口控制。公共知识使用端只读例外见 DEV-004 |

## DEV-004：公共知识使用端只读允许专用 WithoutIsolation（RQ-008）

| 项 | 内容 |
| --- | --- |
| 状态 | 已批准方案口径（2026-10-27）；功能尚未实现 |
| 变更 | 使用端只读路径可在**专用** Domain DAO/领域方法内使用 `WithoutIsolation`：（1）检索/问答/引用/详情/正文对已知 ID 集合只读后裁决；（2）公共列表与分类导航分页，SQL 固定 `visibility=PUBLIC` + 活动已发布/未删除等谓词，list 与 count 同一条件 |
| 原因 | 公共浏览与检索必须读他机构元数据；本期以业务侧专用 WithoutIsolation 落地，不阻塞于 Starter 通用共享读 |
| 允许 | 专用 Domain DAO；已知 ID 批量只读；公共目录固定谓词分页；用记录所属 organ 定位解析产物 |
| 禁止 | 管理 CRUD、生成 CRUD 无隔离分页、无固定 PUBLIC 谓词的扫库、Service 散落调用、客户端 organId 授权、写路径、跨机构原文件下载、草稿/任务/处理产物借共享属性开放；本机构列表不得改走公共无隔离入口 |
| 相关 | [RQ-008](../requirements/RQ-008-public-knowledge-sharing.md)；页面「本机构 / 公共」分开展示；共享范围变更专用命令同步标量；Milvus 主路径为现有 collection **原地**加 `visibility`；内容/业务标签仍走新版本 PROCESS 发布；资格取消后允许专用撤销公开/下线且不自动撤回 |

## 已登记延期缺口（非 Profile 例外）

- 发布：事务内先 `setSearchable` 再 MySQL CAS；无 offline/reconcile；无主题分布式锁。通用删除仅允许未发布且无活动版本的 release，已发布记录不得借删除绕过下线。
- 任务：`input_snapshot` / `lease_generation` / `cancel_requested` 列未接线；RUNNING 协作取消未实现。
- 运营：主题/草稿/FAQ 经生成 `save` 已补受信组织、默认值与草稿白名单/`updateByVersion`（RQ-005）；`replaceFile` 与 PENDING retry/cancel 已暴露专用 HTTP。任务/切片/反馈 `/save` 仅补组织与 `@LoginGuard`，字段白名单未收紧，仍为已知残余风险。
- 原文下载：`GET /knowledge_revision/{id}/source/download` 已按 RQ-006 提供附件流（审核不强制已下载）。

## 已关闭：字符切分 vs HybridChunker

| 项 | 内容 |
| --- | --- |
| 状态 | 已关闭（实现对齐架构） |
| 曾有偏差 | 架构写 Docling + HybridChunker，PROCESS 实际为 convert Markdown 后每 1200 字符切分，`char:start:end` 定位 |
| 现实现 | FILE（PDF/DOCX/DOC/PPTX/PPT/XLSX/XLS/MD/TXT/HTML）调用 Serve `/v1/chunk/hybrid/file`；FAQ 由 `KnowledgeChunkPolicy.chunkFaq`；产物为 `document.md` + `chunks.json`，locator 为 `chunk:{n}`；区分 rawText / embeddingText。遗留 `.doc`/`.ppt`/`.xls` 依赖 Serve 主机 LibreOffice 能力 |

## 本地实现选择（非例外）

- 基于 MySQL 函数索引的逻辑删除策略遵循 Profile 的 MySQL 8.0.13+ 基线。
- Milvus、Docling 和模型服务是本领域的外部基础设施；发布权威仍在 MySQL。
- 代码生成器生成基础 CRUD，领域流程在 Biz 层扩展。
- 本服务提供受控问答 `POST /knowledge/answer`（模板 + 可信证据 → chat completions）；不持有 Agent Runtime 会话。
