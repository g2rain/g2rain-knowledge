# 架构决策记录

尚未登记项目 ADR。以下决定已作为当前设计基线，需要在实现选择变化时建立单独 ADR：

- MySQL 是知识元数据与发布状态的权威来源，Milvus 是可重建派生索引。
- 本地持久化磁盘保存原文件与解析产物，不引入 MinIO、OSS 或 RAGFlow。
- Docling、Embedding、Rerank 和 Milvus 是内部/外部依赖，不向 Agent Runtime 直接暴露。
- 发布前完成索引；首期仅支持人工同步发布，发布命令在 MySQL 事务外维护 Milvus `searchable`，并以 MySQL 活动 revision 作为唯一权威。
