# RQ-006：原文件受控下载方案

状态：已实现（后端）。`GET /knowledge_revision/{revisionId}/source/download` 已提供附件流；App 页面接入与 Gateway CORS 核对仍属前端/部署侧。

## 背景与目标

审核在解析和切片之前进行，审核员需要阅读服务器保存的原文件。现有版本 Controller 只有文件替换能力，缺少读取入口。文件审核的完整交付因此被阻塞，FAQ 则可以直接通过版本详情显示问答内容。

本方案统一采用附件下载：审核员下载原文件，在本地打开，再回到页面执行通过或驳回。不实现在线预览、格式转换或下载后自动审核，也不要求下载记录成为审核通过的技术条件。审核通过/驳回接口本身不读取文件、不校验「是否已下载」；「人是否看过」由页面流程保证，后端不强制。

## 接口契约

服务内路径：

```http
GET /knowledge_revision/{revisionId}/source/download
```

按现有 App 服务前缀，前端调用路径为 `/knowledge/knowledge_revision/{revisionId}/source/download`。部署时与实际 Gateway 路由共同核对（服务内为 `/knowledge_revision/...`，App 侧带 `/knowledge` 前缀；本仓库不单独证明 StripPrefix 策略，以联调为准）。

- 只接收版本 ID，不接收组织 ID、文件名、路径或任意 URL。
- 使用现有登录机制及受信组织上下文，保留 Starter 数据隔离；接口使用 `@LoginGuard`。
- 仅支持存在且属于当前组织的 `FILE` 版本，并要求原文件已经上传。
- **文件就绪判定**与 `KnowledgeReviewServiceImpl.hasSource` 对齐：`sourceType=FILE` 且 `sourceStoragePath` 非空且非空白。FAQ 来源明确拒绝，不得下载。路径有值但磁盘文件缺失时，返回可理解的业务错误，避免一律暴露为笼统的 `STORAGE_OPERATION_FAILED`（可新增或复用领域错误码，实现时选定并写进契约）。
- 不限定为已发布版本，`DRAFT`、`REVIEWING` 等已有原文件的版本均可读取；下载不改变版本状态、乐观锁 `version`、审核信息或任务。
- 成功返回原始文件二进制流，不包装成 `Result`，不返回存储地址，不跳转到静态文件目录。
- 文件类型使用版本记录中已保存的 `sourceContentType`；无法可靠确定时使用 `application/octet-stream`。当前上传允许 PDF / Markdown / HTML，下载契约不另行收窄 MIME，以库内元数据为准。
- `Content-Disposition` 固定为 `attachment`，文件名**只使用**版本记录中的 `sourceFileName`（存储侧已净化），经安全编码后写入响应头；中文文件名须可正确保存（如 RFC 5987）。禁止使用请求参数中的文件名，禁止把未净化输入直接拼进响应头。
- 使用 `Cache-Control: no-store`，避免业务原文落入共享缓存。若跨域下载需要前端读取文件名，部署核对清单须确认 Gateway CORS 已暴露 `Content-Disposition`。
- 登录失效、版本不存在或不可访问、FAQ 来源、文件未就绪、磁盘文件缺失和读取失败，沿用 G2rain 统一错误契约；不得泄露物理路径或底层异常。**所有可执行的业务与权限检查必须在开始发送文件流之前完成**；流一旦开始写出，中断后不能再向已发送的内容追加 JSON 错误。

## 处理流程与具体类方法

1. `KnowledgeRevisionController.downloadSource(revisionId)`：新增 GET 接口并使用 `@LoginGuard`，设置附件响应头并流式输出，确保输入流释放，不将整个文件读入内存。
2. `KnowledgeRevisionService.downloadSource(revisionId)`：在现有版本 Service 增加单一下载能力，不新建管理 Controller。
3. `KnowledgeRevisionServiceImpl.downloadSource(revisionId)`：通过 `KnowledgeRevisionDao.selectById` 获取记录，复用 `KnowledgeAccessService.requireOrganId/assertSameOrgan`，按上方就绪条件校验来源与文件，然后打开原文件。返回内部文件资源描述，供 Controller 处理文件名、类型和流；资源描述不作为 JSON 序列化给调用方。
4. 复用 `KnowledgeStorageClient.openSource(relativePath)`：只使用版本记录中的受控路径（`sourceStoragePath`）；沿用存储实现的根目录约束与路径检查，防止越界读取。不新增文件存储框架或独立下载 DAO。

实现说明：本知识服务仓库内**没有**现成的 `StreamingResponseBody` / `InputStreamResource` / `Content-Disposition` 下载样板，仅有 JSON `Result` 等响应。实现时以 Spring 流式二进制响应为准自行落地，并保证异常路径下文件流关闭（含客户端主动中断、写出失败）。

文件资源描述类按实现需要放在 Biz 层，仅携带文件名、类型和流式资源。公共 API 模块不依赖 Biz；不为下载创建表或新增状态。

## 页面配合

- 版本详情和审核页的文件来源展示「下载原文件」；FAQ 直接展示问题和答案。
- 通过 App 现有认证 HTTP 客户端获取二进制数据，避免直接导航下载时丢失认证头。
- 下载请求单独使用原始响应模式，保留附件响应头，不套用默认 `Result` 解包。具体客户端能力在实现时核对，不能假设已有 JSON API 方法可直接下载。
- 下载失败不生成错误 JSON 附件，应解析统一错误并提示；浏览器保存完成后释放下载所用临时对象 URL。
- 审核仍单独调用已有通过/驳回接口，携带最新乐观锁版本号。下载成功不代表已经阅读或批准；无下载入口时 UI 可阻止提交文件审核，属前端约束，后端不强制「已下载」。

## 实施顺序与验收

1. 核对现有存储读取边界；确认无现成 Controller 下载样板后，选定 Spring 流式响应写法。
2. 在现有版本 Controller/Service 增加下载能力，复用现有 DAO、组织校验与存储客户端；就绪判定与 `hasSource` 对齐；文件缺失使用业务错误码。
3. 部署核对：服务内 `GET /knowledge_revision/{id}/source/download` ↔ App `GET /knowledge/knowledge_revision/{id}/source/download`；如需前端读文件名，核对 CORS 暴露 `Content-Disposition`。
4. 按 App 页面方案接入下载，检查独立模式与 qiankun 集成模式的认证链路。
5. 实现后验证：审核员在不同于上传者的登录会话中可下载同组织原文件；跨组织请求被拒绝；FAQ、无 `sourceStoragePath`、磁盘文件缺失返回可理解错误；中文文件名、文件内容完整性、路径保护、写流前完成检查及流释放正确。

没有该接口时只能验收 FAQ 审核，不能验收文件审核主流程。无需 SQL 迁移，不修改现有上传、保存或审核返回契约。后端先部署下载接口，前端再开放下载入口；回退页面不影响已有文件和审核状态。

与 [RQ-005](RQ-005-knowledge-save-contract-upgrade.md) 的关系：下载不依赖保存默认值逻辑，但共享登录与组织上下文。建议先完成 RQ-005 中 `KnowledgeRevisionVo.version` 出参（仅知识版本 VO，不改 common 基类；供审核/替换文件 CAS），再或并行部署本下载接口。
