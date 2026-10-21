<p align="center">
  <img src="https://github.com/g2rain.png" alt="G2Rain" width="180" />
</p>

# g2rain-knowledge

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-437291?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)

G2rain 的知识库服务，为客服和 Agent 提供可追溯的知识接入、版本管理、受控发布、检索证据与受控问答能力。

服务以 MySQL 作为知识元数据和发布状态的权威来源，以 Milvus 作为可重建的向量检索投影。调用方只能获得经组织隔离、活动版本和引用完整性校验后的证据，不接触存储路径、向量集合或模型供应商接口。

[官网](http://www.g2rain.com) · [完整文档](docs/index.md) · [接口与集成契约](docs/api/integration-contracts.md) · [架构说明](docs/architecture/overview.md) · [Issues](https://github.com/g2rain/g2rain/issues) · [Discussions](https://github.com/g2rain/g2rain/discussions)

## v1.0.0 稳定版

维护者已确认服务器端验证通过；2026-10-19 本地完整 Maven 构建与 40 个测试通过。配套使用 [g2rain-knowledge-app v1.0.0](https://github.com/g2rain/g2rain-knowledge-app/releases/tag/v1.0.0)。版本范围、配置与延期项见[发布说明](docs/releases/v1.0.0.md)。

## v1.0.0 稳定版

维护者已确认服务器端验证通过；2026-10-19 本地完整 Maven 构建与 40 个测试通过。配套使用 [g2rain-knowledge-app v1.0.0](https://github.com/g2rain/g2rain-knowledge-app/releases/tag/v1.0.0)。版本范围、配置与延期项见[发布说明](docs/releases/v1.0.0.md)。

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

2026-10-19 本地 `mvn clean verify` 通过，40 个测试无失败、错误或跳过；维护者另确认已在服务器端验证通过。服务器验证与本地测试分开记录，完整并发、恢复与延期项见[发布说明](docs/releases/v1.0.0.md)。

## 文档

- [工程文档入口](docs/index.md)
- [项目事实与验证状态](docs/project.yaml)
- [知识生命周期与数据模型](docs/architecture/knowledge-lifecycle.md)
- [领域规则与安全边界](docs/architecture/domain-rules.md)
- [接口与集成契约](docs/api/integration-contracts.md)
- [配置、部署与恢复](docs/operations/configuration.md)
- [开发计划与未完成项](docs/development/implementation-plan.md)
- [社区与贡献](docs/community.md)

## 边界说明

- 本服务不负责 Agent Runtime 的会话编排或多轮对话状态。
- MySQL 的活动版本裁决优先于 Milvus 检索结果；Milvus 可由权威数据重建。
- Gateway 负责入口身份校验，本服务基于受信主体上下文与 `organ_id` 执行数据隔离。
- 外部基础设施的生命周期、模型训练与模型账号管理不属于本服务职责。

## 贡献

欢迎通过 Issue、Discussion 和 Pull Request 参与 g2rain 建设。

代码贡献前请尽量补充必要的测试和文档，并确保构建、测试与静态检查通过。提交代码时，请同步更新受影响的 `docs` 文档；新增或改变长期架构决策时，在 `docs/decisions` 中增加 ADR。

提交前至少执行：

```bash
mvn clean verify
```

更多 Agent 与贡献工作约束见 [AGENTS.md](AGENTS.md) 与 [社区与贡献](docs/community.md)。

## 许可证

本项目基于 [Apache 2.0 许可证](LICENSE) 开源。

## 联系我们

- 官网：[http://www.g2rain.com](http://www.g2rain.com)
- Issues：[GitHub Issues](https://github.com/g2rain/g2rain/issues)
- 讨论：[GitHub Discussions](https://github.com/g2rain/g2rain/discussions)
- 邮箱：[g2rain_developer@163.com](mailto:g2rain_developer@163.com)

## 致谢

感谢所有为 g2rain 项目提交 Issue、代码、文档、建议和使用反馈的开发者们！
