# 部署与运行要求

`g2rain-deploy` 负责在内部网络提供 MySQL、Docling Serve 和 Milvus。Knowledge Service 应与持久化知识文件卷一起部署；文件卷、MySQL 与 Milvus 数据目录必须纳入同一备份与恢复策略。多副本时所有实例必须挂载同一 `KNOWLEDGE_STORAGE_ROOT` 持久化卷（或等价共享存储），任务 Worker 凭租约串行化同一 revision 的文件与索引操作。

预期的内部地址为：

```text
Docling: http://docling-serve:5001
Milvus: http://milvus:19530
MySQL: mysql:3306
```

Docling 与 Milvus 不开放宿主机端口。Milvus 采用单容器 Standalone、嵌入式 etcd、Woodpecker 本地 WAL 和本地持久化存储，不引入 MinIO 或外部消息队列。Knowledge Service 启动时必须校验 Milvus 可达性和 Token，确认目标 collection 的 `content_vector` 为 4096 维且与 embedding profile 匹配，并确认向量与标量索引齐备（含 RQ-008 的 `visibility`）。部署定义位于 `g2rain-deploy`，上线前仍须完成资源限制、备份恢复和滚动发布验证。

## 容器镜像发布（GHCR）

镜像地址为 `ghcr.io/g2rain/g2rain-knowledge:<tag>`，对应 GitHub 仓库 `g2rain/g2rain-knowledge`。

### GitHub Actions 自动发布（推荐）

工作流位于 `.github/workflows/image.yml`。先将该文件及配套 Jib 配置合入默认分支；仅推送稳定版 tag 时触发，普通分支推送不触发，也没有手动运行入口。工作流只构建并推送容器镜像，不创建 GitHub Release。

- 推送稳定版标签（例如 `v1.0.1`）：自动测试、构建并发布 `1.0.1` 与 `sha-<完整提交 SHA>`。标签提交必须属于 main 历史，且存在对应的 `docs/releases/v1.0.1.md`。
- 不自动更新 `latest`；部署使用明确的版本标签。

例如，提交本次发布代码和 `docs/releases/v1.0.1.md` 后执行：

```bash
git tag v1.0.1
git push origin v1.0.1
```

工作流使用 Ubuntu、Temurin JDK 25 与 Maven，先执行 `clean install`（含测试），成功后通过 Jib 上传；依赖从 Maven Central 解析，不依赖开发机本地 Maven 缓存。镜像默认使用 Jib 的 Linux amd64 平台。

认证使用 GitHub 自动提供的 `GITHUB_TOKEN`，工作流已声明 `contents: read`、`packages: write`，无需添加 GHCR PAT Secret。仓库/组织须允许 Actions 创建和写入容器包；已有包须授予本仓库 Actions 写入权限。

首次镜像推送后，将包改为公开，供部署服务器匿名拉取：打开 GitHub 仓库页面右侧 **Packages → g2rain-knowledge**，进入包页面的 **Package settings → Change visibility → Public**，输入包名确认。公开设置对后续推送到同一包的版本持续生效，一般只需操作一次。若组织管理员禁用了可见性修改，联系管理员开放修改权限或代为设置。公开后的镜像任何人均可拉取，请勿向镜像中打包密钥、令牌或私有数据。权限说明见 [GitHub Actions 包发布文档](https://docs.github.com/en/packages/managing-github-packages-using-github-actions-workflows/publishing-and-installing-a-package-with-github-actions)。

发布成功后在对应运行摘要中查看镜像标签，并配置下文的 `KNOWLEDGE_IMAGE`。工作流只发布镜像，不会自动更新运行中的服务。

### 本地手动发布

也可在具备 JDK 25、Maven 3.9+ 与项目依赖的构建机器上手动发布。

先使用有目标组织包写入权限的 GitHub 账号登录。`GHCR_TOKEN` 通过当前终端环境安全注入，使用具备 `write:packages` 的 PAT (classic)，不要写入仓库或直接放在命令参数中：

```bash
printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
unset GHCR_TOKEN
bash build.sh sha-$(git rev-parse --short=12 HEAD) push
```

发布前确认工作区干净，以便 SHA 标签可追溯。脚本先执行整个工程的 `clean install`（包含测试），再通过 Jib 直接上传；上传无需运行 Docker daemon，Docker CLI 仅用于保存认证。基础镜像从容器仓库拉取。首次推送后按上述步骤将包设为 Public；包可见性对同一包的后续版本持续生效。私有包的部署账号须登录 GHCR，拉取 Token 使用 `read:packages`。详情见 [GitHub Container registry 文档](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)。

确认待发布提交与版本后也可使用版本标签，例如 `bash build.sh 1.0.1 push`。不要用新的未发布代码覆盖已有稳定版标签；`latest` 仅在明确需要更新默认版本时使用，不会随 SHA 或版本标签自动发布。

只构建本地镜像（需要 Docker daemon）：`bash build.sh <tag>`。覆盖仓库地址：`APP_IMAGE=其他仓库地址 bash build.sh <tag> push`。

在 `g2rain-deploy` 的 `.env` 中指定实际已推送的标签：

```dotenv
KNOWLEDGE_IMAGE=ghcr.io/g2rain/g2rain-knowledge:<tag>
```

使用现有部署更新流程拉取并重建 knowledge 服务；该变量只控制后端，`KNOWLEDGE_APP_IMAGE` 仍单独配置。镜像发布不包含数据库迁移或知识文件卷。

## RQ-008 升级顺序（已有环境）

1. MySQL：执行 `scripts/migrations/2026-10-27-rq008-visibility.sql`（勿重跑会 DROP 的全量 SQL）。
2. Milvus：对已有 collection **原地**加 `visibility`、建索引、load；步骤见 [milvus-visibility-inplace.md](milvus-visibility-inplace.md)。`init-collection.sh` 不会改已有表。
3. 停旧 knowledge 实例后发布新后端与 knowledge-app；再开放公共入口与 `includePublic`。
4. 验收：双机构、`adminCompany` true/false、公共列表与检索开关。

新建空库依赖 `g2rain-deploy/config/milvus/knowledge-collection.json` 已含 `visibility`；存量升级仍走上面的原地加字段。
