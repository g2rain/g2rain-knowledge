# 部署与运行要求

`g2rain-deploy` 负责在内部网络提供 MySQL、Docling Serve 和 Milvus。Knowledge Service 应与持久化知识文件卷一起部署；文件卷、MySQL 与 Milvus 数据目录必须纳入同一备份与恢复策略。多副本时所有实例必须挂载同一 `KNOWLEDGE_STORAGE_ROOT` 持久化卷（或等价共享存储），任务 Worker 凭租约串行化同一 revision 的文件与索引操作。

预期的内部地址为：

```text
Docling: http://docling-serve:5001
Milvus: http://milvus:19530
MySQL: mysql:3306
```

Docling 与 Milvus 不开放宿主机端口。Milvus 采用单容器 Standalone、嵌入式 etcd、Woodpecker 本地 WAL 和本地持久化存储，不引入 MinIO 或外部消息队列。Knowledge Service 启动时必须校验 Milvus 可达性和 Token，确认目标 collection 的 `content_vector` 为 4096 维且与 embedding profile 匹配，并确认向量与标量索引齐备。部署定义位于 `g2rain-deploy`，上线前仍须完成资源限制、备份恢复和滚动发布验证。
