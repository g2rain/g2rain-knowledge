# 发布记录

- [v1.0.0：知识库首个稳定版](v1.0.0.md)
- [v1.0.1：知识库维护版本](v1.0.1.md)

源代码通过 `feature/* → develop → main` 的 PR 流程集成。合入 main 后，在已验证的 main 提交创建 `v<major>.<minor>.<patch>` 标签；发布工作流校验标签属于 main、存在同名发布说明后创建 GitHub 稳定 Release。

发布说明区分维护者的服务器验证、本地构建和已知限制。推送稳定版 tag 时，GitHub Actions 构建并推送 GHCR 镜像，不自动创建 GitHub Release，也不自动部署。发布说明作为仓库文档维护。
