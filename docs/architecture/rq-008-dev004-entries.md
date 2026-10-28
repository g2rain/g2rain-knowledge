# RQ-008 / DEV-004 专用只读入口清单

状态：实施规格。关联 [RQ-008](../requirements/RQ-008-public-knowledge-sharing.md)、[DEV-004](deviations.md)。

## 1. 已知 ID 复核

| 方法（计划） | 输入 | 输出 | 隔离 |
| --- | --- | --- | --- |
| `KnowledgeRetrievalDomainDao.selectActiveChunksByIds(chunkIds)` | 已知 `chunkId` 集合（有上限） | chunk+revision+item+release 投影 | `WithoutIsolation`；业务裁决 |
| `KnowledgePublicReadService.verifyForRetrieve(organId, includePublic, candidates)` | 召回候选 | 通过可见性的证据 | 调用上表 |

裁决：`delete_flag=0` ∧ 活动已发布/INDEXED ∧（`item.organ_id = 调用机构` **或**（`includePublic` ∧ `item.visibility=PUBLIC`））。

## 2. 公共目录分页

| 方法（计划） | 谓词（SQL 写死，客户端不可取消） |
| --- | --- |
| `KnowledgePublicCatalogDomainDao.selectPage` / `count` | `item.visibility='PUBLIC'` ∧ 各表未删除 ∧ `release.active_revision_id = revision.id` ∧ `revision` PUBLISHED+INDEXED ∧ `item.status=ACTIVE` |
| `selectCategoriesWithPublic` | 同上，按 `category_id`/`organ_id` 聚合，仅有可见公共知识的分类 |

list 与 count **同一谓词**。筛选仅：`categoryId`、关键词、分页、安全排序。

## 3. 禁止

生成 CRUD 无隔离分页；Service 散落 `*WithoutIsolation`；写路径；原文件跨机构下载；本机构管理列表改走公共 DAO。
