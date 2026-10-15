package com.g2rain.knowledge.task;

import com.g2rain.common.utils.Moments;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.client.docling.DoclingClient;
import com.g2rain.knowledge.client.docling.ParsedDocument;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.service.KnowledgeProcessPersistenceService;
import com.g2rain.knowledge.service.KnowledgeIndexService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.UUID;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;

/** Claims a committed PROCESS task on a virtual thread; processing stages follow in this executor. */
@Component
public class KnowledgeTaskAsyncExecutor {
    private final String workerId = "knowledge-" + UUID.randomUUID();
    @Resource
    private KnowledgeTaskDao knowledgeTaskDao;
    @Resource private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource private KnowledgeStorageClient knowledgeStorageClient;
    @Resource private DoclingClient doclingClient;
    @Resource private KnowledgeProcessPersistenceService knowledgeProcessPersistenceService;
    @Resource private KnowledgeIndexService knowledgeIndexService;

    @Async("knowledgeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void execute(KnowledgeTaskRequestedEvent event) {
        KnowledgeTaskPo task = knowledgeTaskDao.selectById(event.taskId());
        if (task == null || knowledgeTaskDao.claim(task.getId(), task.getVersion(), workerId,
            Moments.now().plus(Duration.ofMinutes(5)), Moments.now()) != 1) {
            return;
        }
        if (!"PROCESS".equals(task.getTaskType())) return;
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(task.getRevisionId());
        if (revision == null) return;
        try {
            String content;
            if ("FAQ".equals(revision.getSourceType())) {
                content = "# " + revision.getFaqQuestion() + "\n\n" + revision.getFaqAnswer();
            } else {
                try (InputStream source = knowledgeStorageClient.openSource(revision.getSourceStoragePath())) {
                    ParsedDocument parsed = doclingClient.parse(source, new SourceMetadata(revision.getSourceFileName(), revision.getSourceContentType(), revision.getSourceFileSize()));
                    content = parsed.markdown().isBlank() ? parsed.text() : parsed.markdown();
                }
            }
            String path = knowledgeStorageClient.writeParsed(revision.getOrganId(), revision.getKnowledgeId(), revision.getId(), task.getId(), content);
            knowledgeProcessPersistenceService.saveParsed(revision, path, chunks(content));
            knowledgeIndexService.index(revision.getId());
            knowledgeProcessPersistenceService.complete(revision.getId());
            knowledgeTaskDao.complete(task.getId(), workerId, task.getVersion() + 1, Moments.now());
        } catch (Exception exception) {
            knowledgeProcessPersistenceService.fail(task.getRevisionId());
            knowledgeTaskDao.fail(task.getId(), workerId, task.getVersion() + 1, "PROCESS_PARSE_FAILED", "文档解析处理失败", Moments.now());
        }
    }

    private java.util.List<ParsedChunk> chunks(String text) throws Exception {
        var result = new ArrayList<ParsedChunk>();
        for (int start = 0, no = 1; start < text.length(); start += 1200, no++) {
            int end = Math.min(text.length(), start + 1200); String part = text.substring(start, end);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(part.getBytes(StandardCharsets.UTF_8)));
            result.add(new ParsedChunk(no, "char:" + start + ':' + end, hash, part.split("\\s+").length));
        }
        return result;
    }
}
