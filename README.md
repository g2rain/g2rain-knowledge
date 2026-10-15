# g2rain-knowledge

G2rain 的知识库服务，为客服和 Agent 提供可追溯的知识接入、版本管理、受控发布、检索证据与受控问答能力。

服务以 MySQL 作为知识元数据和发布状态的权威来源，以 Milvus 作为可重建的向量检索投影。调用方只能获得经组织隔离、活动版本和引用完整性校验后的证据，不接触存储路径、向量集合或模型供应商接口。

## 当前能力

- 知识文件与 FAQ 的接收、版本、标签、审核、发布、回滚和审计；
- Docling 解析、内容切分、向量化、索引与检索编排；
- `POST /knowledge/retrieve`：返回通过校验的证据与引用；
- `POST /knowledge/answer`：仅基于可信证据和服务端模板生成受控回答；
- 基于 `organ_id` 的组织隔离，以及检索超时、依赖不可用和无证据等边界处理。

完整的接口字段、错误语义和调用边界见[接口与集成契约](docs/api/integration-contracts.md)。

## 工程结构

```text
g2rain-knowledge-api       对外稳定模型与检索/问答契约
g2rain-knowledge-biz       领域服务、管理接口、持久化与外部适配器
g2rain-knowledge-startup   Spring Boot 启动与运行时装配
docs/                      架构、需求、配置、测试与运行文档
```

模块依赖方向为 `startup → biz → api`。公共模型、主体上下文、统一返回和生成 CRUD 均复用 G2rain 基础能力；领域命令不以通用 CRUD 替代生命周期和发布语义。

## 快速开始

### 前置条件

- JDK 25
- Maven 3.9+
- 可用的 MySQL、Milvus、Docling 与模型服务配置

运行前，请根据[配置说明](docs/operations/configuration.md)通过环境变量、Nacos 或其他受控配置方式提供存储路径、数据库、Milvus 和外部服务凭据。不要将任何密码、Token 或真实知识内容写入仓库。

### 构建与测试

```bash
mvn clean verify
```

也可只执行领域模块及其依赖的测试：

```bash
mvn -pl g2rain-knowledge-biz -am test
```

2026-10-08 的完整构建已通过，Biz 模块 19 个单元测试全部通过。真实 MySQL/Milvus、Docling、模型服务与部署环境的集成验证仍在进行中；构建通过不代表这些外部依赖已经验收。

## 文档

- [工程文档入口](docs/index.md)
- [项目事实与验证状态](docs/project.yaml)
- [知识生命周期与数据模型](docs/architecture/knowledge-lifecycle.md)
- [领域规则与安全边界](docs/architecture/domain-rules.md)
- [接口与集成契约](docs/api/integration-contracts.md)
- [配置、部署与恢复](docs/operations/configuration.md)
- [开发计划与未完成项](docs/development/implementation-plan.md)

## 边界说明

- 本服务不负责 Agent Runtime 的会话编排或多轮对话状态。
- MySQL 的活动版本裁决优先于 Milvus 检索结果；Milvus 可由权威数据重建。
- Gateway 负责入口身份校验，本服务基于受信主体上下文与 `organ_id` 执行数据隔离。
- 外部基础设施的生命周期、模型训练与模型账号管理不属于本服务职责。

更多贡献与工作约束见 [AGENTS.md](AGENTS.md)。
