# Milvus visibility 原地加字段（RQ-008）

## 步骤

1. 对运行中的知识 collection 增加标量字段 `visibility`（VARCHAR，取值 PRIVATE/PUBLIC）。
2. 存量 Entity 回填默认 `PRIVATE`（或等价默认）。
3. 按 MySQL `knowledge_item.visibility` + 活动 revision 的 chunk 对账写回真实值。
4. 对账通过后再部署带 `includePublic`/公共召回的后端，并开放前端公共入口。
5. 切换写路径前停用会写出无 `visibility` 字段的旧实例。

## 回退

先关闭公共读与 visibility 同步写任务，再回退服务；字段可保留。
