# 发布记录

- [v1.0.0：知识库首个稳定版](v1.0.0.md)

源代码通过 `feature/* → develop → main` 的 PR 流程集成。合入 main 后，在已验证的 main 提交创建 `v<major>.<minor>.<patch>` 标签；发布工作流校验标签属于 main、存在同名发布说明后创建 GitHub 稳定 Release。

发布说明区分维护者的服务器验证、本地构建和已知限制。GitHub Release 不等同于自动部署，也不代表向 npm、Maven 或镜像仓库推送制品；这些渠道如需发布，另按其制品与凭证要求执行。
