# RQ-008：基于共享范围的公共知识升级方案

状态：待实施方案。更新日期：2026-10-27。

本方案替代此前的系统公共标签设计。使用 `visibility` 控制共享范围，分类用于目录组织，标签用于业务筛选；`SYSTEM_PUBLIC` / `system_public` 均不再作为公共权限标识。本文只更新方案，不表示功能已实现，不改变当前活动需求。复用已完成的 HybridChunker 链路。

## 0. 已冻结决策（实施按此执行）

| 决策 | 口径 |
| --- | --- |
| 共享标识 | `knowledge_item.visibility`：`PRIVATE` / `PUBLIC`，默认 `PRIVATE` |
| 内容与业务标签 | 已发布不就地改；须新版本 → 审核 → PROCESS → 发布 |
| 共享范围变更 | 专用命令可就地改 `visibility`，并同步 Milvus 标量（零 Docling/embedding） |
| 使用端页面 | 「本机构知识」与「公共知识」分路由、分 API、不混排 |
| 公共列表隔离 | 专用 Domain DAO + `WithoutIsolation`；SQL 写死 PUBLIC + 活动已发布等谓词 |
| 检索复核 | 已知 ID 集合 `WithoutIsolation` 读后业务裁决（DEV-004） |
| 业务标签过滤 | `includePublic=true` 时，对本机构与跨机构公共候选同等生效 |
| 来源标识（首期） | 直接暴露 `organId` / `categoryId` / `knowledgeId` 等业务 ID；不引入 `sourceKey`；后续再脱敏 |
| Milvus schema | **主路径：现有 collection 原地新增 `visibility` 标量**；存量回填后与 MySQL 对账 |
| 资格取消 | **允许**归属机构专用撤销公开或下线（只改可见性）；**不**因资格变化自动撤回已有公共内容 |

## 1. 目标与边界

支持其他机构浏览公共知识列表、查看详情、阅读当前已发布正文、使用引用，以及按需检索和问答。公共知识保持真实所属机构，只存储一份；公开不转移维护权。

不新增独立知识库集合实体；`knowledge_item` 是共享范围设置根。不支持匿名访问、指定机构共享、订阅、有效期或跨机构代管。首期不开放跨机构原文件下载。

## 2. 产品规则摘要

| 项目 | 规则 |
| --- | --- |
| 设置层级 | 主题设置；revision/chunk 继承，不独立修改 |
| 维护资格 | 发布/更新公共知识要求可信 `adminCompany=true`，且知识属于当前机构 |
| 公共阅读 | 已获准调用服务的机构可浏览、查看及阅读公共活动版本 |
| 本机构列表 | 含本机构私有主题 **与** 本机构 `visibility=PUBLIC` 的主题 |
| 公共列表 | 全平台 `visibility=PUBLIC` 的活动已发布版本 |
| 检索 | `retrieve` / `answer` 增加 `includePublic`，默认 / 缺省 / null 均为 false |
| `includePublic=false` | 仅本机构（含本机构 PUBLIC） |
| `includePublic=true` | 本机构 + 跨机构 PUBLIC，统一召回排序 |
| 可用版本 | 当前活动、已发布、已索引；主题及关联记录有效、未删除 |
| 分类、标签 | 不决定可见性；公共知识仍可有分类与业务标签 |

## 3. 数据与接口设计

### 3.1 MySQL 权威字段

`knowledge_item` 新增 `visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE'`，应用枚举仅接受 `PRIVATE`、`PUBLIC`；迁移按项目 MySQL 基线生成。现有数据填 `PRIVATE`，不根据分类或历史标签推断公开。

`organ_id` 保留真实所属机构。`visibility` 为主题当前权威策略，不复制为 revision/chunk 独立可写字段。版本回滚只切换内容，不自动恢复历史共享范围；主题当前为 `PUBLIC` 时，回滚仍须具备公共发布资格。

公开属性经独立命令修改：请求建议含 `knowledgeId`、`visibility`、`expectedVersion`，使用乐观锁。不得在普通标签更新或内容保存中隐式改变共享范围。创建主题可指定目标范围（默认 `PRIVATE`）；指定 `PUBLIC` 时校验资格，但未发布主题仍不可供他机构使用。

建议评估索引：`(visibility, delete_flag)` 及与活动 release 联查相关组合，以真实 EXPLAIN 定稿。

### 3.2 Milvus 检索投影

每个 chunk Entity 新增 `visibility` 标量，取值与所属主题当前范围一致，用于召回前过滤。MySQL 为最终权威，不能仅信任向量侧字段。

**Schema 升级主路径（已确认）：** 在现有 collection **原地**增加 `visibility` 字段；同步调整部署定义、启动 schema 校验、`MilvusIndexEntity`、写入/读取/整行复制、对账与 `MilvusFilterBuilder`。标量仅允许 `PRIVATE`/`PUBLIC`，不混用 tags 表达权限。

存量 Entity：加字段后回填默认 `PRIVATE`，再按 MySQL 活动版本对账写回真实 `visibility`；对账完成前不开放跨机构公共召回。

仅改变 `visibility` 时：不调用 Docling、不重新切片、不调用 embedding；复用已存向量更新标量。若 SDK/部署需整行 upsert，则完整保留向量及其他字段，**不得**误入重新计算 embedding 的 INDEX/PROCESS 路径。

仅当原地加字段在目标环境验证失败时，才启用应急方案：新 collection + 既有 chunk ID/已存向量重建投影（仍不重新切片/embedding），完成对账后受控切换。应急方案须单独登记，不作为默认实施路径。

### 3.3 使用端接口

| 能力 | 契约要点 |
| --- | --- |
| 公共知识分页 | 专用只读；仅 PUBLIC 活动版本；分页、关键词、安全排序；可按 `categoryId` 筛选 |
| 公共知识详情 | 按 `knowledgeId`；当前公共活动版本元数据 |
| 公共正文阅读 | 按主题定位活动版本；受控渲染正文；不返回存储路径 |
| 公共分类导航 | 仅返回存在可见公共知识的分类；不暴露完整私有分类树 |
| 检索与问答 | 增加可选 `includePublic`；入口 `Boolean.TRUE.equals(value)` |

HTTP 路径按现有 API 命名规范在实现时确定；不复用管理详情接口扩大访问。列表返回轻量字段，不批量加载完整正文。

**来源标识（首期）：** 列表、分类、详情、检索证据直接返回业务 ID（至少 `organId`、`categoryId`、`knowledgeId`；证据另含 `revisionId`/`chunkId` 等）。同名分类靠 `categoryId` + `organId` 区分。不新增 `sourceKey`。响应中的 `organId` 仅展示/区分，不是授权凭据。

公共详情/正文入口本身表示公共访问，不要求 `includePublic`；每次仍校验数据库状态。`includePublic` 只控制检索/问答是否额外召回跨机构公共知识。

## 4. 写权限与可信主体

复用 `PrincipalContextHolder.isAdminCompany()`，以 Gateway 可信上下文为准；不接受请求体或客户端自声明标记；不等同于 `adminUser`。

下列操作须校验所属机构、`adminCompany=true`、乐观锁及现有状态机：设置 `PUBLIC`、更新公共主题或内容、创建公共主题新版本、替换文件、导入更新、提交审核、处理、发布、回滚。更新前或目标状态任一为 `PUBLIC` 均须校验；不能通过「同时改为 PRIVATE 并改正文」绕过。

普通管理 CRUD、版本/切片/release 通用写入口须收紧，不能绕过专用命令。`adminCompany` 不授予跨机构写权限，不改变 RQ-007 管理查看范围。

后台 Worker 使用受控任务与持久化机构上下文；发布时对**当前调用方**重新检查资格。

**资格取消（已确认）：** 允许归属机构在失去 `adminCompany` 后仍执行专用「撤销公开」或「下线」命令，且命令**只**收回可见性，不可附带改内容。已有公共内容**不**因资格变化自动撤回。

## 5. 检索与标签筛选

```text
includePublic=false:
  searchable == true AND organ_id == 当前机构 AND 业务标签条件

includePublic=true:
  searchable == true
  AND (organ_id == 当前机构 OR visibility == "PUBLIC")
  AND 业务标签条件
```

须保留括号，使 `searchable` 与业务标签同时作用于两类候选。按 chunk ID 去重；统一相关性排序；不因同 `knowledge_key` 跨机构覆盖。

业务标签 `allTags`/`anyTags` 对两类候选相同 AND/OR。调用方标签仍按**本机构**字典校验（未知 code 拒绝）；按 `tag_code` 字符串匹配。首期不合并跨机构标签字典。无业务标签条件时不加标签限制。

MySQL 批量复核归属、关联、未删除、活动版本、发布/索引及 hash/profile。跨机构候选另须本次 `includePublic=true` 且主题当前 `visibility=PUBLIC`。

同一检索上下文贯穿召回、补召回、rerank、证据与 answer；正文进模型前完成复核；长生成前再验公共证据。缓存键覆盖调用机构、访问模式与共享策略失效。

## 6. 共享读取与机构隔离

管理 CRUD、草稿、审核、任务、切片清单、原文件下载维持机构/管理范围隔离。使用端不套用维护者部门查看范围。

**DEV-004 专用只读 `WithoutIsolation`（已批准）：**

| 路径 | 方式 | 硬性条件 |
| --- | --- | --- |
| 检索 / 问答 / 引用 / 详情 / 正文 | 已知 ID 集合只读后裁决 | 本机构 **或**（公共访问且 `visibility=PUBLIC` 活动已发布） |
| 公共列表 / 分类分页 | PublicCatalog Domain DAO | SQL 写死 PUBLIC、未删除、活动已发布/已索引、关联一致；list 与 count 同谓词 |

约束：仅上述专用 DAO/方法；禁止 Service 散落调用生成 `*WithoutIsolation`；禁止写路径；禁止生成 CRUD 无隔离分页或先拉全量再过滤。客户端可传关键词、`categoryId`、分页、安全排序；不能用自声明 organId/visibility 扩大授权。存储按记录所属 organ 定位，不用消费机构拼路径。本机构列表继续 `@DataIsolation`。

## 7. 发布、共享变更与索引一致性

| 场景 | 处理 |
| --- | --- |
| 未发布主题设 PUBLIC | 保存策略；无活动版本时他机构不可见 |
| 已发布改为 PUBLIC | MySQL 立即生效（公共阅读可见）；索引异步同步后可跨机构召回 |
| PUBLIC → PRIVATE | MySQL 立即拒绝跨机构列表/阅读/引用/复核，再修索引 |
| 公共内容更新 | 新版本完整链路；发布成功前他机构仍见旧活动版本 |
| 下线 / 删除 | 公共入口全部停止；按既有规则清 searchable |
| 回滚 | 切内容；沿用主题当前 visibility；同步**新活动 revision** 投影并做 visibility/searchable 对账 |
| 同步失败 | 持久化重试、告警、对账；MySQL 权威兜底 |

共享策略变更与同步任务可靠提交；主题 CAS；任务以最新策略与活动版本为准；主题内串行化发布与投影更新。对账同时校验 `visibility` 与 `searchable`。撤销保证后续授权检查；已返回正文无法追溯收回。

## 8. 阅读、引用、下载与反馈

跨机构只展示当前公共活动版本。历史曾公开或知晓 ID 不构成资格；猜私有/草稿/非活动 ID 不得返回正文或管理元数据。

分类仅导航；挪分类不改 visibility，不暴露私有条目或空私有目录。正文清理危险 HTML；关联图等资源须经同一公共版本授权接口，禁止可绕过校验的源存储直链。

原文件下载首期仍限机构内现有权限。反馈归提交机构；关联公共知识按当前可见性验证；不向维护方自动开放消费方问答/反馈正文。

审计记录公开/撤销/公共更新/发布/回滚；不记正文或 Secret。

## 9. 页面调整

管理端：保留分类与业务标签；新增共享范围 PRIVATE/PUBLIC；按 `adminCompany` 控制操作；已发布内容编辑引导新版本；共享范围单独提交并展示同步状态。

使用端分开展示：

| 入口 | 数据 | API |
| --- | --- | --- |
| 本机构知识 | 本机构私有 + 本机构 PUBLIC | 机构隔离列表 |
| 公共知识 | 全平台 PUBLIC 活动版本 | 公共专用只读 API；无编辑/审核/任务/原文件下载 |

禁止同表混排或 tab 共用无隔离列表接口。检索/问答可另设「包含公共知识」（默认关 → `includePublic`），与公共浏览专区分离。

## 10. 实施步骤

1. **P0**：核对 `adminCompany` 传播；定稿 DEV-004 入口清单与公共分页 SQL 谓词；部署侧**原地**加 Milvus `visibility` 并验证；明确存量回填/对账步骤。
2. **P1**：MySQL `visibility`、枚举/DTO/VO、专用范围命令、资格取消撤销命令、全写入口校验与审计。
3. **P2**：Entity/过滤器/`visibility` 标量同步（零重嵌）与对账。
4. **P3**：`includePublic` + 已知 ID 复核；公共列表/分类/详情/正文/引用及资源授权。
5. **P4**：knowledge-app 本机构/公共分开展示联调；双机构真实 MySQL/Milvus 验收。

同步：`integration-contracts`、`domain-rules`、`mysql-schema`、Milvus 部署定义、`security-boundaries`、DEV-004、运维与测试。选定实施后再改 `project.yaml` 的 `activeRequirement`。

## 11. 升级与回退

- 现有主题统一 `PRIVATE`；旧请求仅本机构检索。
- 若 `tags_json` 残留旧试验公共标签：不自动公开；运营确认后清理；标签不再有权限语义。
- Milvus：**原地加字段 → 回填/对账 → 再开放公共入口**。应急新 collection 仅在原地失败时启用。
- 上线顺序：MySQL 字段 → Milvus 字段与回填 → 支持新策略的后端 → 前端。旧写入实例若会写出无 `visibility` 的 Entity，须在切换写路径前停用。
- 回退：先关跨机构读、公开变更与同步任务，再切回已验证服务；保留库字段与审计；不得开放缺少可见性复核的公共入口。

## 12. 验收与验证

- `includePublic` 缺省/null/false 无跨机构候选；true 仅增公共活动版本；本机构 PUBLIC 在 false 下仍可命中。
- 他机构可浏览公共列表/分类/详情/正文；不可访问私有、草稿、历史版本、任务、切片、原文件。
- 公共列表仅专用 WithoutIsolation DAO；list/count 谓词一致；猜私有 ID 或走管理列表不得放行。
- 本机构页与公共页分开展示、分 API。
- 非 adminCompany 不能借各写入口绕过；adminCompany 不能跨机构改。
- 失去资格后仍可专用撤销/下线；不会自动撤回；撤销后 MySQL 权威立即拒绝跨机构访问。
- 改 visibility 零 Docling/embedding；chunk ID、内容 hash、向量不变。
- Milvus 为现有 collection 原地字段；存量回填/对账后公共召回正确。
- 并发发布/回滚/公开撤销/乱序任务后活动内容与投影一致。
- 业务标签对两类候选同等过滤；首期 VO 可含 organId/categoryId。

实施后执行 `mvn -pl g2rain-knowledge-biz -am test` 与 `mvn clean verify`，并完成至少两个机构（含 adminCompany true/false）的真实集成验证。本文档更新只检查文档差异，不声明实现或测试已完成。
