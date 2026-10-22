package com.g2rain.knowledge.enums;

import com.g2rain.common.exception.ErrorCode;

/**
 * Errors that describe knowledge-domain invariants. Generic parameter,
 * authentication and persistence errors continue to use {@code SystemErrorCode}.
 */
public enum KnowledgeErrorCode implements ErrorCode {
    INVALID_KNOWLEDGE_TYPE("knowledge.40001", "知识类型无效"),
    INVALID_SOURCE_TYPE("knowledge.40002", "来源类型无效"),
    REVISION_NOT_EDITABLE("knowledge.40901", "当前知识版本不可编辑"),
    REVISION_VERSION_CONFLICT("knowledge.40902", "知识版本已被其他操作更新，请刷新后重试"),
    SOURCE_TYPE_MISMATCH("knowledge.40903", "知识主题类型与版本来源类型不匹配"),
    CATEGORY_NOT_AVAILABLE("knowledge.40904", "知识分类不存在或不可用"),
    INVALID_TAG_CODE("knowledge.40003", "标签编码格式无效"),
    TAG_NOT_AVAILABLE("knowledge.40906", "标签不存在或已停用"),
    REVISION_NOT_REVIEWABLE("knowledge.40907", "当前知识版本不可审核"),
    REVIEW_COMMENT_REQUIRED("knowledge.40005", "审核拒绝必须填写原因"),
    INVALID_SOURCE_FILE("knowledge.40004", "知识源文件不符合允许的格式或大小"),
    IMPORT_FILE_INVALID("knowledge.40010", "批量导入文件无效或为空"),
    IMPORT_ITEM_NOT_FOUND("knowledge.40011", "请先通过主题 CSV 创建知识主题后再上传文件"),
    SOURCE_NOT_READY("knowledge.40905", "知识草稿尚未具备可解析的来源内容"),
    SOURCE_FILE_MISSING("knowledge.40914", "知识原文件不存在或不可读"),
    SOURCE_DOWNLOAD_NOT_SUPPORTED("knowledge.40915", "当前版本不支持原文件下载"),
    KNOWLEDGE_TYPE_LOCKED("knowledge.40916", "主题已有版本，禁止修改知识类型"),
    RELEASE_SAVE_UPDATE_FORBIDDEN("knowledge.40917", "发布选择请使用发布或回滚接口，禁止通用保存更新"),
    RELEASE_DELETE_FORBIDDEN("knowledge.40918", "发布记录已承载线上版本，禁止删除；下线请使用专用接口"),
    INVALID_TAGS_JSON("knowledge.40008", "标签 JSON 格式无效"),
    STORAGE_NOT_CONFIGURED("knowledge.50001", "知识文件存储未配置"),
    STORAGE_OPERATION_FAILED("knowledge.50002", "知识文件存储操作失败"),
    TASK_NOT_RETRYABLE("knowledge.40908", "当前任务不可重试"),
    TASK_NOT_CANCELLABLE("knowledge.40909", "当前任务不可取消"),
    TASK_VERSION_CONFLICT("knowledge.40910", "任务已被其他操作更新，请刷新后重试"),
    TASK_NOT_DISPATCHABLE("knowledge.40919", "当前任务不可推动执行"),
    DOCLING_NOT_CONFIGURED("knowledge.50003", "文档解析服务未配置"),
    DOCLING_OPERATION_FAILED("knowledge.50201", "文档解析服务调用失败"),
    DOCLING_INVALID_RESPONSE("knowledge.50202", "文档解析服务未返回可用内容"),
    EMBEDDING_NOT_CONFIGURED("knowledge.50004", "向量模型服务未配置"),
    EMBEDDING_OPERATION_FAILED("knowledge.50203", "向量模型服务调用失败"),
    EMBEDDING_INVALID_RESPONSE("knowledge.50204", "向量模型服务返回了无效向量"),
    MILVUS_OPERATION_FAILED("knowledge.50209", "向量库服务调用失败"),
    MILVUS_INVALID_RESPONSE("knowledge.50210", "向量库服务返回了无效结果"),
    RELEASE_NOT_PUBLISHABLE("knowledge.40911", "当前知识版本不可发布"),
    RELEASE_TARGET_MISMATCH("knowledge.40912", "发布记录与知识版本不匹配"),
    RELEASE_VERSION_CONFLICT("knowledge.40913", "知识发布记录已被其他操作更新，请刷新后重试"),
    INVALID_RETRIEVE_TOP_K("knowledge.40006", "检索证据数量超出允许范围"),
    ANSWER_NOT_CONFIGURED("knowledge.50005", "知识问答模型未配置"),
    ANSWER_OPERATION_FAILED("knowledge.50205", "知识问答模型调用失败"),
    ANSWER_INVALID_RESPONSE("knowledge.50206", "知识问答模型未返回有效回复"),
    ANSWER_TEMPLATE_NOT_AVAILABLE("knowledge.40007", "知识回复模板不存在"),
    RERANK_NOT_CONFIGURED("knowledge.50006", "重排序模型未配置"),
    RERANK_OPERATION_FAILED("knowledge.50207", "重排序模型调用失败"),
    RERANK_INVALID_RESPONSE("knowledge.50208", "重排序模型返回了无效结果"),
    DEPENDENCY_UNAVAILABLE("knowledge.50301", "知识检索依赖暂不可用"),
    RETRIEVE_DEADLINE_EXCEEDED("knowledge.50302", "知识检索超过总耗时预算");

    private final String code;
    private final String messageTemplate;

    KnowledgeErrorCode(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String messageTemplate() {
        return messageTemplate;
    }
}
