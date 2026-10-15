# 知识库服务代码分析与开发执行计划

分析日期：2026-10-06。本文基于工作区现有源码、POM、DDL、docs，以及本地 `g2rain`、`g2rain-common`、`g2rain-spring-boot-starter` 源码制定。本文是待执行计划，不代表领域功能已实现或通过测试。本轮仅写文档，不修改 Java、SQL、POM、配置，也不执行代码生成、数据库迁移或外部调用。

## 1. 实施结论与约束

工程已经具备 `startup → biz → api` 三模块和九张表的生成 CRUD。下一步开发应在生成代码上补充知识领域流程，不能重新开发实体基础框架、通用增删改查、租户拦截器、分页、ID、统一返回和异常处理。

计划范围补充（2026-10-06）：不因业务逻辑不足调整已生成通用接口及其通用实现。现有接口的路径、参数、响应和通用 CRUD 行为保持原有契约；需要生命周期、状态 CAS、多表事务、审核、发布等业务语义时，新增专用领域接口、Service 与必要的领域 SQL。只有经确认的安全性不足（如越权、组织隔离失效、内部路径或敏感信息泄露）属于现有接口整改范围，且应采用最小必要变更并记录影响。

“缺少业务状态机、编排或乐观锁语义”本身按逻辑差距处理，不能据此关闭通用接口、改变 generated save/delete 行为或把通用实现改造成领域命令。若确有绕过授权修改受保护字段等安全问题，应单独说明攻击入口、缺失权限和受影响数据，不能将所有逻辑差距统一归为安全整改。

必须遵循中央 `java-domain-service 1.0.0`，并落实 [公共基线](../architecture/g2rain-foundation.md)、[模块边界](../architecture/modules.md)、[领域规则](../architecture/domain-rules.md)、[业务流程](../architecture/business-flows.md)、[检索契约](../api/integration-contracts.md)、[安全边界](../security/security-boundaries.md)。规则优先级为中央规范、项目明确的领域约束、现有实现；生成代码不是领域正确性的证明。

- MySQL 是元数据、状态和 `active_revision_id` 的唯一权威；Milvus 是可重建投影。
- 领域顺序固定为 `DRAFT → REVIEWING → APPROVED → PROCESSING → READY_TO_PUBLISH → PUBLISHED`：人工**只审核源文件/FAQ 与标签**；审核通过后才由 `PROCESS` 任务执行解析、切片、Embedding 与 Milvus staged 写入。审核通过前不调用 Embedding、不写 Milvus。正式说明见 [DEV-001](../architecture/deviations.md)。
- 发布、回滚、下线为人工同步命令，不建立发布任务、定时发布或自动到期机制。
- API 模块发布检索查询、稳定 VO/枚举；运营本地写入 DTO 留在 Biz，不能向其他后端提供通用远程 CRUD。
- 每次读写校验可信主体、组织归属和逻辑删除；不调用生成的 `*WithoutIsolation` 方法处理普通业务。
- 事务只覆盖本地必要写入，网络、磁盘、解析、Embedding、Milvus 不进入长事务；独立事务 Bean 保证 Spring 代理生效。
- 首期不实现会话、不部署或管理模型/Docling/Milvus 基础设施；本服务提供受控问答 `POST /knowledge/answer`（模板 + 可信证据），与检索证据契约分离。访问 = Gateway 可达 + Starter 隔离（DEV-003）。

## 2. 已有实现盘点与差距

### 2.1 可直接复用的生成资产

八个前缀为 `KnowledgeCategory`、`KnowledgeTag`、`KnowledgeItem`、`KnowledgeRevision`、`KnowledgeChunk`、`KnowledgeTask`、`KnowledgeRelease`、`KnowledgeFeedback`。

| 已有位置 | 生成资产及方法 | 本计划处理方式 |
| --- | --- | --- |
| API `api/`、`dto/`、`vo/` | 九组 `*Api`、`*SelectDto`、`*Vo`；查询契约 `selectList`、`selectPage` | 保留查询能力，审查响应字段和访问授权；不再创建同类查询 CRUD |
| Biz `dto/`、`converter/` | 九组 `*Dto`、`*Converter` | 持久化转换按需复用；运营命令使用窄 DTO，不把完整表字段开放给调用方 |
| Biz `dao/po/`、`dao/` | 九组 `*Po`、`*Dao`；`insert`、`insertMultiple`、`update`、`delete`、`updateByVersion`、`selectById`、`selectList` | 普通表操作复用；仅状态 CAS、任务租约、跨表复核等缺失领域 SQL 才扩展 |
| Biz `service/`、`service/impl/` | 九组 `*Service`、`*ServiceImpl`；`save`、`delete`、列表和分页 | 保留通用实现；业务逻辑不足由新增领域 Service 承担；仅确认的安全问题最小整改 |
| Biz `controller/` | 九个生成 Controller，`/knowledge_*` 的查询、`/save`、删除入口 | 保留通用接口；另增审核、发布、任务等专用接口，不以逻辑不足为由关闭或改造现有入口 |
| Biz `resources/mybatis/mapper/` | 九个 `*Mapper.xml` | 保留生成 SQL；新增领域 SQL 使用独立 DAO/XML，避免被再次生成覆盖 |
| Startup | `Application`、`ArgumentResolverConfig`、`VirtualThreadConfigurer`、dev/nacos 配置 | 复用启动骨架，只在后续阶段整改装配差距 |
| 根目录 | POM、`codegen.properties`、[初始化建表脚本](../../scripts/g2rain-knowledge.sql) | 不重建工程，不重复生成 CRUD，不把初始化脚本当作生产升级脚本重复执行 |

已观察到：生成 PO/DTO/VO 复用 common 基类，DAO 标注 `@DataIsolation`，API 仅发布查询接口，Biz 保存本地写入 DTO，基本模块方向符合规范。未发现 `src/test` 测试源；没有证据证明完整构建、真实隔离、发布或检索已经可用。

### 2.2 实施前必须处理的具体问题

| 编号 | 源码/文档证据 | 影响与计划 |
| --- | --- | --- |
| A01 | 2026-10-06 时的 `docs/project.yaml`、`docs/index.md`、架构与本地开发文档未反映现有工程，AGENTS 状态段也已过时 | P0 更新 docs 项目事实并逐页区分“已生成骨架”与“领域功能规划”；AGENTS 的文字调整留到获得实现范围后处理 |
| A02 | 根 POM 使用 Java 25、Boot 4.0.5、common 1.0.8、Starter 1.0.5，插件是 `g2rain-crafter:1.0.8`；旧 docs 称 `g2rain-generator-maven-plugin` | 以当前 POM 为事实，不猜版本、不替换插件。核对 Crafter 与旧生成器的迁移关系、实际目标和配置后更新文档；已有 CRUD 无须重跑 |
| A03 | `KnowledgeRevisionServiceImpl.save` 更新调用 `dao.update`；对应共享 update SQL 只按 id 更新并递增 version；已有 `updateByVersion` | 属于领域并发语义差距，不调整通用 save。新增领域编辑复用 `updateByVersion`，状态推进添加状态+version CAS；领域接口不允许丢失更新 |
| A04 | 生成写 DTO 包含 organId、状态及内部字段；通用写 Controller 没有知识领域授权/状态编排 | 拆分安全与逻辑：组织/操作者伪造、未授权写入须核验并最小修复；缺少状态编排通过新增领域接口实现，不据此限制或改造通用接口。新增命令不接受客户端决定组织、操作者、审核/发布状态、路径和租约 |
| A05 | `KnowledgeRevisionVo` 有 `sourceStoragePath`、`parsedStoragePath`、`parserTaskId` 等内部字段；反馈 VO 有修正答案 | 响应投影及生成查询必须排除内部路径/供应商标识；反馈敏感字段按能力控制。仅隐藏 OpenAPI 字段不构成保护 |
| A06 | Starter `PrincipalContextFilter` 从请求头赋值；登录守卫和参数注入是框架能力 | 上下文存在不等于身份已验证。冻结 Gateway/服务身份校验链和能力来源，拒绝直连伪造主体头；不能以 `isBackEnd` 自动放行 |
| A07 | Starter `GlobalExceptionHandler.respond` 统一返回 HTTP 200；检索文档要求依赖失败 HTTP 503，示例是裸业务 JSON | P0 明确 `Result<KnowledgeRetrieveVo>` 包装和 snake_case 业务字段，联调 Agent Runtime；专用检索依赖异常处理保持 common 错误体并返回 503 |
| A08 | `codegen.properties`、开发连接配置需 Secret 审核 | 已见代码生成配置包含明文数据库口令；实施时改为受控本地/Secret 注入并排查历史暴露，不在本文复制值。本轮不修改配置 |
| A09 | Spring 虚拟线程开关与 `VirtualThreadConfigurer` 同时存在；分页已有手写解析器 | P0 核验是否重复配置，HTTP 线程与异步任务分别测试；任务使用 common 上下文传播，不能直接照搬裸执行器 |
| A10 | task 没有取消请求、处理输入快照、索引重建目标字段 | P0/P1 冻结最小增量迁移方案，支持协作取消、同任务重试与重建目标；不能把缺失信息塞进错误摘要 |
| A11 | 早期领域规则“检索未知标签按无匹配”与接口/实现“必须存在则拒绝”不一致 | **已冻结为拒绝**（DEV-002）；停用标签保留历史检索语义；标签在 DRAFT 确认，`REVIEWING`/`APPROVED` 起冻结 |
| A12 | 早期计划写「先 PARSE 再审核再 INDEX」；实现与业务流程已改为「先审源文件，再 PROCESS」 | 以 [domain-rules](../architecture/domain-rules.md) 与 [DEV-001](../architecture/deviations.md) 为准；计划中 PARSE/INDEX 分任务描述视为历史草案，主路径收敛为审核后 `PROCESS` |

上述静态分析形成于 2026-10-06，不能据此宣称现有隔离插件、DDL 执行和依赖组合已通过验证。后续在 2026-10-08 执行的 `mvn clean verify` 已通过（Biz 19 个单元测试）；真实服务和集成验证范围仍以本计划列出的待验证项为准。

## 3. common 与 Starter 的复用落点

| 场景 | 复用类型/模块（本地源码已存在） | 知识库需补充的领域部分 |
| --- | --- | --- |
| 数据与协议模型 | `com.g2rain.common.model.BasePo/BaseDto/BaseVo`、`PageSelectListDto`、`PageData`、`Result` | 业务 DTO/VO、字段白名单；不写替代基类/响应壳 |
| ID、时间、校验 | `IdGenerator.generateId()`、`Moments`、`Validations`、CreateGroup/UpdateGroup | revision/chunk/task/audit ID 均用注入 ID；领域状态和格式校验；时间按 UTC 存储，统一 Java 时间转换 |
| 异常 | `BusinessException`、`ErrorCode`、`SystemErrorCode`、`ExceptionProcessor` | `KnowledgeErrorCode` 只补领域错误语义，专用检索依赖异常不改变平台异常体系 |
| 主体 | `PrincipalContextHolder.require/getOrganId/getUserId/getTraceId`、`PrincipalContext` | 获取已验证主体、组织归属、领域能力校验；不建私有 ThreadLocal |
| 请求基础能力 | `g2rain-starter-aegis-core`（POM 聚合 web-infra、identity-client、tracing-otel） | 复用作用域 Filter、登录守卫、身份注入、JSON、异常处理、ID Bean、trace；业务能力与调用来源另行校验 |
| SQL 隔离与分页 | `g2rain-starter-mybatis-extensions`、`@DataIsolation`、`PageContext` | 对新增 DAO 配置隔离；JOIN 每张表仍明确 `organ_id`、`delete_flag=0`；严格同组织不能被组织层级范围扩大 |
| 同主题发布互斥 | `g2rain-starter-data-redis` 的 `DistributedLock` / `RedissonClient` | 锁键含 organId+knowledgeId，覆盖预启用、CAS、关闭与补偿；MySQL CAS 仍为正确性底线 |
| 异步传播 | `Contexts.wrap/wrapCallable/wrapSupplier`、`ContextExecutors`、`PrincipalContextHolder.runWith/callWith` | Scheduler 根据受信组织配置建立新作用域；持久任务从可信记录恢复最小上下文，不持久化用户 Token |
| 内部服务调用（确有需要时） | `g2rain-starter-feign-plus` | 复用内部服务 Result 解码及主体透传；外部 Docling/模型接口只传独立供应商凭据，不透传完整主体头 |
| 可观测性/OpenAPI | `g2rain-starter-tracing-otel`、`g2rain-starter-spring-doc`、Actuator | 领域指标、净化错误、就绪检查；禁止将查询正文作为指标标签 |
| 消息 | cache-sync、stream-redis 当前无需新增 | 首期使用 MySQL 任务，不引入第二份任务权威或发布消息队列 |

注意：本地 common 文档存在版本描述漂移，Starter POM 已引用 common 1.0.8。实施以最终解析的依赖和公共 API 为准，并通过 `mvn dependency:tree`、`mvn clean verify` 验证，不能仅凭源码目录推定运行版本兼容。

## 4. 类、方法和模型清单

下文以 `com.g2rain.knowledge` 为包根。所有新类为计划命名；已有九组 CRUD 类不在新增清单。Service 接口在 `service`，实现类在 `service.impl`，类名为对应接口加 `Impl`。事务协作类在 `service.tx`，纯规则在 `domain`，客户端接口及实现均在 `client` 子包；不新增重复 Repository 层。

表中签名用于明确职责与开发范围，最终参数细节在对应需求开始时冻结。`organId` 只允许受信服务内部传递，不进入公开请求模型；`expectedVersion` 为 `Integer`，ID 为 `Long`，时刻使用 UTC 对应的时间类型。

### 4.1 API 模块：检索契约及稳定模型

| 类/包 | 字段或方法 | 责任 |
| --- | --- | --- |
| `api.KnowledgeRetrieveApi` | `Result<KnowledgeRetrieveVo> retrieve(KnowledgeRetrieveRequest request)` | 唯一已明确的 Runtime 查询入口 `POST /knowledge/retrieve` |
| `request.KnowledgeRetrieveRequest` | `query`、`allTags`、`anyTags`、`topK` | 对应 `all_tags/any_tags/top_k`；不接受组织、collection、profile、revision、阈值、路径 |
| `vo.KnowledgeRetrieveVo` | `answerable`、`traceId`、`reasonCode`、`sources` | NO_EVIDENCE 为正常成功业务结果 |
| `vo.KnowledgeEvidenceVo` | `knowledgeId/revisionId/chunkId/content/title/section/page/version/citation/score` | ID 保持 common 的字符串序列化；业务 `version` 表示 revisionNo，不覆盖 BaseVo 的乐观锁字段，使用独立组合模型 |
| `vo.KnowledgeRevisionDetailVo` | revision ID、状态、来源展示、标签、预期版本、任务摘要 | 安全运营详情，禁止路径与解析供应商任务标识 |
| `vo.KnowledgeTaskStatusVo`、`vo.KnowledgeReleaseResultVo` | 安全任务状态；发布后 releaseVersion、活动 revision、警告码 | 不返回租约内部标识或原始依赖异常 |
| `enums.KnowledgeRevisionStatus` | DRAFT/REVIEWING/APPROVED/PROCESSING/PROCESS_FAILED/READY_TO_PUBLISH/PUBLISHED/DEPRECATED（及兼容保留的历史别名若仍存在） | 以业务流程状态机为准；DDL COMMENT 滞后见 DEV-001，禁止另建与业务流程冲突的状态词汇 |
| `enums.KnowledgeTaskType/KnowledgeTaskStatus/KnowledgeSourceType` | 主路径 `PROCESS`（内含解析/切片/Embedding/Milvus）与 `DELETE_INDEX`；PENDING/RUNNING/SUCCESS/FAILED/CANCELED；FILE/FAQ | 不新增 PUBLISH 任务类型；历史 PARSE/INDEX 分任务命名仅作兼容或清理对象 |
| `enums.KnowledgeIndexStatus/KnowledgeReleaseStatus/KnowledgeFeedbackType/KnowledgeFeedbackStatus` | 按 DDL 原值定义 | P0 对照 DDL 冻结，不根据常识新增状态 |
| `enums.KnowledgeErrorCode` | 实现 ErrorCode；状态冲突、版本冲突、租约丢失、索引不完整、profile 不匹配、依赖不可用等 | 参数/鉴权等通用错误优先复用 SystemErrorCode；错误编码先核对平台命名和冲突 |

运营写契约不移入 API。反馈若后续需要 Runtime 同步写，先登记调用方、权限、幂等和架构例外，再定义受信命令；首期反馈由 App 经 Gateway 写入。

### 4.2 Biz：运营 DTO 与流程对象

| 类型（DTO 在 Biz 模块的 `dto`） | 必要字段 |
| --- | --- |
| `KnowledgeCreateDto` | knowledgeKey、knowledgeName、knowledgeType、categoryId、description；不含状态/organId |
| `KnowledgeDraftCreateDto` | knowledgeId、revisionNo、sourceType、可选复制的历史 revision ID、tags、priority |
| `KnowledgeDraftEditDto` | revisionId、expectedVersion、可编辑展示信息、tags、priority；来源替换用明确的文件/FAQ 方法 |
| `KnowledgeFaqEditDto` | revisionId、expectedVersion、question、answer |
| `KnowledgeParseDto` | revisionId、expectedVersion |
| `KnowledgeReviewDto` | revisionId、expectedVersion、净化后的审核意见；通过与拒绝用不同方法 |
| `KnowledgePublishDto`、`KnowledgeRollbackDto` | knowledgeId、目标 revisionId、expectedReleaseVersion、commandKey、reason |
| `KnowledgeOfflineDto` | knowledgeId、expectedReleaseVersion、commandKey、reason |
| `KnowledgeTaskRetryDto`、`KnowledgeTaskCancelDto` | taskId、expectedVersion、reason |
| `KnowledgeDeleteDto` | knowledgeId 或 revisionId、expectedVersion、reason；服务方法区分删除对象 |
| `KnowledgeTagGovernDto` | tagId、expectedVersion、名称/排序/状态；code 只在创建时设置 |
| `KnowledgeFeedbackCreateDto`、`KnowledgeFeedbackProcessDto` | 定位信息、类型、净化摘要；处理状态、expectedVersion、可选修正答案 |
| `KnowledgeRebuildDto` | 受控目标 profile 标识、knowledgeId/revisionId、reason；不接受任意服务地址或 collection 名 |

命令校验复用 common 校验分组，不允许客户端写入审核人、发布人、任务状态、lease、storagePath。上传绑定 `MultipartFile` 留在 Controller，Service 接收受控 `InputStream` 与元数据。

`domain.model` 内部流程对象：`StoredSource`（相对定位、hash、size、MIME）、`ParsedDocument`（结构化解析结果）、`ChunkContent`（稳定编号、定位、正文/hash/token/page/titlePath）、`TaskInputSnapshot`（来源 hash、parser/chunker/profile 版本、目标 profile）、`TaskLease`（taskId、organId、owner、领取代次/预期 version、到期时刻）、`ReleaseChange`（新旧 revision、release 版本、commandKey、动作）、`RetrieveCandidate`、`VerifiedCandidate`、`IndexCheckResult`、`RebuildCheckResult`。这些对象不作为公开契约、不输出包含原文的 toString。

### 4.3 Biz：入口、授权与查询

| 类 | 待开发方法 | 约束 |
| --- | --- | --- |
| `controller.KnowledgeRetrieveController` | `retrieve(KnowledgeRetrieveRequest)` | 实现 KnowledgeRetrieveApi，绑定/校验/包装，逻辑委托 RetrieveService |
| 各领域 Controller | 主题、版本、任务、发布等各自领域动作 | 管理端优先复用对应领域的 Controller 接口；前端 App 负责组合调用，不新增聚合式 `KnowledgeManagementController` |
| `controller.KnowledgeReleaseCommandController` | `publish`、`rollback`、`offline`、`reconcile` | 同步命令，返回明确的已提交结果和投影警告 |
| `controller.KnowledgeTaskController` | 领域命令：`processRevision`、`retry`、`cancel`（以及生成 CRUD） | 不向前端开放领取、续租和标记成功 |
| `controller.KnowledgeFeedbackCommandController` | `submitFeedback`、`processFeedback`、`createDraftFromFeedback` | 查询列表复用已有查询能力，并脱敏 |
| `controller.KnowledgeTagCommandController` | `renameTag`、`disableTag`、`deleteTag` | 新增具有标签治理语义的命令入口；通用标签 CRUD 保留，不向其注入冻结 code、历史引用等业务逻辑 |
| `service.KnowledgeAccessService` / `KnowledgeAccessServiceImpl` | `PrincipalContext requireTrustedPrincipal()`；`Long requireOrganId()`；`void requireCapability(String capability)`；`void requireRuntimeRetrieve()`；`void assertSameOrgan(Long recordOrganId)` | 从平台已有身份/权限能力取证，失败关闭；不实现新认证协议，不能把普通主体头或 admin 标记当全部权限 |
| `service.KnowledgeManagementQueryService` / Impl | `KnowledgeRevisionDetailVo getRevisionDetail(Long revisionId)`；`KnowledgeTaskStatusVo getTaskStatus(Long taskId)` | 领域安全投影；基础列表/分页不重新实现 |
| `converter.KnowledgeManagementConverter`、`KnowledgeEvidenceConverter` | `toRevisionDetail(...)`、`toTaskStatus(...)`、`toEvidence(...)` | 纯内存字段转换，无 SQL/网络 |

新增领域接口负责审核、版本不可变、任务领取、发布切换、标签治理及反馈闭环；运营相关业务流程接入这些专用接口。现有九组通用接口及通用实现不承担新增业务编排，不因缺少这些逻辑而关闭、重定向、修改字段契约或追加状态规则。

安全例外单独核验：若现有接口存在跨组织访问、未授权写入或内部路径/敏感信息泄露，应复用 common/Starter 能力进行最小必要修复，并记录证据、整改范围与兼容影响。新增安全详情投影不能替代对现有查询泄露问题的修复；也不能仅因为接口能够修改某表，就认定其所有业务逻辑差距都是安全缺陷。

### 4.4 Biz：知识主题、草稿、标签和状态机

| 类 | 待开发方法 | 事务与复用 |
| --- | --- | --- |
| `service.KnowledgeDraftService` / Impl | `Long createKnowledge(KnowledgeCreateDto)`；`Long createDraft(KnowledgeDraftCreateDto)`；`void editDraft(KnowledgeDraftEditDto)`；`void replaceFile(Long revisionId, Integer expectedVersion, InputStream content, SourceMetadata metadata)`；`void editFaq(KnowledgeFaqEditDto)`；`void reopenDraft(Long revisionId, Integer expectedVersion)` | 组织/权限/状态校验；文件 IO 在事务外，事务失败回收本次未引用暂存文件；复用生成 insert/updateByVersion，不复制生成 save |
| `service.tx.KnowledgeDraftTxService` | `void attachSource(...)` | 管理端接口保持单一动作；主题、草稿和发布记录由前端 App 按需要组合调用，不在主题创建接口内预建 release |
| `service.KnowledgeTagGovernanceService` / Impl | `void validateDraftTags(List<String> codes)`；`List<String> normalizeRetrieveTags(List<String> codes)`；`void renameTag(...)`；`void disableTag(...)`；`void deleteTag(...)` | 基础创建/查询复用生成资产；禁止改 code；停用不重写历史快照；未知检索 code 无匹配，非法格式拒绝 |
| `domain.KnowledgeRevisionStateMachine` | `void requireEditable(status)`；`void assertTransition(from,to)`；`void requireReviewable(revision)`（源内容就绪，不要求解析清单）；`void requireProcessable(revision)`；`void requirePublishable(revision,indexCheck)` | 纯规则；审核只校验源与标签；失败状态恢复 DRAFT 必须重新验证唯一草稿条件 |
| `domain.KnowledgeSourcePolicy` | `void validateFile(metadata,sniffResult)`；`void validateFaq(question,answer)`；`void assertExclusiveSource(sourceType,...)` | 大小/白名单/MIME/内容嗅探一致；FILE 与 FAQ 二选一 |
| `domain.KnowledgeTagPolicy` | `List<String> normalize(codes)`；`void validateCode(code)`；`void assertCodeImmutable(old,new)` | 小写字母数字下划线、去重、数组和字节长度上限 |

分类首期保持扁平字典，普通维护使用现有 `KnowledgeCategoryService`；知识主题领域命令负责 categoryId 的同组织引用校验，不改造通用分类维护逻辑，不再建分类 CRUD 或树服务。priority 仅运营/同主题并列展示排序，不能成为召回过滤或权限。

### 4.5 Biz：受控存储、解析与切片

| 类 | 待开发方法 | 责任 |
| --- | --- | --- |
| `client.storage.KnowledgeStorageClient`、`LocalKnowledgeStorageClient` | `StoredSource storeSource(organId,knowledgeId,revisionId,InputStream,metadata)`；`InputStream openSource(...)`；`void writeParsedArtifacts(...,ParsedDocument,List<ChunkContent>)`；`List<ChunkContent> readChunkContents(...,locators)`；`boolean verifyArtifacts(...)`；`void deleteUnreferencedStaging(...)` | 只允许服务生成的路径；原子落盘；检测 symlink/穿越/越界；外部不见存储路径 |
| `client.docling.DoclingClient`、`DoclingServeClient` | `ParsedDocument parse(InputStream content, SourceMetadata metadata, Deadline deadline)`；`List<ChunkContent> chunk(ParsedDocument document, ChunkingProfile profile, Deadline deadline)` | 适配受控 Docling/HybridChunker 契约，不假设 Java 内有 Python chunker；若 Serve 不提供切片则先补内部适配契约 |
| `domain.KnowledgeChunkPolicy` | `void validateManifest(List<ChunkContent>)`；`List<ChunkContent> chunkFaq(question,answer,profile)`；`void verifyContentHash(...)` | FAQ 走相同稳定定位/引用规则；UTF-8 字节、token、页码/定位、顺序完整性；正文不写 MySQL |
| `service.KnowledgeParseService` / 解析 Tx（若保留） | 解析与切片作为 `PROCESS` Worker 内部步骤，**不是**审核前独立入口；不得再以「解析成功自动进入 REVIEWING」作为主路径 | 主入口为审核通过后的 `processRevision` / `PROCESS` 任务；独立 `submitParse` 仅可作内部/兼容能力，不能绕过审核 |

存储命名包含 organId/knowledgeId/revisionId；解析临时产物还须含任务代次，旧 worker 不得覆盖新租约产物。相同 SHA-256 只提示重复/候选复用，不跳过审核。文件和数据库不能原子提交，采用暂存、验证、短事务挂接及失败清理；崩溃残留通过受控维护检查发现，首期不自动删除已引用历史文件。

### 4.6 Biz：任务租约、幂等、重试和 Worker

| 类 | 待开发方法 | 责任 |
| --- | --- | --- |
| `service.KnowledgeTaskCommandService` / Impl | `Long ensureTask(revisionId,type,input)`；`TaskLease claimNext(Long organId,String workerId)`；`boolean renewLease(TaskLease)`；`void retry(KnowledgeTaskRetryDto)`；`void cancel(KnowledgeTaskCancelDto)`；`int recoverExpiredLeases(Long organId)` | MySQL 权威；ensureTask 是领域幂等命令，复用生成 insert；租约使用同一 UTC 时间标准。命名避开生成器的 `KnowledgeTaskService`。当前已实现 FAILED 原任务重试和 PENDING 取消；RUNNING 协作取消待持久取消标记迁移后实现 |
| `service.tx.KnowledgeTaskTxService` | `Long createTask(...)`；`TaskLease claim(...)`；`boolean renew(...)`；`void retryFailed(...)`；`void requestCancel(...)`；`int recoverExpired(...)` | 领取条件 PENDING+到期+未删除；owner/代次/status/expiry 条件更新；失败外层捕获唯一键冲突再回查 |
| `service.KnowledgeTaskExecutionService` / Impl | `void execute(TaskLease)`；`void executeDeleteIndex(TaskLease)`；`void handleFailure(TaskLease,Throwable)` | 分派 `PROCESS`/`DELETE_INDEX`；`PROCESS` 内顺序执行解析、切片、Embedding、Milvus staged 与对账；读取输入快照，绑定组织作用域；异常分类、重试与结束写入短事务 |
| `domain.KnowledgeTaskPolicy` | `String idempotencyKey(revisionId,type,input)`；`boolean isRetryable(failure)`；`Duration nextDelay(attemptNo)`；`void assertLeaseValid(lease,current)` | 幂等键包含输入版本、parser/chunker/profile、必要目标信息；退避/上限配置化 |
| `service.KnowledgeTaskContextService` / Impl | `<T> T callInTaskContext(TaskLease,Callable<T>)` | 由受信数据库任务构造最小上下文，再用 PrincipalContextHolder/Contexts 传播；不恢复用户 Token，也不假造普通运营权限 |

首期 Scheduler 的组织集合来自服务端受控配置并经过部署授权，不来自请求，不新增跨组织无隔离扫描。每个组织独立上下文领取/恢复，轮转避免饥饿。规模需要全局调度时，必须先在 deviations/需求登记受信最小元数据扫描方案，再实现；不能直接使用 `selectListWithoutIsolation` 拉全表。

领取可采用行锁与条件更新；首期不依赖 `SKIP LOCKED` 才能实现互斥。如选择该语法，须核对目标 MySQL 版本和隔离插件支持，不能仅按 8.0.13 设计下限推定兼容。

租约到期后旧 Worker 必须停止落库和发布任何成功结果。MySQL 的 fencing 不能自动约束 Milvus/文件副作用：同 revision 的处理还需由 Starter 分布式锁控制，失锁/失租后终止写入，幂等 Entity upsert + 对账兜底。依赖调用设上限和续租，旧任务结果未被数据库接受时不得推进 revision。

PENDING 可以直接取消；RUNNING 用持久取消标记协作取消，无法中断的依赖调用结束后仍不得写 SUCCESS。人工重试复用同一任务、保留尝试次数，并把 `PROCESS_FAILED` 恢复到可重试或可退回草稿的正确状态。`PROCESS` 已成功且已发布后，不能被人工重试重置为 staged=false 的写入而破坏已发布投影。

### 4.7 Biz：审核、Embedding、Milvus 索引

| 类 | 待开发方法 | 责任 |
| --- | --- | --- |
| `service.KnowledgeReviewService` / Impl | `void submit(...)`；`void approve(KnowledgeReviewDto)`；`void reject(KnowledgeReviewDto)` | 只校验源内容与标签就绪 + REVIEWING + expectedVersion，能力 knowledge.review；`approve` → `APPROVED`，**不**在审核事务内创建处理任务或访问模型/Milvus；拒绝回 `DRAFT` |
| `service.tx.KnowledgeReviewTxService`（若拆分） | `void approveSource(...)`；`void rejectAndRecord(...)` | 审核状态/操作者/时间/净化原因同事务；拒绝不产生 `PROCESS` 任务 |
| `client.model.EmbeddingClient`、`ConfiguredEmbeddingClient` | `List<float[]> embed(List<String> texts)` | Model Nexus `POST /v1/embeddings`；校验维度/个数/NaN/Infinity；透传 PrincipalHeaders；不日志正文 |
| `client.model.KnowledgeRerankClient`、`ConfiguredKnowledgeRerankClient` | `List<RerankScore> rerank(String query,List<String> documents,int topN)` | Model Nexus `POST /v1/rerank`；只对 MySQL 校验后的候选重排；允许 `top_n` 子集结果；透传 PrincipalHeaders |
| `client.milvus.MilvusKnowledgeClient`、`MilvusV2KnowledgeClient` | `void validateSchema(EmbeddingProfile)`；`void upsertStaged(List<IndexEntity>,Deadline)`；`List<RetrieveCandidate> search(vector,filter,limit,cursor,Deadline)`；`void setSearchable(organId,revisionId,chunkIds,value,Deadline)`；`IndexCheckResult inspectRevision(...)`；`void deleteRevision(organId,revisionId,chunkIds,Deadline)` | 官方 V2 SDK 封装；显式 Schema；chunkId 为现有 MySQL ID；searchable 默认 false；collection 只来自服务器 profile |
| `domain.KnowledgeEmbeddingPolicy` | `void validateVectors(vectors,profile)`；`void assertProfileCompatible(profile,collectionSchema)` | 首期 qwen-4096-v1、4096 维、COSINE/HNSW；模型版本和归一化固定绑定 |
| `domain.KnowledgeMilvusFilterBuilder` | `Filter buildRetrieveFilter(organId,allTags,anyTags)`；`Filter buildRevisionFilter(organId,revisionId)` | 强制 organId/searchable；参数转义/模板化；不接收原始表达式，不添加常态巨量 revision IN |
| `service.KnowledgeIndexService` / Impl | `void executeIndex(TaskLease,TaskInputSnapshot)`；`IndexCheckResult verifyRevision(Long revisionId)` | 外部 Embedding/upsert/read-after-write 对账在事务外；数量、ID、组织、revision、tags、profile、hash/抽样正文校验 |
| `service.tx.KnowledgeIndexTxService` | `void completeIndex(TaskLease,IndexCheckResult)`；`void failIndex(TaskLease,safeError)` | 租约/输入快照/revision CAS 后推进 READY_TO_PUBLISH、INDEXED，并写任务成功；任何缺项不得成功 |

Milvus partial upsert、读写可见性、服务实际版本、SDK 版本在 P0 用部署/本地依赖事实冻结。若 partial upsert 不受支持，需评审完整 Entity 更新方案，不在 Service 里猜 API、不盲目追加旧 SDK。重试 INDEX 不覆盖已发布版本或切片算法变化后的旧 Entity；内容/profile 变更应创建新 revision 或独立重建处理输入。

### 4.8 Biz：同步发布、回滚、下线与补偿

| 类 | 待开发方法 | 事务/并发 |
| --- | --- | --- |
| `service.KnowledgeReleaseCommandService` / Impl | `KnowledgeReleaseResultVo publish(KnowledgePublishDto)`；`rollback(KnowledgeRollbackDto)`；`offline(KnowledgeOfflineDto)`；`IndexCheckResult reconcile(Long knowledgeId)` | 锁内编排校验、目标预启用、MySQL Tx、关闭旧版本或补偿；reconcile 同步执行，无发布任务 |
| `service.KnowledgeReleaseGuard` / Impl | `<T> T withKnowledgeLock(Long organId,Long knowledgeId,Supplier<T>)`；`void assertLockHeld(...)` | 复用 Starter Redisson；锁获取等待有上限，显式区分看门狗与固定 lease；状态/锁失效失败关闭 |
| `service.KnowledgeReleaseService` | `Long publish(KnowledgeReleasePublishDto)` | 复用生成的 release/revision DAO 的 `updateByVersion`，在一个动作内完成 active revision 切换、目标 PUBLISHED 和旧版 DEPRECATED |
| `domain.KnowledgeReleasePolicy` | `void validatePublish(...)`；`void validateRollback(...)`；`void validateOffline(...)`；`void assertReplayMatches(...)` | 目标同组织同主题、未删除、已审核、索引完整；回滚只到允许的历史 revision；相同 commandKey 不允许不同参数 |

执行顺序：校验 releaseVersion、revisionVersion、目标和索引 → 预启用目标 → release/revision CAS 切换 → 关闭旧 Entity → 返回发布记录标识。

关键并发要求：CAS 失败或补偿前重新检查当前 active_revision_id；若目标已被其他成功命令发布，禁止把它关闭。补偿和 reconcile 都使用同主题锁，并以最新 MySQL 状态为准。分布式锁失效后不盲目写投影；MySQL 复核仍保护错误证据，但可能出现短暂无召回，需要可观测报警与同步对账修复。

下线先事务清空 active_revision_id（需专用 SQL 显式 SET NULL，生成选择性 update 无法承担），提交后关闭旧 Entity。

### 4.9 Biz：检索、引用与预算

| 类 | 待开发方法 | 责任 |
| --- | --- | --- |
| `service.KnowledgeRetrieveService` / Impl | `KnowledgeRetrieveVo retrieve(KnowledgeRetrieveRequest)` | 校验 Runtime 身份/组织→query Embedding→Milvus→批量 MySQL 活动复核→必要补召回→Rerank→引用/证据策略；不生成回答 |
| `service.KnowledgeActiveRevisionService` / Impl | `List<VerifiedCandidate> verifyCandidates(List<RetrieveCandidate>)` | 服务内部读取组织，批量 JOIN chunk/revision/item/release；比对 Entity ID/归属/hash/profile，全部参与表必须未删除 |
| `domain.KnowledgeRetrievePolicy` | `NormalizedRetrieve normalize(request,limits)`；`List<VerifiedCandidate> selectEvidence(candidates,policy)`；`boolean isAnswerable(evidence)` | query 长度、topK 上限、标签格式、最低分/数量/去重、同主题 priority 稳定并列；没有证据才 NO_EVIDENCE |
| `domain.KnowledgeCitationBuilder` | `Citation build(verifiedCandidate)`；`boolean isStableCitation(...)` | FILE 页码/标题路径/来源定位；FAQ 以稳定问答定位；不可引用候选剔除 |
| `domain.Deadline` | `static Deadline start(Duration)`；`Duration remaining()`；`void requireRemaining(dependency)` | 单调时钟端到端预算；每次网络、SQL、补召回、重试/Rerank 都不能重置总预算 |
| `client.KnowledgeDependencyException` | 净化的依赖名、错误码、trace | 继承/沿用 BusinessException 契约；不携带原始 provider body/请求正文作为公开 message |
| `controller.KnowledgeRetrieveExceptionAdvice` | `ResponseEntity<Result<Void>> handleDependencyUnavailable(KnowledgeDependencyException)` | 仅 scoped 到检索 Controller，高优先级复用 ExceptionProcessor，HTTP 503；不得再造全局异常 handler |

Rerank 是否必选在 P0 固定；默认按设计走 Rerank，启用时故障不能静默降级为无证据。预召回数量、最大补召回轮数、阈值、依赖重试上限均服务配置化。Rerank 后输出前再次批量复核活动版本，尽量缩短发布切换窗口；响应语义以最终 MySQL 读取时刻为准，不承诺跨系统线性一致快照。

正文从已校验的 Milvus Entity/解析产物取得，并校验内容 hash 与 MySQL 清单匹配；不能只因一个 chunkId 存在就信任该 Entity 的其余字段。查询原文、候选正文和响应证据不得进入 access/debug 日志。

### 4.10 Biz：反馈、删除、对账和重建

| 类 | 待开发方法 | 责任 |
| --- | --- | --- |
| `service.KnowledgeFeedbackCommandService` / Impl | `Long submit(KnowledgeFeedbackCreateDto)`；`void process(KnowledgeFeedbackProcessDto)`；`Long createDraftFromFeedback(Long feedbackId,KnowledgeDraftCreateDto)` | trace/revision/chunk 至少一项定位；有关联 ID 必须同组织；OPEN→PROCESSING→RESOLVED/IGNORED；修正作为草稿输入，不能直接改发布内容 |
| `service.KnowledgeDeletionService` / Impl | `void deleteKnowledge(KnowledgeDeleteDto)`；`void deleteDraft(KnowledgeDeleteDto)` | 活动主题先撤销发布权威，再逻辑删除并创建 DELETE_INDEX；禁止直接 generated delete 绕过活动指针；首期不自动物理删除历史文件 |
| `service.tx.KnowledgeDeletionTxService` | `void markDeletedAndEnqueueCleanup(...)`；`void completeDeleteIndex(TaskLease)` | 状态/逻辑删除/清理任务同事务，执行索引删除前复核不存在合法活动引用 |
| `service.KnowledgeIndexRebuildService` / Impl | `Long requestRebuild(KnowledgeRebuildDto)`；`void executeRebuild(TaskLease,TaskInputSnapshot)`；`RebuildCheckResult verifyTargetProfile(profile)` | 重用 INDEX 类型与可靠任务机制，通过持久处理输入区分重建；只重建有效且产物完整的 revision，运行 collection 不直接覆盖 |
| `service.KnowledgeRecoveryCheckService` / Impl | `RecoveryReport verifyMetadataAndFiles(Long organId)`；`RebuildCheckResult verifyActiveCoverage(Long organId,profile)` | MySQL/文件 hash/清单/release/活动索引核验；恢复编排与流量切换由运行团队负责 |

新模型/profile 的 collection 切换是 P7 的单独交付门槛：不得修改已发布 revision 的历史处理记录来假装已用新模型。先评审每 revision 的重建结果/profile 映射存储、双 profile 查询与回滚策略、全量验收及受控配置切换，再执行。若首期只需同 profile 同内容修复，复用既有 chunk ID 并按 active_revision_id 修复 searchable；不在业务服务自动创建/销毁基础设施。

### 4.11 Biz：仅新增必要的领域 DAO/SQL

所有新 DAO 放 `dao.domain`，XML 放 `resources/mybatis/mapper/`，对应 `*DomainMapper.xml`。普通 insert、selectById、列表、分页、逻辑删除和已有 updateByVersion 不重复实现。新增跨表 SQL 返回内部查询投影，不返回公开 PO。

| DAO | 必要新增方法 | SQL 原子条件 |
| --- | --- | --- |
| `KnowledgeRevisionDomainDao` | `int transitionStatus(organId,revisionId,expectedVersion,expectedStatus,targetStatus,actor,time)`；`int applyParsedResult(...)`；`int applyIndexResult(...)` | id+organ+delete_flag=0+version+前置状态；使用现有列只写领域结果，避免通用任意字段 patch |
| `KnowledgeTaskDao` | `claim(...)`、`complete(...)`、`fail(...)`、`retryFailed(...)`、`cancelPending(...)` | 专用状态 SQL 仍归属于 `knowledge_task` 的既有 DAO；0 行表示竞争失败或状态不满足 |
| `KnowledgeRetrievalDomainDao` | `List<ActiveChunkProjection> selectActiveChunks(organId,chunkIds)` | chunk→revision→item→release JOIN：所有组织一致、未删除、active 指针一致、PUBLISHED/INDEXED；批次有上限 |
| `KnowledgeTagDomainDao` | `boolean existsRevisionUsingTag(organId,tagCode)` | 在 JSON tag_code 快照中检查历史引用，不用 tag_name，不跨组织 |

反馈状态 CAS 如能由现有 updateByVersion+事务校验完成则复用，不新建反馈 CRUD DAO；只有真实并发测试证明需要状态谓词时，才增补专用 `transitionFeedbackStatus`。

### 4.12 Startup：只装配、不写业务

| 类（均在 `config`，Scheduler/Health 分别在对应包） | 方法/属性 | 责任 |
| --- | --- | --- |
| `KnowledgeProperties` | storage/upload、docling、embedding profiles、milvus、rerank、retrieve、tasks、受控 workerOrganIds | 建议前缀 `g2rain.knowledge`；映射既有环境变量；Secret 无明文默认值、不输出 toString |
| `KnowledgeClientConfiguration` | `doclingClient(...)`、`embeddingClient(...)`、`rerankClient(...)`、`milvusKnowledgeClient(...)`、`storageClient(...)` | 只装配、关闭资源；实际客户端/领域逻辑在 Biz；不给外部模型透传 principal API key |
| `KnowledgeWorkerConfiguration` | `knowledgeWorkerExecutor()`、`clock()` | 使用 ContextExecutors，设置并发上限/反压，优雅停机/停止领取/处理失租 |
| `scheduler.KnowledgeTaskScheduler` | `dispatch()`、`recoverExpired()`、`renewRunningLeases()` | 只调用 Biz 公共服务，受信组织轮转，不注入 DAO，不把领取算法放 Startup |
| `KnowledgeStartupValidator` | `validateRequiredConfiguration()`、`validateStorageRoot()`、`validateEmbeddingProfile()`、`validateMilvusSchema()` | 校验持久卷、独立 Secret、profile/模型不可变版本、4096、schema/index；调用 Biz client 验证，不管理部署 |
| `health.KnowledgeReadinessHealthIndicator` | `Health health()` | 区分可用和就绪；缺少关键依赖时生产失败/不就绪；详情无路径和 Token |

`KnowledgeMetrics` 放 Biz 的 `observability`，方法 `recordTaskOutcome(...)`、`recordDependencyLatency(...)`、`recordCandidateDiscard(...)`、`recordProjectionWarning(...)`，复用 Micrometer/OTel。低基数标记依赖、任务类型、状态，不用 query、chunkId 或无限 organId 作为指标标签。

## 5. 需要先冻结的数据增量设计

V1 已有基础结构；新增需求通过 V2+ 增量迁移，不修改已执行 V1，不重复生成现有 CRUD。以下是差距候选，字段名/DDL 在 P0 设计评审后才定稿。

| 缺失能力 | 建议最小补充 | 约束 |
| --- | --- | --- |
| 可靠任务输入 | knowledge_task 增加结构化处理输入版本/快照字段，记录 source hash、parser/chunker 版本、profile/model version、目标处理 profile | 只保存不可变定位与非 Secret 元数据，不保存正文、请求头、凭据；同幂等键输入必须一致 |
| 跨实例协作取消 | 持久 `cancel_requested` 或等效取消状态元数据 | PENDING 与 RUNNING 区分；可被其他实例观察，不能用进程内变量替代 |
| fencing | 现有 version/owner 能表达领取代次时优先复用；不足再加 lease_generation | 续租若推进 version 必须更新 lease token；所有结果写入验证相同代次 |
| 发布并发 | 请求携带 release 与 revision 的 expectedVersion | 两次提交只有一方能更新；冲突后由前端刷新状态再重新组合调用 |
| 多 profile 重建 | 每 revision 的目标 profile/产物/任务结果映射 | P7 必须先评审历史不可变性、活动路由、回滚；不为 MVP 无条件扩展复杂模型 |

新增表或新增列必须遵守 BasePo 通用列、organ_id、逻辑删除、函数唯一索引、无物理外键、IdGenerator。对原生成文件的结构变化只生成差异并审阅合并，`tables.overwrite=false`，不得自动覆盖现有业务。

## 6. 执行顺序与阶段验收

先完成 P0，再按阶段纵向交付；每次仅将正在执行的一项需求登记为 `docs/project.yaml` 的 activeRequirement，不能因为有本计划就把全部需求标记完成。

| 阶段 | 对应需求/前置 | 具体开发工作 | 阶段完成证据 |
| --- | --- | --- | --- |
| P0 基线与契约核验 | RQ-001/RQ-002，当前骨架 | 文档事实、依赖/插件关系、九组生成资产复用核查、权限链、现有接口安全问题核验、新增领域接口契约、Result/503、DTO/VO 安全审查、外部接口/profile、A01—A11 决策、增量迁移设计 | 最终依赖树、模块方向、生成资产清单；真实 MySQL 隔离/逻辑删除、已有 updateByVersion 行为验证；逻辑差距映射至新增能力，安全整改单独列出证据与范围 |
| P1 主题/草稿/FAQ/受控文件 | P0；RQ-003 | 新增 Access、Draft/Tx、Source/Tag/StateMachine、Storage、安全详情接口；仅经确认的现有接口安全问题最小修复 | 两组织互不可见/不可写；新增领域接口保证唯一 DRAFT、来源二选一、非法编辑/路径拒绝、FAQ/文件草稿写入可回查；现有通用接口无因逻辑不足产生的契约变化 |
| P2 源内容审核与 PROCESS 编排 | P1；RQ-002/003/004 | Review（源审核）、`processRevision`、task 输入/取消、Scheduler/Worker、Docling、Chunk | 仅审源文件/FAQ；approve→APPROVED；approve 后才允许 PROCESS；拒绝/失败恢复；重复提交、领取、崩溃、失租、取消测试 |
| P3 PROCESS 内 Embedding/Milvus | P2 | Embedding、Milvus V2、profile/filter、Index/对账、启动验证、领域指标 | 未审核零模型写入；同 chunkId 幂等；4096/UTF-8/tag/manifest 对账；只成功对账才 READY_TO_PUBLISH |
| P4 人工发布/回滚/下线 | P3；RQ-004 | ReleaseService、同步 reconcile | 首次发布、同主题并发 CAS、回滚、下线均使用真实事务测试 |
| P5 Runtime 检索 | P4，模型/检索基础策略可在 P3 并行准备 | Retrieve、ActiveRevision、RetrievalDomainDao、Rerank、Citation、Deadline、503 advice、Runtime 契约 | v1 完整字段、Result 序列化、NO_EVIDENCE、503、旧版/跨组织/hash异常丢弃、标签 AND/OR、总预算、稳定引用 |
| P6 反馈/删除/运营闭环 | P1—P5 | Feedback、Deletion/Tx、DELETE_INDEX worker、安全查询 | 修正仅生成草稿；逻辑删除/下线即时排除证据；未开始/运行中任务取消；索引清理可重试；历史文件保留 |
| P7 恢复与生产验收 | RQ-004，全链路可运行 | Rebuild/RecoveryCheck、profile 映射增量设计、配置切换/回滚、就绪、共享卷/备份/运行文档 | MySQL+文件同批恢复、Milvus 重建/计数/引用/组织验收、过期任务恢复、停机/重启、RPO/RTO 演练记录 |

首个可验证交付点是 P1 的安全主题/草稿，不重新交付九张表 CRUD；首个生产可用链路至少完成 P0—P5 与必要的 P7 运行验收。P6/P7 不能以“已有 CRUD”替代领域验收。

## 7. 必须覆盖的测试类与方法

| 测试类 | 关键场景/建议方法 |
| --- | --- |
| `KnowledgeArchitectureTest` | `modulesMustFollowStartupBizApi`、`apiMustNotExposePoOrBizDto`、`convertersMustNotCallDaoOrClient` |
| `KnowledgeGeneratedFoundationIT` | `crudMustRespectOrganIsolation`、`logicalDeleteMustReleaseUniqueKeys`、`staleVersionMustFail`、`missingContextMustFailClosed`；验证真实 MyBatis 插件，不使用 Mock 证明隔离 |
| `KnowledgeAccessContractIT` | `forgedPrincipalHeadersMustBeRejected`、`runtimeMustNotExecuteManagementCommands`、`generatedEndpointsMustEnforceAuthorizationAndTenantIsolation`、`queryMustNotExposeStoragePaths`；只验证现有接口安全边界，不要求其新增领域编排 |
| `KnowledgeInterfaceCompatibilityIT` | `generatedInterfaceContractsMustRemainUnchangedExceptDocumentedSecurityFixes`、`newDomainCommandsMustEnforceLifecycleRules`；现有契约兼容与新增领域规则分别验收 |
| `KnowledgeDraftServiceIT` | `onlyOneEditableDraftPerKnowledge`、`publishedRevisionMustBeImmutable`、`failedAttachmentMustNotLeaveReferencedPartialFile`、`uniqueConflictMustRollbackBeforeLookup` |
| `KnowledgeStorageSecurityTest` | `rejectTraversalAndSymlinkEscape`、`rejectMimeMismatchAndOversize`、`mustUseGeneratedFileName` |
| `KnowledgeTaskLeaseIT` | `onlyOneWorkerClaimsTask`、`expiredLeaseCanBeReclaimed`、`oldWorkerCannotCommit`、`retryMustKeepTaskIdentityAndAttemptHistory`、`runningCancellationSurvivesRestart` |
| `KnowledgeSourceReviewProcessIT` | `sourceReadyCanEnterReviewing`、`approveDoesNotCallEmbeddingOrMilvus`、`processRequiresApproved`、`rejectReturnsDraftWithoutProcessTask`、`processSuccessEntersReadyToPublish`、`replayedProcessMustPreserveChunkIdentity` |
| `KnowledgeIndexIT` | `upsertMustBeIdempotentByChunkId`、`rejectWrongDimensionOrProfile`、`incompleteManifestMustNotBecomePublishable`、`overlongUtf8ContentMustNotBeTruncated` |
| `KnowledgeReleaseConsistencyIT` | `preEnableFailureMustNotChangeRelease`、`casFailureMustCompensate`、`compensationMustNotDisableConcurrentActiveTarget`、`closeOldFailureMustKeepCommittedRelease`、`replayMustNotDuplicateAudit`、`offlineMustSetActiveRevisionToNull` |
| `KnowledgeRetrieveContractIT` | `validEvidence`、`noEvidence`、`oldRevisionAndForeignOrganDiscarded`、`unknownTagReturnsNoEvidence`、`allAnyTagsMustUseAndBetweenGroups`、`dependencyFailureMustReturn503`、`totalBudgetMustIncludeRetryAndRerank`、`evidenceHashAndCitationMustBeVerified` |
| `KnowledgeFeedbackDeletionIT` | `correctionMustCreateDraft`、`foreignReferencesMustBeRejected`、`deleteMustWithdrawReleaseAndEnqueueIndexCleanup` |
| `KnowledgeStartupIT` | `missingSecretOrStorageMustFailProductionStartup`、`wrongMilvusSchemaMustFailReadiness`、`devAndNacosProfilesMustBeVerifiedSeparately`、`asyncContextMustPropagate` |
| `KnowledgeRecoveryE2E` | `fileOrFaqFromDraftToRetrieveAndRollback`、`restoreMetadataAndFilesThenRebuildMilvus`、`collectionSwitchMustKeepTenantAndCitationGuarantees` |

真实 MySQL 测试覆盖唯一约束、事务/AOP 回滚与 CAS；真实 Milvus 测试覆盖 ARRAY 过滤、partial upsert、可见性及 profile；Docling/模型用受控契约夹具测试故障，并完成真实环境联调。测试数据不能使用生产原文、Token 或租户信息。

实施验证命令：先 `mvn dependency:tree` 核对版本，再按变更运行有意义的针对性测试，阶段交付执行 `mvn clean verify`。代码生成检查仅在新增/改变数据结构时于独立输出执行并审阅，不因本计划再次生成已存在的 CRUD。构建输出不提交 target、flattened POM、IDE 文件。

## 8. 执行前尚需确认的外部事实

这些事项在对应阶段调查并形成具体配置/契约，无须现在暂停文档交付，也不能在实现中用默认猜测替代：

1. Gateway 对主体头的清洗及服务身份验证方式，knowledge 五个能力点对应的平台权限来源；Starter 本身不包含完整的这些领域权限决策。
2. Docling Serve 部署镜像已固定为 `v1.29.0`；已确认同步 multipart `POST /v1/convert/file`、`files`、`from_formats`、`to_formats` 和可选 `X-Api-Key`，首版适配器仅请求 Markdown 与纯文本。HybridChunker 的最终输出契约、支持格式/限额仍需通过部署 OpenAPI 确认。
3. Embedding/Rerank 实际供应商协议、不可变模型版本、归一化规则、独立 Secret、服务超时/限流，以及 Rerank 必选策略。
4. Milvus 部署镜像为 `3.0.2`，后续 P3 接入时使用同版本官方 Java V2 SDK 与 `MilvusClientV2`，连接凭证使用 `MILVUS_TOKEN`；partial upsert、读写一致性、生产 profile/collection 绑定和新 profile 迁移方案仍需联调确认。
5. MySQL 实际表结构与 V1 一致性、生产迁移工具、ID 服务可用性、Redis 锁装配和 shared storage 挂载。
6. Agent Runtime 对 Result 包装、业务 snake_case、HTTP 503 的联调与版本变更顺序；不得未经协调改变已使用契约。
7. 首期部署组织集合的受信来源、共享卷权限、资源配额、备份批次与运行团队的 RPO/RTO。

## 9. 本次交付与后续文档更新

本次交付本文及 docs 入口链接。后续 P0 同步 project.yaml 的工具链/事实/验证状态，g2rain-foundation 的 Crafter/Starter 说明，以及 modules、integration-contracts、configuration、local-development、testing、requirements 状态；架构例外只在明确需要并登记后实施，不静默偏离中央规范。

## 10. 当前实施进度（2026-10-07）

### 已冻结

- DEV-001 生命周期：先审源文件，再 PROCESS。
- DEV-002 检索未知 tag：拒绝。
- DEV-003 访问：Gateway + Starter 隔离；无服务内能力点。
- Answer：`POST /knowledge/answer` 为本服务能力；排除 Runtime 会话编排。
- 主题/草稿/FAQ 元数据：运营走生成 `save`；不强制 DraftService HTTP。

### 已落地（代码）

- 三模块 + 生成 CRUD；审核 / processRevision / publish / rollback；PROCESS Worker（Docling/FAQ→切片→Embedding→Milvus）。
- Retrieve / Answer / Rerank / Milvus 客户端主路径。
- 本轮补齐：VO 路径脱敏、`replaceFile`、PENDING retry/cancel、Retrieve Deadline/503/traceId/hash。

### 本轮延期（文档承认）

- 发布顺序修正、offline、reconcile、主题分布式锁。
- 任务 `input_snapshot` / `lease_generation` / `cancel_requested` Worker 接线与 RUNNING 协作取消。
- P6 反馈删除闭环、P7 恢复验收、真实 MySQL/Milvus IT。
