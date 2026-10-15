# 知识库领域规则

本页补充业务规则，不替代 MySQL、Milvus 或接口契约设计。规则由 Biz 层专用应用服务执行；生成的 CRUD 不能跳过这些校验。

权威生命周期与状态机以 [知识库业务流程](business-flows.md) 为准；本页补充审核对象、任务与发布约束。相对早期「先解析再审核」草案的正式调整说明见 [架构例外](deviations.md)。

## 知识主题与版本

| 规则 | 说明 |
| --- | --- |
| 稳定身份 | `knowledge_item` 是业务知识主题，`organ_id + knowledge_key` 在有效记录中唯一；文件名、文件 SHA-256 和 FAQ 文案均不是主题身份。 |
| 版本不可变 | 已进入 `REVIEWING`、`APPROVED`、`PROCESSING`、`READY_TO_PUBLISH`、`PUBLISHED` 或 `DEPRECATED` 的 revision 不可直接修改内容、来源文件、切片或标签；`PROCESS_FAILED` 可回到可编辑 `DRAFT` 或创建新草稿。已发布内容的变更必须创建新的 DRAFT revision。 |
| 草稿编辑 | 同一知识主题同一时刻最多允许一个可编辑 DRAFT；MySQL 函数唯一索引和 `version` 乐观锁共同防止重复草稿与静默覆盖。允许在另一 revision 处于 `REVIEWING` / `APPROVED` / `PROCESSING` / `READY_TO_PUBLISH` 时创建下一份草稿。 |
| 来源二选一 | `source_type=FILE` 必须有受控文件；`source_type=FAQ` 必须同时有标准问题和标准答案。两种来源不能在同一 revision 混用。 |
| 发布方式 | 首期仅支持人工立即发布、人工回滚和人工下线；不提供定时发布、自动到期或发布任务。 |
| 优先级 | `priority` 仅用于运营列表与同主题证据并列时的稳定排序；不写入 Milvus，不参与租户隔离或强制召回过滤。 |
| 分类 | `knowledge_category` 为首期扁平字典；`category_id` 仅用于运营目录与后台浏览，不参与权限和 Milvus 强制过滤。 |

## 标签治理

| 规则 | 说明 |
| --- | --- |
| 标签字典 | `knowledge_tag` 是组织内扁平字典。`tag_code` 创建时显式给出（小写字母、数字、下划线），创建后冻结；`tag_name` 可改。禁止空编码、空名称和重复有效 `tag_code`。 |
| 版本快照 | revision 保存 `tags_json` 的 `tag_code` 数组，`PROCESS` 任务复制到 Milvus Entity 的 `tags` ARRAY 字段。字典改展示名或停用不追写历史 revision。 |
| 停用 | 停用标签后不得再被新草稿选择；已有 revision 仍按其 code 快照检索，直到运营人员以新 revision 替换。 |
| 删除 | 已被任何 revision 使用的标签不执行物理删除，只能停用或逻辑删除；未使用标签可逻辑删除。 |
| 筛选语义 | `all_tags` / `any_tags` 传 `tag_code`。`all_tags` 必须全部命中，`any_tags` 至少命中一个；两组同时存在时取 AND。标签为空不限制业务范围。未知或停用 code 在新草稿选择时拒绝；**检索请求中未知 code 直接拒绝**（与接口契约及实现一致，不伪装为无证据）。 |

## 文件、解析与切片

- 文件接收只接受已配置白名单中的格式、大小和内容类型；扩展名与内容嗅探不一致时拒绝。
- 服务端生成存储路径和安全文件名。原始文件名仅作展示元数据，不能参与文件系统路径和 Milvus filter。
- 同组织内相同 SHA-256 仅用于提示重复和复用解析产物的候选；不能绕过审核、发布或创建新的知识版本。
- 切片策略、解析器版本、`embedding_profile`（及对应模型标识/版本）必须记录在 revision/任务元数据中；任一项变化导致重新处理时，创建新的 `PROCESS` 任务或新 revision，不覆盖已发布索引。
- 引用必须可追溯到 `knowledge_id`、`revision_id`、`chunk_id`、标题路径和页码或来源定位；无法生成稳定引用的 chunk 不得进入生产发布。
- 解析产物、chunk 清单与向量 Entity 属于机器中间结果，不作为人工审核对象；运营审核只针对可阅读的原始文件或 FAQ 正文与标签。

## 任务、重试与幂等

| 场景 | 规则 |
| --- | --- |
| 创建任务 | 在产生领域状态变更的 MySQL 事务中创建 `knowledge_task`；Worker 仅处理已提交任务。 |
| 领取 | Worker 以事务领取 `PENDING` 且 `available_time <= 当前UTC时间` 的任务，写入 `lease_owner`、`lease_expire_time` 和 `RUNNING`。租约到期的 `RUNNING` 任务可被安全回收。 |
| 幂等键 | 同一 revision、任务类型、输入版本（例如文件 SHA-256、解析器版本、embedding profile 或发布版本）生成稳定 `idempotency_key`。重复投递返回既有任务或安全地重放。 |
| 重试 | 网络超时、限流和暂时不可用将同一任务恢复为 `PENDING`、递增 `attempt_no` 并更新 `available_time` 进行退避；格式错误、内容不支持、维度不匹配等不可恢复错误直接 `FAILED`。 |
| 失败恢复 | `FAILED` 任务保留错误码和净化摘要。人工重试重置同一幂等任务的可领取状态并保留尝试次数，不新增冲突任务。 |
| 取消 | 仅尚未开始的任务可取消。已运行任务采用协作式取消；即使外部调用无法中断，也不得把半成品标为成功。 |
| 顺序 | 权威顺序为 `审核原文/FAQ → PROCESS`。`PROCESS` 任务在审核通过后执行，内部完成解析、切片、Embedding、Milvus staged 写入与对账。不得在审核通过前消耗 Embedding 或写入 Milvus。下游动作必须验证上游状态，不依赖队列到达顺序。 |

## 审核、发布与并发

### 审核对象与时机

- **审核只针对源内容**：FILE 审受控原始文件（及展示元数据、标签）；FAQ 审标准问题与标准答案（及标签）。
- **不审中间产物**：解析 Markdown/JSON、chunk 正文与定位清单、Embedding 向量、Milvus Entity 等由 Worker 与对账规则保证质量，不进入人工审核界面。切片与索引结果对审核人不可读、不可操作，要求其二次审核既不现实，也缺少业务闭环。
- 提交审核的前置条件是来源文件或 FAQ 已就绪，且标签与来源可审；**不要求**已解析、已切片或已索引。
- 草稿提交后 revision 进入 `REVIEWING`。运营执行通过/拒绝时，状态必须仍为 `REVIEWING`。
- 审核通过进入 `APPROVED`；此后才允许发起 `PROCESS` 任务并进入 `PROCESSING`。审核拒绝回到可编辑 `DRAFT`，必须填写可见于运营端的净化原因。
- `PROCESS` 成功且对账通过后进入 `READY_TO_PUBLISH`；失败进入 `PROCESS_FAILED`，可退回 `DRAFT` 修正后重新提交审核与处理。

### 发布与并发

- 发布请求必须携带当前 `knowledge_release.version` 或等效预期版本，防止过期页面把更新后的发布记录覆盖。
- 同一 `knowledge_item` 的发布、回滚、下线串行化；不同知识主题可以并行发布。
- 发布、回滚和下线是同步领域命令，不创建 `knowledge_task`。Milvus 更新必须在 MySQL 发布事务之外执行，并以 MySQL 活动 revision 复核作为最终安全边界。
- 回滚只能选择同组织、同知识主题、已完成索引校验且未逻辑删除的历史 revision。
- 下线先撤销 MySQL 活动版本，再同步关闭 Milvus Entity。首期不自动物理删除原文件或解析产物。
- 发布动作以 release 与 revision 的乐观锁版本判断并发冲突；前端刷新状态后再重新发起动作。

## 检索可回答判定

`answerable` 是 Knowledge Service 对“是否存在足够、有效证据”的判断，不是大模型回答质量承诺。至少同时满足：

1. 调用请求合法，并具有可信 `organ_id`；
2. 至少一个候选通过 Milvus 的 `organ_id`、`searchable=true`、标签条件过滤；
3. 至少一个候选通过 MySQL 对 chunk 未删除、组织一致与活动 revision 的复核；
4. Rerank 后满足配置化的最低证据策略，例如最低分、最少证据数或来源去重规则；
5. 每个返回证据均能产生稳定引用。

预算、候选数量、分页轮数和超时均为运行配置，不应硬编码在 Controller 或对外接口中。缺少证据或复核后没有有效候选时返回 `answerable=false` 与 `NO_EVIDENCE`；依赖超时或不可用返回依赖错误，不伪装为无证据。

## 反馈闭环

- 反馈必须关联 trace、revision 或 chunk 中至少一个可定位信息；匿名完整对话不进入知识库表。
- `LOW_SCORE`、`WRONG_ANSWER`、`MANUAL_TAKEOVER`、`CORRECTED` 是反馈事实；`OPEN → PROCESSING → RESOLVED / IGNORED` 是运营处理状态。
- 人工修正不自动覆盖已发布知识，而是作为创建草稿 revision 或 FAQ 的输入；发布仍需经过完整审核与处理流程。
- 运营看板应按组织、标签、知识主题、revision、chunk 和反馈类型聚合，不跨组织汇总原始内容。

## 访问与隔离

- **入口**：凡经 Gateway 可达本服务接口的调用方，均可访问**本租户**知识数据与领域命令（含检索、受控问答与运营写路径）。
- **隔离**：统一采用 Starter `@DataIsolation` 与主体上下文中的 `organ_id`；Biz 侧可做 `requireOrganId` / `assertSameOrgan` 防御性断言。
- **不做**：本服务内不落地 `knowledge.read/edit/review/publish/operate` 等细粒度能力点门禁，也不另建第二套租户隔离或业务 `WithoutIsolation` 路径。
- 细粒度 IAM 角色若需要，由 Gateway / 管理端在入口侧控制，不替代 Starter 数据隔离。
