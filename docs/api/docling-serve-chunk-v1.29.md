# Docling Serve HybridChunker 契约（v1.29）

部署镜像：`ghcr.io/docling-project/docling-serve-cpu:v1.29.0`（派生 `g2rain/docling-serve-cpu:v1.29.0`）。

本页冻结 knowledge 适配器依赖的同步切片 HTTP 契约；字段来自官方 Serve OpenAPI / client 类型定义。

## 端点

`POST {DOCLING_BASE_URL}/v1/chunk/hybrid/file`

- `Content-Type: multipart/form-data`
- 可选鉴权头：`X-Api-Key`（`DOCLING_API_KEY`）

## 请求字段（knowledge 使用子集）

| 字段 | 值 |
| --- | --- |
| `files` | 源文件字节 + 原始文件名 |
| `include_converted_doc` | `true`（同时拿 Markdown 全文） |
| `convert_from_formats` | 按 `SourceFileKind`：`pdf` / `docx` / `pptx` / `xlsx` / `md`（含 `.txt`） / `html`；遗留 `.doc`/`.ppt`/`.xls` 在 Serve v1.29 OpenAPI 未声明时**省略**该字段，改由文件名嗅探 |
| `chunking_include_raw_text` | `true` |
| `chunking_max_tokens` | 配置项，默认 `512` |
| `chunking_merge_peers` | `true` |
| `chunking_use_markdown_tables` | `true` |

knowledge FILE 白名单与 Docling 映射：

| 扩展名 | `convert_from_formats` | 说明 |
| --- | --- | --- |
| `.pdf` | `pdf` | 二进制签名 `%PDF-` |
| `.docx` | `docx` | ZIP/OOXML 签名 |
| `.doc` | （省略，嗅探） | OLE 签名；底层需 DOC/LibreOffice |
| `.pptx` | `pptx` | ZIP/OOXML 签名 |
| `.ppt` | （省略，嗅探） | OLE 签名；底层需 PPT/LibreOffice |
| `.xlsx` | `xlsx` | ZIP/OOXML 签名 |
| `.xls` | （省略，嗅探） | OLE 签名；底层需 XLS/LibreOffice |
| `.md` / `.markdown` | `md` | UTF-8 文本 |
| `.txt` / `.text` | `md` | Docling 将 txt 归 Markdown 后端 |
| `.html` / `.htm` | `html` | UTF-8 文本 |

## 响应 `ChunkDocumentResponse`

| 字段 | 说明 |
| --- | --- |
| `chunks[]` | 切片列表 |
| `documents[]` | `include_converted_doc=true` 时的 `ExportResult` |
| `processing_time` | 处理耗时（秒） |

### `chunks[]`（`ChunkedDocumentResultItem`）

| 字段 | 映射 |
| --- | --- |
| `chunk_index` | 0-based → 内部 `chunkNo = index + 1` |
| `text` | 带结构上下文 → `embeddingText` |
| `raw_text` | 原文 → `rawText`；缺省时回退 `text` |
| `num_tokens` | → `tokenCount` |
| `headings` | → `titlePath`（` > ` 连接） |
| `page_numbers` | → `pageStart` / `pageEnd`（min/max） |

### `documents[].content`

| 字段 | 映射 |
| --- | --- |
| `md_content` | → 解析产物 `document.md` |
| `text_content` | Markdown 为空时的回退 |

## knowledge 边界

- Docling：结构解析 + HybridChunker。
- knowledge：`validateManifest`、稳定 `chunk:{n}` locator、产物落盘、MySQL 清单、Embedding/发布。
- FAQ 不调用本端点，由 `KnowledgeChunkPolicy.chunkFaq` 本地切分。
