# 发布记录

- [v1.0.0：知识库首个稳定版](v1.0.0.md)

源代码通过 `feature/* → develop → main` 的 PR 流程集成。准备发布时先将 `develop` 通过 Pull Request 合入 `main`，再在已验证的 main 提交创建 `v<major>.<minor>.<patch>` 标签。推送稳定版 tag 会触发 GHCR 镜像构建与发布。

发布说明区分维护者的服务器验证、本地构建和已知限制。推送稳定版 tag 时，GitHub Actions 构建并推送 GHCR 镜像，不自动创建 GitHub Release，也不自动部署。发布说明作为仓库文档维护。
