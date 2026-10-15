# 安全边界

- 调用方、上传文件、查询条件、Docling 结果和外部模型响应都视为不可信输入。
- **入口鉴权**：假定请求经 Gateway；凡可达本服务接口的主体可访问本租户知识接口。
- **租户隔离**：统一采用 Starter `@DataIsolation` 与 `PrincipalContext` 中的 `organ_id`；每次 MySQL、磁盘与 Milvus 查询必须落实组织隔离。本服务不另建第二套隔离机制，也不在业务路径使用 `WithoutIsolation`。
- Milvus 查询强制过滤 `organ_id` 与派生的 `searchable = true`，候选结果还必须由 MySQL 校验为活动 revision；不能信任客户端提供的组织、revision 或 collection。
- 上传前校验大小、格式、扩展名、MIME 类型、内容嗅探结果和恶意文件风险；解析在最小权限的隔离执行环境中完成。
- `KNOWLEDGE_STORAGE_ROOT` 下只使用服务生成的标识与净化文件名；拒绝绝对路径、路径穿越和符号链接逃逸。查询 VO 不得返回内部存储路径或解析供应商标识。
- Milvus Token、数据库凭据、模型密钥及内部服务 Token 只能通过 Secret 注入，不得写入代码、文档、镜像层或日志。
- 日志与审计记录 trace、状态、模型标识和失败分类，不记录完整原文、文件内容、认证头或 Secret。
- 发布、回滚、删除和跨范围变更应记录操作者、租户、对象、revision、时间和原因。
