package com.g2rain.knowledge.task;

import com.g2rain.common.utils.Moments;
import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.knowledge.client.docling.DoclingClient;
import com.g2rain.knowledge.client.docling.DoclingProperties;
import com.g2rain.knowledge.client.storage.KnowledgeStorageClient;
import com.g2rain.knowledge.client.storage.SourceFileKind;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.KnowledgeTaskDao;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.domain.ChunkContent;
import com.g2rain.knowledge.domain.ChunkingProfile;
import com.g2rain.knowledge.domain.ChunkingResult;
import com.g2rain.knowledge.domain.KnowledgeChunkPolicy;
import com.g2rain.knowledge.service.KnowledgeIndexService;
import com.g2rain.knowledge.service.KnowledgeProcessPersistenceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Claims a committed PROCESS task on a virtual thread; processing stages follow in this executor. */
@Slf4j
@Component
public class KnowledgeTaskAsyncExecutor {
    private final String workerId = "knowledge-" + UUID.randomUUID();
    @Resource
    private KnowledgeTaskDao knowledgeTaskDao;
    @Resource private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource private KnowledgeStorageClient knowledgeStorageClient;
    @Resource private DoclingClient doclingClient;
    @Resource private DoclingProperties doclingProperties;
    @Resource private KnowledgeChunkPolicy knowledgeChunkPolicy;
    @Resource private KnowledgeProcessPersistenceService knowledgeProcessPersistenceService;
    @Resource private KnowledgeIndexService knowledgeIndexService;

    @Async("knowledgeTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void execute(KnowledgeTaskRequestedEvent event) {
        log.info("received KnowledgeTaskRequestedEvent: taskId={}, workerId={}", event.taskId(), workerId);
        KnowledgeTaskPo persisted = knowledgeTaskDao.selectByIdWithoutIsolation(event.taskId());
        if (persisted == null || persisted.getOrganId() == null) {
            log.warn("skip task event: task missing or organId null, taskId={}", event.taskId());
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
            log.warn("skip task before claim: organ/task mismatch, taskId={}, organId={}, taskOrganId={}, principalOrganId={}, taskStatus={}",
                taskId, organId,
                task == null ? null : task.getOrganId(),
                PrincipalContextHolder.getOrganId(),
                task == null ? null : task.getTaskStatus());
            return;
        }
        LocalDateTime now = Moments.now();
        int claimed = knowledgeTaskDao.claim(organId, task.getId(), task.getVersion(), workerId,
            now.plus(Duration.ofMinutes(5)), now);
        if (claimed != 1) {
            log.warn("claim failed, task stays PENDING: taskId={}, organId={}, version={}, taskStatus={}, availableTime={}, now={}, workerId={}",
                task.getId(), organId, task.getVersion(), task.getTaskStatus(), task.getAvailableTime(), now, workerId);
            return;
        }
        log.info("claim succeeded: taskId={}, revisionId={}, organId={}, workerId={}",
            task.getId(), task.getRevisionId(), organId, workerId);
        if (!"PROCESS".equals(task.getTaskType())) {
            log.warn("skip non-PROCESS task after claim: taskId={}, taskType={}", task.getId(), task.getTaskType());
            return;
        }
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(task.getRevisionId());
        if (revision == null || !Objects.equals(organId, revision.getOrganId())) {
            log.warn("revision missing or organ mismatch after claim: taskId={}, revisionId={}, organId={}",
                task.getId(), task.getRevisionId(), organId);
            return;
        }
        try {
            log.info("PROCESS chunk start: taskId={}, revisionId={}, sourceType={}",
                task.getId(), revision.getId(), revision.getSourceType());
            ChunkingResult chunking = chunkRevision(revision);
            knowledgeChunkPolicy.validateManifest(chunking.chunks());
            log.info("PROCESS write parsed start: taskId={}, revisionId={}, chunkCount={}",
                task.getId(), revision.getId(), chunking.chunks().size());
            String path = knowledgeStorageClient.writeParsedArtifacts(
                revision.getOrganId(), revision.getKnowledgeId(), revision.getId(), task.getId(),
                chunking.documentMarkdown(), chunking.chunks());
            knowledgeProcessPersistenceService.saveParsed(revision, path, chunking.chunks(), chunking.parserName());
            log.info("PROCESS index start: taskId={}, revisionId={}", task.getId(), revision.getId());
            knowledgeIndexService.index(revision.getId());
            knowledgeProcessPersistenceService.complete(revision.getId());
            knowledgeTaskDao.complete(organId, task.getId(), workerId, task.getVersion() + 1, Moments.now());
            log.info("PROCESS completed: taskId={}, revisionId={}, workerId={}", task.getId(), revision.getId(), workerId);
        } catch (Exception exception) {
            log.error("PROCESS failed: taskId={}, revisionId={}, workerId={}",
                task.getId(), task.getRevisionId(), workerId, exception);
            knowledgeProcessPersistenceService.fail(task.getRevisionId());
            knowledgeTaskDao.fail(organId, task.getId(), workerId, task.getVersion() + 1, "PROCESS_PARSE_FAILED", "文档解析处理失败", Moments.now());
        }
    }

    private ChunkingResult chunkRevision(KnowledgeRevisionPo revision) throws Exception {
        ChunkingProfile profile = ChunkingProfile.defaults(doclingProperties.getChunkingMaxTokens());
        if ("FAQ".equals(revision.getSourceType())) {
            List<ChunkContent> chunks = knowledgeChunkPolicy.chunkFaq(
                revision.getFaqQuestion(), revision.getFaqAnswer(), profile);
            String markdown = "# " + Objects.requireNonNullElse(revision.getFaqQuestion(), "")
                + "\n\n" + Objects.requireNonNullElse(revision.getFaqAnswer(), "");
            return new ChunkingResult(markdown, chunks, KnowledgeChunkPolicy.PARSER_FAQ);
        }
        SourceFileKind kind = SourceFileKind.fromStored(revision.getSourceFileName(), revision.getSourceContentType());
        if (kind == null) {
            throw new IllegalStateException("unsupported source file kind");
        }
        try (InputStream source = knowledgeStorageClient.openSource(revision.getSourceStoragePath())) {
            log.info("PROCESS docling hybrid chunk start: revisionId={}, fileName={}, fromFormat={}",
                revision.getId(), revision.getSourceFileName(), kind.doclingFromFormat());
            return doclingClient.chunkFile(
                source,
                new SourceMetadata(revision.getSourceFileName(), revision.getSourceContentType(), revision.getSourceFileSize()),
                kind.doclingFromFormat(),
                profile);
        }
    }
}
