package com.g2rain.knowledge.task;

import com.g2rain.common.utils.Moments;
import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.client.docling.DoclingClient;
import com.g2rain.knowledge.client.docling.ParsedDocument;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.client.storage.SourceFileKind;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.service.KnowledgeProcessPersistenceService;
import com.g2rain.knowledge.service.KnowledgeIndexService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Objects;
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
        KnowledgeTaskPo persisted = knowledgeTaskDao.selectByIdWithoutIsolation(event.taskId());
        if (persisted == null || persisted.getOrganId() == null) {
            return;
        }
        PrincipalContext organScope = PrincipalContext.of();
        organScope.setOrganId(persisted.getOrganId());
        PrincipalContextHolder.runWith(organScope, () -> executeInOrgan(persisted.getOrganId(), event.taskId()));
    }

    private void executeInOrgan(Long organId, Long taskId) {
        KnowledgeTaskPo task = knowledgeTaskDao.selectById(taskId);
        if (task == null || !Objects.equals(organId, task.getOrganId())
            || !Objects.equals(organId, PrincipalContextHolder.getOrganId())) {
            return;
        }
        if (knowledgeTaskDao.claim(organId, task.getId(), task.getVersion(), workerId,
            Moments.now().plus(Duration.ofMinutes(5)), Moments.now()) != 1) {
            return;
        }
        if (!"PROCESS".equals(task.getTaskType())) {
            return;
        }
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(task.getRevisionId());
        if (revision == null || !Objects.equals(organId, revision.getOrganId())) {
            return;
        }
        try {
            String content = loadContent(revision);
            String path = knowledgeStorageClient.writeParsed(revision.getOrganId(), revision.getKnowledgeId(), revision.getId(), task.getId(), content);
            knowledgeProcessPersistenceService.saveParsed(revision, path, chunks(content));
            knowledgeIndexService.index(revision.getId());
            knowledgeProcessPersistenceService.complete(revision.getId());
            knowledgeTaskDao.complete(organId, task.getId(), workerId, task.getVersion() + 1, Moments.now());
        } catch (Exception exception) {
            knowledgeProcessPersistenceService.fail(task.getRevisionId());
            knowledgeTaskDao.fail(organId, task.getId(), workerId, task.getVersion() + 1, "PROCESS_PARSE_FAILED", "文档解析处理失败", Moments.now());
        }
    }

    private String loadContent(KnowledgeRevisionPo revision) throws Exception {
        if ("FAQ".equals(revision.getSourceType())) {
            return "# " + revision.getFaqQuestion() + "\n\n" + revision.getFaqAnswer();
        }
        SourceFileKind kind = SourceFileKind.fromStored(revision.getSourceFileName(), revision.getSourceContentType());
        if (kind == null) {
            throw new IllegalStateException("unsupported source file kind");
        }
        try (InputStream source = knowledgeStorageClient.openSource(revision.getSourceStoragePath())) {
            if (!kind.requiresDocling()) {
                return new String(source.readAllBytes(), StandardCharsets.UTF_8);
            }
            ParsedDocument parsed = doclingClient.parse(source,
                new SourceMetadata(revision.getSourceFileName(), revision.getSourceContentType(), revision.getSourceFileSize()));
            return parsed.markdown().isBlank() ? parsed.text() : parsed.markdown();
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
