# 安全边界

- 调用方、上传文件、查询条件、Docling 结果和外部模型响应都视为不可信输入。
- **入口鉴权**：假定请求经 Gateway；凡可达本服务接口的主体可访问本租户知识接口。
- **租户隔离**：统一采用 Starter `@DataIsolation` 与 `PrincipalContext` 中的 `organ_id`；每次 MySQL、磁盘与 Milvus 查询必须落实组织隔离。本服务不另建第二套隔离机制。业务路径默认不使用 `WithoutIsolation`。已批准例外：（1）异步 Worker 按已提交的 `taskId` 读取持久化 `organ_id`，读完后必须在仅含该机构的上下文中继续；（2）RQ-008 / DEV-004：使用端专用 Domain 只读——已知 ID 复核按「本机构 OR（公共访问且 `visibility=PUBLIC` 活动已发布）」裁决；公共列表/分类分页以 `WithoutIsolation` 执行，但 SQL 必须写死 PUBLIC、活动发布、未删除等谓词，list 与 count 一致。禁止管理 CRUD、生成 CRUD 无隔离分页、写路径、跨机构原文件下载；本机构页与公共页分开展示、分 API。
- Milvus 查询强制过滤 `searchable = true`，并在本机构或（`includePublic` 时）`visibility=PUBLIC` 的共享条件下召回；候选结果还必须由 MySQL 校验为活动 revision 与可见性；不能信任客户端提供的组织、revision 或 collection。
- 上传前校验大小、格式、扩展名、MIME 类型、内容嗅探结果和恶意文件风险；解析在最小权限的隔离执行环境中完成。
- `KNOWLEDGE_STORAGE_ROOT` 下只使用服务生成的标识与净化文件名；拒绝绝对路径、路径穿越和符号链接逃逸。查询 VO 不得返回内部存储路径或解析供应商标识。
- Milvus Token、数据库凭据、模型密钥及内部服务 Token 只能通过 Secret 注入，不得写入代码、文档、镜像层或日志。
- 日志与审计记录 trace、状态、模型标识和失败分类，不记录完整原文、文件内容、认证头或 Secret。
- 发布、回滚、删除和跨范围变更应记录操作者、租户、对象、revision、时间和原因。
