# 接口与集成契约

## Agent Runtime 检索接口

```http
POST /knowledge/retrieve
```

契约版本为 `v1`。请求必须携带可被 `PrincipalContext` 解析的受信服务身份、`organ_id` 和 trace；`organ_id` 由该上下文派生，不接受请求体传入。Agent Runtime 只能调用检索用例，不能调用运营管理命令。

标签是可选业务筛选条件，不承担权限或数据隔离职责。请求中的 `allTagCodes` / `anyTagCodes` 必须是组织内已存在的 `tag_code`，服务端校验后构造 Milvus filter：

```json
{
  "query": "会员退款多久到账？",
  "allTagCodes": ["after_sales", "x100"],
  "anyTagCodes": ["refund", "arrival"],
  "topK": 5
}
```

| 字段 | 规则 |
| --- | --- |
| `query` | 必填，去除首尾空白后不得为空；最大长度由服务配置限制 |
| `allTagCodes` | 可选，全部为 `tag_code`，必须全部命中；重复值去重 |
| `anyTagCodes` | 可选，全部为 `tag_code`，至少命中一个；与 `allTagCodes` 同时存在时为 AND 关系 |
| `topK` | 可选，默认值与最大值由服务配置控制，超出最大值直接拒绝 |
| `includePublic` | 可选 Boolean；缺省 / null / false 仅本机构；true 时额外召回 `visibility=PUBLIC` 的跨机构活动版本（RQ-008） |

调用方不得传入 `organ_id`、revision、collection、Milvus filter、模型、分数阈值或本地文件定位。

响应仅返回可引用的已发布证据：

```json
{
  "answerable": true,
  "trace_id": "trace_xxx",
  "sources": [{
    "knowledge_id": "20001",
    "revision_id": "30088",
    "chunk_id": "900123",
    "content": "退款将在 1—3 个工作日原路退回。",
    "title": "退款政策",
    "section": "退款到账时间",
    "page": 3,
    "version": "3.0",
    "citation": "退款政策 v3.0，第 3 页",
    "score": 0.92
  }]
}
```

`answerable=true` 仅表示存在通过组织隔离、MySQL 活动 revision、证据策略和引用完整性校验的候选。无有效证据时返回 HTTP 200 与 `answerable=false`：

```json
{
  "answerable": false,
  "trace_id": "trace_xxx",
  "reason_code": "NO_EVIDENCE",
  "sources": []
}
```

| 情况 | 协议语义 |
| --- | --- |
| 请求参数不合法或缺少可信主体 | 使用 G2rain 统一参数/鉴权错误，不返回检索结果 |
| 没有通过校验的证据 | HTTP 200，`answerable=false`，`reason_code=NO_EVIDENCE` |
| Milvus、Embedding、Rerank 或必要 MySQL 复核超出预算或不可用 | HTTP 503，使用统一依赖不可用错误与 `trace_id`；不得伪装为“无知识” |
| 候选仅来自旧版本、跨组织或无法引用的 chunk | 服务丢弃候选；若无剩余证据，按 `NO_EVIDENCE` 返回 |

端到端时间预算由 `RETRIEVE_TOTAL_TIMEOUT_MS` 配置。服务在预算内为瞬时网络错误执行受限重试；Agent Runtime 仅在自身总预算仍允许时重试 HTTP 503，并保持同一 trace 关联。契约测试至少覆盖有效证据、无证据、旧版本、跨组织、标签筛选、依赖超时和引用定位。

## 知识问答接口

```http
POST /knowledge/answer
```

请求继承检索字段，并可选择服务端固定模板：

```json
{
  "query": "会员退款多久到账？",
  "allTagCodes": ["after_sales"],
  "topK": 5,
  "templateCode": "CUSTOMER_SERVICE"
}
```

`templateCode` 只用于从当前机构的 `knowledge_answer_template` 中获取 `ACTIVE` 回复模板，不代表模型；省略时查询编码为 `DEFAULT` 的模板。服务先完成组织、标签和活动版本复核，经 Model Nexus `POST /v1/embeddings` 向量化与 `POST /v1/rerank` 重排可信文本块；只有重排序后仍有有效证据才调用 Model Nexus `POST /v1/chat/completions`。模型只能收到重排序后的可信证据、数据库模板和问题，不能由调用方指定模型或提交提示词正文。没有证据时不调用模型，返回固定的无证据回复以及空 `sources`。

## 运营管理接口边界

运营管理接口经 App / Gateway 访问，使用受信主体上下文和领域能力校验。以下是目标用例矩阵，不代表现有 HTTP 实现或最终 URL 已冻结。

| 用例 | 建议命令或查询 | 关键规则 |
| --- | --- | --- |
| 知识主题 | 创建、分页查询、查看详情 | `knowledge_key` 仅在有效 `organ_id` 内唯一 |
| 草稿版本 | 创建 revision、上传来源文件、编辑 FAQ/标签、查看处理状态 | 已发布版本不可直接编辑；写入使用乐观锁 |
| 标签字典 | 创建、查询、停用、排序、改展示名 | 扁平标签；`tag_code` 创建后冻结；停用或改名不改写历史 revision |
| 处理任务 | 审核通过后发起 `PROCESS`、查看任务、人工重试、取消未开始任务 | 外部调用由 Worker 执行，操作具备幂等键；未审核不得处理 |
| 审核 | 提交审核、通过、拒绝 | 来源就绪后进入 `REVIEWING`；仅审原始文件/FAQ 与标签；通过后进入 `APPROVED` 才可发起 `PROCESS`；拒绝回 `DRAFT` 并记录原因 |
| 发布 | 人工发布、回滚、下线、索引对账 | 以预期 release 与 revision 版本防止并发覆盖；同步维护 Milvus 派生状态；不创建发布任务 |
| 反馈 | 创建反馈、查询、处理、据此创建草稿 | 修正不直接覆盖已发布 revision |

管理接口不直接暴露 Milvus collection、Entity、存储路径、模型服务地址或通用 Mapper/Service CRUD。具体请求/响应字段、错误码和 IAM 资源映射在选定实施需求后冻结。

## 外部依赖

| 依赖 | 契约边界 |
| --- | --- |
| Docling Serve | FILE（PDF/Word/PPT/Excel/MD/TXT/HTML）经同步 `POST /v1/chunk/hybrid/file` 做结构感知切片；契约摘要见 [docling-serve-chunk-v1.29.md](docling-serve-chunk-v1.29.md)。FAQ 不调用 Serve |
| Embedding API | 基于配置的模型生成向量；记录模型标识、耗时和失败类别，不记录敏感原文 |
| Rerank API | 对限定候选集重排；不可绕过租户/版本过滤 |
| Milvus | 仅写入或查询已获准的 chunk Entity；服务端强制组织、发布投影和标签过滤 |

这些接口的认证方式、超时、重试、幂等键、错误码和版本兼容策略尚未在实现中确定，须在对应需求中补充后再发布。
