# RQ-005：页面接入所需的保存接口升级方案

状态：已实现（后端）。创建默认值、组织上下文、CreateGroup 调整、版本 VO/DTO `version` 与草稿 `updateByVersion`、save `@LoginGuard` 已落地；任务/切片/反馈字段白名单仍为残余缺口。

## 1. 背景与范围

知识 App 页面升级方案 review 的第 1 项发现：前端只应提交业务输入，但生成的保存 DTO 把 `organId` 和部分系统初始化字段声明为创建必填。当前 Service 先调用 `Validations.validateSave(dto)`，再转换并入库，既没有在校验前补齐上下文，也没有统一初始化版本状态。

本次明确调整现有后端保存接口实现，使页面不必传组织、审计字段或伪造处理状态。保留已生成的 CRUD 结构、URL 和返回契约，不新增管理型聚合 Controller，不重复生成或实现一套 CRUD。

范围：

- 所有九张表已有 `/save` 的组织上下文处理，防止信任客户端组织字段。
- 页面直接维护的分类、标签、主题、版本、发布选择和回复模板的创建默认值。
- 版本和发布选择保存时的系统字段保护，确保不能通过通用保存绕过审核、处理和发布。

不包括 review 第 2 至第 4 项的页面实现本身：前端 API 返回类型修正、原文下载、历史发布记录。原文下载独立定义于 [RQ-006](RQ-006-knowledge-source-download.md)。任务与切片不新增面向页面的创建能力，不在本次改造中扩展其业务初始化规则。反馈业务状态不在本次重设计。

已知残余风险：任务、切片、反馈的 `/save` 本次只补受信组织与 `@LoginGuard`，不向 App 暴露手工编辑界面；若 Gateway 仍可达，通用更新仍可能触及内部状态字段。收紧其字段白名单不在本次范围，实施后在 `deviations.md` 保留为已知缺口，除非另行立项。

## 2. 统一处理顺序

现有 `XxxServiceImpl.save(dto)` 保持入口，调整为以下顺序：

1. 从 `KnowledgeAccessService.requireOrganId()` 取得组织上下文；缺失时拒绝请求。
2. 区分新增与更新，沿用现有 ID 判断规则。
3. 更新先读取现有数据，检查存在性和所属组织；不能通过提交新的 `organId` 改变归属。
4. 以受信组织覆盖 DTO/持久化对象中的 `organId`；创建人与时间由服务端填充。需要 `createUserId` 时，若 `currentUserId()` 为空则拒绝请求（与 `requireOrganId` 同等严格），不得写入空创建人。
5. 新增补齐默认值、固定系统状态；更新保护不可修改字段。
6. 对规范化后的 DTO 执行 G2rain `Validations.validateSave`，并执行必要的领域校验。
7. 沿用表对应 DAO 保存，返回原有 ID。版本草稿更新必须使用 `KnowledgeRevisionDao.updateByVersion`，禁止继续用生成的 `dao.update` 更新受保护版本字段。

前端省略 `organId` 是合法请求。兼容旧客户端携带该字段，但它不参与组织归属裁决，服务端始终使用当前上下文。现有 `@DataIsolation` 继续生效，不新增第二套隔离机制。

不能仅修改 DTO 的 `@NotNull` 或只依靠数据库 DEFAULT：当前 DTO 会在执行 SQL 前被校验，Mapper 是否省略空列也不应成为页面契约的一部分。

## 3. 创建字段与默认值

| 对象 | 调用方业务输入 | 服务端补齐或固定 |
|---|---|---|
| 分类 | 分类名称、编码及现有 DTO 支持的业务字段 | `organId`；省略状态时 `ACTIVE`；省略 `sortOrder` 时 `0` |
| 标签 | 标签名称、编码、说明等业务字段 | `organId`；省略状态时 `ACTIVE`；省略 `sortOrder` 时 `0` |
| 主题 | `knowledgeKey`、`knowledgeName`、`knowledgeType`；可选分类和说明 | `organId`、`createUserId`；省略 `status` 时 `ACTIVE` |
| 版本 | `knowledgeId`、`revisionNo`、`sourceType`；可选 `priority`、`tagsJson`、FAQ 内容 | `organId`、`createUserId`；固定 `revisionStatus=DRAFT`、`chunkCount=0`、`indexStatus=PENDING`；省略 `priority` 时 `0`；省略标签时规范化为 `[]` |
| 发布选择 | `knowledgeId` | `organId`；固定 `releaseStatus=UNPUBLISHED`、`activeRevisionId=null`；发布人、发布时间和发布原因留空 |
| 回复模板 | `templateCode`、`templateName`、`systemPrompt`；可选说明和状态 | `organId`；省略 `status` 时 `ACTIVE` |

固定字段与可选默认值不同：固定字段无论调用方传什么都不能改变初始化结果；可选默认值只在未传时补齐，已传业务值仍按枚举及字段规则校验。

ID、时间、记录初始乐观锁列（`BasePo.version`，由 Mapper/数据库初始化）和逻辑删除值继续使用 common 模型、现有 ID 生成器及 Mapper/数据库规则；实现时核对落库值，不在业务层另建基础设施。**禁止修改** `g2rain-common` 的 `BaseDto` / `BaseVo` / `BasePo`。版本 CAS 的入参/出参见 §4.1，只落在知识版本自己的 DTO/VO 上。

### 3.1 CreateGroup 校验注解调整清单

**这段清单做什么：** `CreateGroup` 是 Bean Validation 分组；`Validations.validateSave` 在新增时会校验标了 `@NotNull(groups = CreateGroup.class)` / `@NotBlank(groups = CreateGroup.class)` 的字段。生成 DTO 把不少「本该服务端填充」的字段也标成了前端创建必填，导致最小业务 JSON 在 Service 补齐默认值之前就失败。

**这段清单不做什么：** 不改数据库列、不做 DDL/迁移、不改 common 基类、不删 DTO 上的 Java 属性。只调整各业务 DTO 上的校验注解与 Schema 说明，使「前端可不传 → 服务端规范化补齐 → 再 validateSave」成立。

规范化并补齐后再执行 `validateSave`。下列字段不得继续作为「前端创建必填」；应从 CreateGroup 去掉对应 `@NotNull`/`@NotBlank`（或改为服务端填充后再校验的策略），并在 Schema 标明服务端填充/可选默认/只读：

| DTO | 调整校验注解（服务端填充或可选默认，前端创建可不传） | 仍为调用方业务必填 |
|---|---|---|
| `KnowledgeCategoryDto` | `organId`；`status`（默认 `ACTIVE`）；`sortOrder`（默认 `0`） | `categoryCode`、`categoryName` |
| `KnowledgeTagDto` | `organId`；`status`（默认 `ACTIVE`）；`sortOrder`（默认 `0`） | `tagCode`、`tagName` |
| `KnowledgeItemDto` | `organId`；`status`（默认 `ACTIVE`）；`createUserId` | `knowledgeKey`、`knowledgeName`、`knowledgeType` |
| `KnowledgeRevisionDto` | `organId`；`revisionStatus`；`chunkCount`；`indexStatus`；`priority`（可选默认 `0`）；`createUserId` | `knowledgeId`、`revisionNo`、`sourceType` |
| `KnowledgeReleaseDto` | `organId`；`releaseStatus`；`activeRevisionId` 及发布人/时间/原因 | `knowledgeId` |
| `KnowledgeAnswerTemplateDto` | `organId`；`status`（默认 `ACTIVE`） | `templateCode`、`templateName`、`systemPrompt` |

版本上的文件元数据、解析、Embedding、审核、索引完成时间等字段保持非 Create 必填，且创建/更新均不得由普通 `/save` 写入。

### 3.2 版本创建规则

- `knowledgeId` 必须对应当前组织内存在的主题，来源类型与主题类型匹配。
- `FILE` 草稿允许暂时没有文件；保存后调用已有 `replace-file`。在文件就绪前，已有提交审核逻辑应拒绝提交。
- `FAQ` 草稿可以分步保存，提交审核前必须具备非空问题和答案；文件来源不得混入 FAQ 正文。
- `tagsJson` 保持现有字符串契约，不要求前端迁移字段名。服务端须新增解析规则：将 JSON 字符串解析为标签编码数组，再调用 `KnowledgeTagGovernanceService.validateDraftTags` 与 `KnowledgeTagPolicy` 规范化并写回 `tagsJson`。`null` 或省略 → 规范化为 `[]`；`""` 视为非法或等同空数组（实现时二选一并在 Schema 写明）；明确传入 `[]` 表示清空标签；非法 JSON 返回业务错误。Draft HTTP 路径继续使用 `List<String> tags`，与 `tagsJson` 双契约并存时，校验与规范化逻辑必须提取共享，避免漂移。
- 文件元数据、解析器信息、Embedding 信息、审核信息和索引完成时间均不作为普通版本创建输入。
- 沿用数据库唯一索引保证同组织、同主题版本号约束，不在前端承担最终唯一性裁决。

### 3.3 发布选择初始化规则

发布选择 `/save` 只负责建立主题的未发布选择记录，不负责发布版本。页面在没有记录时调用一次；并发重复创建沿用唯一约束和统一错误契约，不自动覆盖已有活动版本。实现时将唯一约束冲突映射到现有统一业务错误码，必要时可预查同主题记录以给出更明确提示，但不改为自动覆盖。

发布、回滚继续独立调用已有专用接口，不在主题或版本保存中自动创建发布记录，不将三个步骤组成后端管理事务。

## 4. 更新边界

| 对象 | 普通保存允许更新 | 普通保存不得更新 |
|---|---|---|
| 主题 | 名称、分类、说明、启停状态等原有业务字段 | 组织、创建人、创建时间；**已有任一关联版本时禁止修改 `knowledgeType`**（查当前组织下该主题是否存在 revision；若有则返回明确业务错误） |
| 版本 | 仅 `DRAFT`：版本号、优先级、标签、FAQ 内容 | 组织、所属主题、来源类型、版本状态、切片数、索引状态、文件元数据、解析字段、模型字段、审核字段、索引时间 |
| 发布选择 | 已有记录不提供通用业务更新，调用方应使用发布/回滚接口 | 活动版本、发布状态、发布人、发布时间、发布原因及所属主题 |
| 分类、标签、模板 | 现有业务可编辑字段 | 组织、创建时间及其他系统审计字段 |

保护字段应在 Service 到 PO 的转换中明确排除，更新时保留数据库值；不能把整个请求 DTO 无差别覆盖到 PO。携带旧页面冗余系统字段时忽略这些字段，不据此推进状态。对非 `DRAFT` 的版本编辑和发布选择的通用更新返回明确业务错误。

### 4.1 版本乐观锁（CAS）入参与出参

**范围限定（重要）：** 只改知识版本这条线的 `KnowledgeRevisionDto`、`KnowledgeRevisionVo` 与 `KnowledgeRevisionConverter`。**不修改** `BaseDto`、`BaseVo`、`BasePo`，也不给分类/标签/主题/发布/模板等其它 DTO/VO 补 `version`。库表乐观锁列本来就在 PO 上，无需 DDL。

现状核对：

- 乐观锁列在 `BasePo.version`；`KnowledgeRevisionDao.updateByVersion` 已存在，领域路径（Draft/Review）已在使用。
- `KnowledgeRevisionDto`、`KnowledgeRevisionVo` 当前**没有** `version` 属性；`KnowledgeRevisionConverter.dto2po` 忽略 `version` 是因为 DTO 没有该字段，不能据此做 CAS。common 基类也不含该 API 字段——这是预期，不要去改基类。

本次要求：

1. **仅在** `KnowledgeRevisionDto` 增加用于 CAS 的期望版本字段（推荐字段名 `version`，语义为期望的当前乐观锁值；也可命名 `expectedVersion`，与 `replace-file` 对齐，实现时二选一并全文统一）。
2. 草稿 `/save` 更新必须：白名单落字段 → 将期望版本显式写入 PO → 调用 `updateByVersion`；冲突返回与现有领域路径一致的业务错误（如 `REVISION_VERSION_CONFLICT`）。禁止依赖生成 `dao.update`（仅 `id` 条件且 `version+1`）更新版本行。
3. **仅在** `KnowledgeRevisionVo`（及 Converter `po2vo`）暴露 `version`，使 list/page 查询能拿到乐观锁值。这与 App 页面升级 review 第 2 项（列表/详情返回类型与 `replace-file` 所需 `expectedVersion`）绑定；本次后端 save 升级须一并完成该 VO 字段，否则草稿 CAS 与上传链仍断。不新增独立详情接口；继续用现有 list/page 按 ID 查询。
4. 未传字段保留原值；明确传空标签集合转换为 `[]`。Mapper 多为动态 `<if test="!= null">`：客户端传 `null` 通常**不会**清空库内列。须用负向用例验证：普通草稿 save 不得因省略或误传 `null` 而清空/替换已上传的文件元数据（`sourceStoragePath`、`sourceFileName`、`sourceContentType`、`sourceSha256`、`sourceFileSize` 等）。不承诺当前 Mapper 未实现的「显式清空可选业务字段」行为。

任务与切片的内部状态仍由既有处理链维护；它们已有保存入口本次只补受信组织处理，不向 App 暴露手工编辑界面。普通 CRUD 能否经 Gateway 暴露继续遵守现有资源准入配置。

## 5. 具体类与方法调整

| 现有类 | 方法/位置 | 调整内容 |
|---|---|---|
| `KnowledgeAccessService` / `KnowledgeAccessServiceImpl` | `requireOrganId`、`currentUserId`、`assertSameOrgan` | 复用已有实现，不新建组织上下文服务；需要创建人时对空 `userId` 拒绝 |
| 九个 `Knowledge*Controller` | `save` | 保留路径及 `Result<Long>`；**现状 save 均无 `@LoginGuard`**，本次统一补上；不新增聚合接口。验收：未登录或无组织上下文时保存失败 |
| 九个 `Knowledge*ServiceImpl` | `save` | 校验前填充受信组织；更新先检查原记录组织，禁止迁移归属 |
| `KnowledgeCategoryServiceImpl`、`KnowledgeTagServiceImpl` | `save` | 新增时补状态、排序默认值 |
| `KnowledgeItemServiceImpl` | `save` | 补默认状态和创建人；**提取并调用**分类归属校验（现仅在 DraftService 私有方法中，Item save 本身尚无） |
| `KnowledgeRevisionServiceImpl` | `save`；新增私有 `normalizeCreate`、`validateDraftUpdate`、`buildDraftUpdate` 等 | 初始化草稿；检查来源、主题、标签；限定更新字段；执行 `updateByVersion` |
| `KnowledgeReleaseServiceImpl` | `save` | 仅初始化未发布选择记录；阻止普通保存修改现有发布状态 |
| `KnowledgeAnswerTemplateServiceImpl` | `save` | 新增默认启用状态；模板目前无额外领域校验，本次仅补组织与默认状态，不虚构「已有模板业务校验」 |
| `KnowledgeDraftServiceImpl` | 私有校验方法 | **提取**最小共享方法：分类归属、来源与主题类型匹配、标签校验入口；禁止暗示 Item/Revision/Template 的生成 save 已具备这些校验。不将 `createKnowledge → createDraft → createRelease` 组合成新管理方法，也不再写一套平行创建路径 |
| `KnowledgeRevisionConverter` | DTO/PO/VO 映射 | 审计字段与内部状态明确处理；更新白名单；`po2vo` 增加 `version` 映射；不把受控存储路径重新暴露给调用方。不改 common Converter/基类 |
| 各业务保存 DTO | 校验注解与 Schema | 按 §3.1 **只改 CreateGroup 注解与 Schema 文案**，不改表结构 |
| `KnowledgeRevisionDto` / `KnowledgeRevisionVo` | 字段 | **仅此二者**增加 CAS 用 `version`；其它表 DTO/VO 不加 |

私有方法仅服务当前表保存，最终名称可按实现调整。DAO 保持与表一一对应，新增 SQL 只能放进对应表 DAO/Mapper。

## 6. 接口示例

下列路径是服务内 Controller 路径，App 通过 Gateway 调用时按既有服务前缀规则增加路由前缀。

创建主题：`POST /knowledge_item/save`

```json
{
  "knowledgeKey": "purchase_policy",
  "knowledgeName": "采购制度",
  "knowledgeType": "DOCUMENT"
}
```

创建文件草稿：`POST /knowledge_revision/save`

```json
{
  "knowledgeId": 1001,
  "revisionNo": "v1",
  "sourceType": "FILE"
}
```

更新文件草稿（示意）：`POST /knowledge_revision/save`

```json
{
  "id": 2001,
  "version": 3,
  "revisionNo": "v1",
  "priority": 1,
  "tagsJson": "[]"
}
```

创建发布选择：`POST /knowledge_release/save`

```json
{
  "knowledgeId": 1001
}
```

以上接口继续返回 `Result<Long>`，不改成完整对象；ID 的 JSON 表示沿用 G2rain 序列化配置，示例不另行定义前端数值精度策略。上传与草稿更新所需的乐观锁值由 list/page 返回的 `version` 取得（后端本次须保证 VO 暴露该字段）；属于与 App review 第 2 项对齐的契约修正，不在本次新增详情接口。

## 7. 实施步骤

1. 核对各表生成 DTO、Mapper 和 Starter 隔离行为，固定新增/更新的上下文填充位置；避免 Controller 提前校验未填充的字段；按 §3.1 **只调整 DTO 上 CreateGroup 校验注解**（不动 DDL、不动 common 基类）。
2. 对所有保存入口补 `@LoginGuard`、登录上下文与组织归属保护；复用 common 和 starter，**不修改**其公共实现（含 `BaseDto`/`BaseVo`/`BasePo`）。
3. 从 `KnowledgeDraftServiceImpl` 提取分类、来源匹配、标签相关最小共享校验；在分类、标签、主题、模板保存中补可选默认值；保留生成 CRUD，不改变 URL 和返回类型。
4. 版本：补默认状态、`tagsJson` 解析、字段白名单；**仅**给 `KnowledgeRevisionDto`/`KnowledgeRevisionVo` 补 `version`；草稿更新走 `updateByVersion`。
5. 调整发布选择保存为仅初始化，实际发布继续使用专用接口。
6. 更新 DTO Schema、接口契约及 App 页面方案，明确前端省略字段与版本 CAS `version`；更新 `deviations.md` 中「草稿 save 依赖 App 约定」的状态，完成后再记录为已实现；任务/切片/反馈白名单缺口如仍存在则登记为已知残余。

无需修改 DDL 或数据迁移。已有记录不批量重置为默认值，默认值仅作用于本次新建。生成文件的手工扩展须保留并 Review，不能通过重新生成覆盖已有领域代码。

建议与 [RQ-006](RQ-006-knowledge-source-download.md) 的实施顺序：先完成本文的组织/状态保护与 `KnowledgeRevisionVo.version` 出参，再部署下载接口；下载不依赖本文的默认值逻辑，但共享同一套登录与组织上下文。

## 8. 验收与后续验证计划

- 不传 `organId` 可创建主题、草稿、发布选择、分类、标签和模板；缺少受信组织或（需创建人时）缺少用户 ID 时失败；未登录调用 save 失败。
- 传入其他组织 ID 不能创建或迁移到该组织；跨组织关联主题、分类和更新目标被拒绝。
- 最小版本请求落库为 `DRAFT / PENDING / 0`，优先级 `0`，标签 `[]`；无需页面传系统状态。
- 使用 `/save` 伪造 `PUBLISHED`、`INDEXED`、审核信息或活动版本，不能改变系统状态。
- 上传后的文件元数据不会被普通草稿保存清空或替换（含请求省略字段与误传 `null` 的负向用例）。
- 草稿更新只修改允许字段；须携带正确 `version` 且走 `updateByVersion`；版本冲突失败；已审核、处理中、已发布版本无法通过普通保存修改。
- list/page 返回的 `KnowledgeRevisionVo` 包含可用于 `replace-file` / 草稿 save 的 `version`（其它资源 VO 不要求加该字段）。
- 已有关联版本的主题不能通过普通 save 修改 `knowledgeType`。
- 发布选择初始化不切换线上版本，也不写 Milvus；发布与回滚仍分别由已有接口处理。
- 保存接口路径、ID 返回契约、DAO 表对应关系和 G2rain 统一返回结构不变；`BaseDto`/`BaseVo`/`BasePo` 未改动。

实现后重点验证真实校验顺序、Mapper 空值处理、组织隔离与 CAS，不仅验证 DTO 赋值。构建和数据库联调按项目规范与用户指定的服务器验证安排执行；本文档阶段不运行构建、不修改业务代码（仅修订本需求文档）。

## 9. 兼容与发布

旧客户端继续传受保护字段时由服务端覆盖或忽略，不能借兼容性保留修改内部状态的能力。原先手工修改版本/发布状态的调用不再受支持，应迁移到已有领域动作接口。旧客户端若更新草稿但不传 `version`，应失败并提示，不得静默改用非 CAS 更新。

上线顺序为先部署后端保存契约（含 `KnowledgeRevisionVo.version`），再升级 App 表单。回退时应优先回退页面并保留组织与状态保护修复，避免恢复依赖客户端填写系统字段的行为。当前需求在进入代码实施时再更新 `docs/project.yaml` 的活动需求，不在文档提案阶段替换正在实施的需求。
