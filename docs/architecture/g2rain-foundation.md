# G2rain 公共基线与代码生成约束

`g2rain-knowledge` 的 Java 实现必须继承 G2rain 的公共模型、主体上下文和数据库代码生成规范。此约束优先于本仓库自行约定的实体、CRUD 或租户处理方式；不得为知识库服务另建一套重复基础框架。

## 必须复用的基础能力

| 场景 | 必须使用 | 约束 |
| --- | --- | --- |
| 数据持久对象 | `com.g2rain.common.model.BasePo` | 所有生成的 PO 继承该类，复用 `id`、创建/更新时间与乐观锁 `version` |
| 接口入参/出参 | `BaseDto`、`BaseVo` 及生成器对应类型 | 保持 G2rain 统一 ID 和时间格式；不得将 PO 直接作为对外契约 |
| 主键生成 | `com.g2rain.common.id.IdGenerator` | 使用服务注入的实现生成 `Long` ID，禁止 `AUTO_INCREMENT` 或私有 ID 算法 |
| 请求主体与机构上下文 | `PrincipalContext` / `PrincipalContextHolder` | `organ_id` 只能从受信上下文获得，禁止由请求体、Milvus Entity 标量字段 或前端传入值决定 |
| DTO 校验 | `CreateGroup`、`UpdateGroup`、`Validations` | 新增与更新使用 G2rain 的校验分组和错误契约 |
| 错误与返回 | `BusinessException`、`ErrorCode`、`Result` | 不自行定义与平台冲突的统一返回或异常体系 |

## MySQL CRUD 生成

所有 `knowledge_*` 表的基础 PO、DTO、VO、Converter、Mapper、DAO、Service、ServiceImpl 和 Controller 骨架，必须通过 `g2rain-generator-maven-plugin` 从已执行的 MySQL DDL 生成。生成器配置的基线如下：

```properties
database.tables=knowledge_category,knowledge_tag,knowledge_item,knowledge_revision,knowledge_chunk,knowledge_task,knowledge_release,knowledge_feedback
tables.overwrite=false
data.isolation.withIsolation=true
data.isolation.tenantColumns=organ_id
```

- `organ_id` 会被识别为隔离列，生成的 CRUD 必须保留组织隔离；业务层仍需从 `PrincipalContext` 注入该值，不得信任客户端参数。
- `delete_flag` 会被识别为逻辑删除字段；查询、更新和删除必须沿用生成器规则及本项目的动态函数索引设计。
- `tables.overwrite=false` 是默认安全策略。已有非空生成文件不应被自动覆盖；更新表结构后由开发者审阅差异并合并生成结果。
- 数据库连接只授予元数据读取和开发环境所需权限；密码只能来自本地 Secret 或 CI Secret，不得提交配置文件。

## 领域扩展边界

生成器负责通用的表级 CRUD，不负责知识库领域语义。以下能力必须位于生成代码之上的领域应用服务/适配器中，不能通过修改生成器模板或绕开基础类实现：

- 文件校验、受控本地存储、Docling 解析与 HybridChunker 切片；
- Embedding、Milvus upsert、索引重建与失败重试；
- revision 状态机、审核、发布、回滚和 `knowledge_release.active_revision_id` 裁决；
- 向量检索、MySQL 活动版本复核、Rerank、证据引用与反馈闭环。

生成的 Controller 仅作为运营后台基础 CRUD 的起点。Agent Runtime 的 `/knowledge/retrieve` 和发布等领域命令必须使用专用接口与应用服务，不能直接暴露通用 CRUD。

## 开发前置与验收

1. 确认 `g2rain-common` 和 `g2rain-generator-maven-plugin` 的兼容版本，并写入 `dependencyManagement` 与 Maven plugin 配置；不得猜测版本号。
2. 在开发 MySQL 执行本仓库迁移脚本后，以 `tables.overwrite=false` 执行生成器。
3. 人工审查生成结果的包名、隔离条件、逻辑删除、字段类型、校验和公开接口；生成代码不是领域正确性的证明。
4. 在生成结果基础上实现领域扩展，并执行构建、测试及租户隔离、逻辑删除、发布/回滚和检索复核测试。

参考实现来源：`g2rain-common` 的公共模型与上下文契约，以及 `g2rain-generator-maven-plugin` 的生成契约和 `codegen.properties`。当前工程已包含 API、Biz、Startup 三个 Maven 模块；实际依赖版本以根 `pom.xml` 及构建解析结果为准。`mvn clean verify` 已于 2026-10-21 通过，但这不替代真实数据库、组织隔离和外部依赖的集成验证。
