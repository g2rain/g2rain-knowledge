# 本地开发准备

当前已具备 Maven 三模块工程和生成 CRUD，知识领域实现正在进行。可先运行 `mvn -pl g2rain-knowledge-biz -am test` 验证不依赖外部服务的领域规则与存储边界；完整构建命令为 `mvn clean verify`。

启动或联调前仍应明确：

1. Java 版本、构建工具和模块结构；
2. 本地 MySQL、Docling、Milvus 与模型 API 的受控开发配置；
3. 可丢弃的本地知识存储根目录；
4. Mock 策略与真实外部依赖的集成测试环境；
5. Secret 注入方式，禁止将开发密钥提交到仓库。

上述选择确定后，同步更新 `docs/project.yaml`、本页和测试策略。文件草稿功能要求通过 `KNOWLEDGE_STORAGE_ROOT` 提供受控持久化目录；未配置时服务会拒绝文件写入。数据库与 Nacos 凭据必须由环境变量或 Secret 注入；代码生成所需的本地账户写入未跟踪的 `codegen.local.properties`，并在执行生成时显式指定该配置文件。
