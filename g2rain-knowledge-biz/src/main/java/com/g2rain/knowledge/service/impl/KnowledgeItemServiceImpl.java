package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.client.storage.SourceFileKind;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.converter.KnowledgeItemConverter;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeCreateDto;
import com.g2rain.knowledge.dto.KnowledgeDraftCreateDto;
import com.g2rain.knowledge.dto.KnowledgeFaqEditDto;
import com.g2rain.knowledge.dto.KnowledgeImportOptionsDto;
import com.g2rain.knowledge.dto.KnowledgeItemDto;
import com.g2rain.knowledge.dto.KnowledgeItemSelectDto;
import com.g2rain.knowledge.dto.KnowledgeReviewDto;
import com.g2rain.knowledge.dto.KnowledgeRevisionSelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.enums.KnowledgeRevisionStatus;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeDraftService;
import com.g2rain.knowledge.service.KnowledgeItemService;
import com.g2rain.knowledge.service.KnowledgeReviewService;
import com.g2rain.knowledge.service.KnowledgeSaveSupport;
import com.g2rain.knowledge.vo.KnowledgeImportItemVo;
import com.g2rain.knowledge.vo.KnowledgeImportResultVo;
import com.g2rain.knowledge.vo.KnowledgeItemVo;
import com.g2rain.mybatis.pagination.PageContext;
import com.g2rain.mybatis.pagination.model.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识主题表服务实现类
 * 表名: knowledge_item
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeItemServiceImpl")
public class KnowledgeItemServiceImpl implements KnowledgeItemService {

    private static final Pattern KNOWLEDGE_KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern REVISION_NO_VN = Pattern.compile("^v(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter REVISION_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_FAILED = "FAILED";

    @Resource(name = "knowledgeItemDao")
    private KnowledgeItemDao knowledgeItemDao;
    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgeSaveSupport knowledgeSaveSupport;
    @Resource
    private KnowledgeDraftService knowledgeDraftService;
    @Resource
    private KnowledgeReviewService knowledgeReviewService;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeItemVo> selectList(KnowledgeItemSelectDto selectDto) {
        return knowledgeItemDao.selectList(selectDto)
            .stream()
            .map(KnowledgeItemConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeItemVo> selectPage(PageSelectListDto<KnowledgeItemSelectDto> selectDto) {
        Page<KnowledgeItemPo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeItemDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeItemVo> result = page.getResult()
            .stream()
            .map(KnowledgeItemConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeItemDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;

        if (creating) {
            dto.setOrganId(organId);
            dto.setCreateUserId(knowledgeAccessService.requireUserId());
            if (isBlank(dto.getStatus())) {
                dto.setStatus(KnowledgeEnablementStatus.ACTIVE.name());
            }
            if (Objects.isNull(dto.getPriority())) {
                dto.setPriority(0);
            }
            dto.setTagsJson(knowledgeSaveSupport.normalizeTagsJson(dto.getTagsJson()));
            knowledgeSaveSupport.validateCategory(organId, dto.getCategoryId());
            Validations.validateSave(dto);
            KnowledgeItemPo entity = KnowledgeItemConverter.INSTANCE.dto2po(dto);
            entity.setId(idGenerator.generateId());
            entity.setOrganId(organId);
            entity.setCreateUserId(dto.getCreateUserId());
            LocalDateTime now = Moments.now();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            Asserts.greaterThan(knowledgeItemDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
            return entity.getId();
        }

        KnowledgeItemPo existing = knowledgeItemDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        if (Objects.nonNull(dto.getKnowledgeType())
            && !Objects.equals(dto.getKnowledgeType(), existing.getKnowledgeType())
            && hasRevisions(existing.getId())) {
            throw new BusinessException(KnowledgeErrorCode.KNOWLEDGE_TYPE_LOCKED);
        }
        knowledgeSaveSupport.validateCategory(organId, dto.getCategoryId());
        if (Objects.nonNull(dto.getTagsJson())) {
            dto.setTagsJson(knowledgeSaveSupport.normalizeTagsJson(dto.getTagsJson()));
        }
        Validations.validateSave(dto);

        KnowledgeItemPo entity = KnowledgeItemConverter.INSTANCE.dto2po(dto);
        entity.setId(existing.getId());
        entity.setOrganId(existing.getOrganId());
        entity.setCreateUserId(existing.getCreateUserId());
        entity.setCreateTime(existing.getCreateTime());
        entity.setUpdateTime(Moments.now());
        if (Objects.isNull(entity.getStatus())) {
            entity.setStatus(existing.getStatus());
        }
        if (Objects.isNull(entity.getKnowledgeType())) {
            entity.setKnowledgeType(existing.getKnowledgeType());
        }
        if (Objects.isNull(entity.getPriority())) {
            entity.setPriority(existing.getPriority());
        }
        if (Objects.isNull(entity.getTagsJson())) {
            entity.setTagsJson(existing.getTagsJson());
        }
        Asserts.greaterThan(knowledgeItemDao.update(entity), 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeItemPo existing = knowledgeItemDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeItemDao.delete(id);
    }

    @Override
    public KnowledgeImportResultVo importItems(KnowledgeImportOptionsDto options, MultipartFile file) {
        KnowledgeImportOptionsDto safeOptions = options == null ? new KnowledgeImportOptionsDto() : options;
        if (file == null || file.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
        List<ItemRow> rows;
        try {
            rows = parseItemCsv(file.getInputStream());
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID, exception);
        }
        if (rows.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
        KnowledgeImportResultVo result = new KnowledgeImportResultVo();
        for (ItemRow row : rows) {
            result.getItems().add(importOneItem(safeOptions, row));
        }
        tally(result);
        return result;
    }

    @Override
    public KnowledgeImportResultVo importFaq(KnowledgeImportOptionsDto options, MultipartFile file) {
        KnowledgeImportOptionsDto safeOptions = options == null ? new KnowledgeImportOptionsDto() : options;
        if (file == null || file.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
        List<FaqRow> rows;
        try {
            rows = parseFaqCsv(file.getInputStream());
        } catch (IOException exception) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID, exception);
        }
        if (rows.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
        KnowledgeImportResultVo result = new KnowledgeImportResultVo();
        for (FaqRow row : rows) {
            result.getItems().add(importOneFaq(safeOptions, row));
        }
        tally(result);
        return result;
    }

    @Override
    public KnowledgeImportResultVo importFiles(KnowledgeImportOptionsDto options, List<MultipartFile> files) {
        KnowledgeImportOptionsDto safeOptions = options == null ? new KnowledgeImportOptionsDto() : options;
        if (files == null || files.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
        List<MultipartFile> nonEmpty = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                nonEmpty.add(file);
            }
        }
        if (nonEmpty.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
        KnowledgeImportResultVo result = new KnowledgeImportResultVo();
        for (MultipartFile file : nonEmpty) {
            result.getItems().add(importOneFile(safeOptions, file));
        }
        tally(result);
        return result;
    }

    private KnowledgeImportItemVo importOneItem(KnowledgeImportOptionsDto options, ItemRow row) {
        KnowledgeImportItemVo item = new KnowledgeImportItemVo();
        item.setKnowledgeKey(row.knowledgeKey());
        try {
            assertKnowledgeKey(row.knowledgeKey());
            if (findExistingByKey(row.knowledgeKey()) != null) {
                return skipped(item, "knowledge_key already exists");
            }
            if (isBlank(row.knowledgeName())) {
                return failed(item, KnowledgeErrorCode.IMPORT_FILE_INVALID, "主题 CSV 行缺少 knowledge_name");
            }
            Integer priority = row.priority() != null ? row.priority() : Objects.requireNonNullElse(options.getPriority(), 0);
            Long knowledgeId = createTopic(row.knowledgeKey(), row.knowledgeName(), "DOCUMENT",
                resolveCategoryId(row.categoryCode(), options.getCategoryId()), row.description(),
                resolveTags(row.tags(), options.getTags()), priority);
            Long revisionId = createDraft(knowledgeId, "FILE");
            return success(item, knowledgeId, revisionId);
        } catch (BusinessException exception) {
            return failed(item, exception);
        } catch (RuntimeException exception) {
            return failed(item, KnowledgeErrorCode.STORAGE_OPERATION_FAILED, exception.getMessage());
        }
    }

    private KnowledgeImportItemVo importOneFaq(KnowledgeImportOptionsDto options, FaqRow row) {
        KnowledgeImportItemVo item = new KnowledgeImportItemVo();
        item.setKnowledgeKey(row.knowledgeKey());
        try {
            assertKnowledgeKey(row.knowledgeKey());
            if (findExistingByKey(row.knowledgeKey()) != null) {
                return skipped(item, "knowledge_key already exists");
            }
            if (isBlank(row.knowledgeName()) || isBlank(row.question()) || isBlank(row.answer())) {
                return failed(item, KnowledgeErrorCode.IMPORT_FILE_INVALID, "FAQ 行缺少名称或问答内容");
            }
            Integer priority = row.priority() != null ? row.priority() : Objects.requireNonNullElse(options.getPriority(), 0);
            Long knowledgeId = createTopic(row.knowledgeKey(), row.knowledgeName(), "FAQ",
                resolveCategoryId(row.categoryCode(), options.getCategoryId()), row.description(), options.getTags(), priority);
            Long revisionId = createDraft(knowledgeId, "FAQ");
            KnowledgeRevisionPo revision = requireRevision(revisionId);
            KnowledgeFaqEditDto faq = new KnowledgeFaqEditDto();
            faq.setRevisionId(revisionId);
            faq.setExpectedVersion(revision.getVersion());
            faq.setQuestion(row.question());
            faq.setAnswer(row.answer());
            knowledgeDraftService.editFaq(faq);
            maybeSubmitReview(options, revisionId);
            return success(item, knowledgeId, revisionId);
        } catch (BusinessException exception) {
            return failed(item, exception);
        } catch (RuntimeException exception) {
            return failed(item, KnowledgeErrorCode.STORAGE_OPERATION_FAILED, exception.getMessage());
        }
    }

    private KnowledgeImportItemVo importOneFile(KnowledgeImportOptionsDto options, MultipartFile file) {
        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "");
        String knowledgeKey = stripExtension(originalName);
        KnowledgeImportItemVo item = new KnowledgeImportItemVo();
        item.setKnowledgeKey(knowledgeKey);
        try {
            assertKnowledgeKey(knowledgeKey);
            if (SourceFileKind.resolve(originalName, file.getContentType()) == null) {
                return failed(item, KnowledgeErrorCode.INVALID_SOURCE_FILE, "不支持的文件类型");
            }
            KnowledgeItemPo existing = findExistingByKey(knowledgeKey);
            if (existing == null) {
                return failed(item, KnowledgeErrorCode.IMPORT_ITEM_NOT_FOUND,
                    KnowledgeErrorCode.IMPORT_ITEM_NOT_FOUND.messageTemplate());
            }
            byte[] bytes = file.getBytes();
            String contentSha256 = sha256Hex(bytes);
            KnowledgeRevisionPo current = findLatestFileRevision(existing.getId());
            if (current != null && contentSha256.equalsIgnoreCase(current.getSourceSha256())) {
                return skipped(item, "source content unchanged (same SHA-256 as current " + current.getRevisionNo() + ")");
            }
            Long revisionId = findOrCreateFileDraft(existing.getId());
            KnowledgeRevisionPo revision = requireRevision(revisionId);
            SourceMetadata metadata = new SourceMetadata(originalName, file.getContentType(), file.getSize());
            knowledgeDraftService.replaceFile(revisionId, revision.getVersion(),
                new ByteArrayInputStream(bytes), metadata);
            maybeSubmitReview(options, revisionId);
            return success(item, existing.getId(), revisionId);
        } catch (BusinessException exception) {
            return failed(item, exception);
        } catch (Exception exception) {
            return failed(item, KnowledgeErrorCode.INVALID_SOURCE_FILE, exception.getMessage());
        }
    }

    private Long createTopic(String knowledgeKey, String knowledgeName, String knowledgeType,
                             Long categoryId, String description, List<String> tags, Integer priority) {
        KnowledgeCreateDto create = new KnowledgeCreateDto();
        create.setKnowledgeKey(knowledgeKey);
        create.setKnowledgeName(knowledgeName);
        create.setKnowledgeType(knowledgeType);
        create.setCategoryId(categoryId);
        create.setDescription(description);
        create.setTags(tags);
        create.setPriority(priority);
        return knowledgeDraftService.createKnowledge(create);
    }

    private Long createDraft(Long knowledgeId, String sourceType) {
        KnowledgeDraftCreateDto draft = new KnowledgeDraftCreateDto();
        draft.setKnowledgeId(knowledgeId);
        draft.setRevisionNo(nextRevisionNo(knowledgeId));
        draft.setSourceType(sourceType);
        return knowledgeDraftService.createDraft(draft);
    }

    private Long findOrCreateFileDraft(Long knowledgeId) {
        KnowledgeRevisionSelectDto query = new KnowledgeRevisionSelectDto();
        query.setKnowledgeId(knowledgeId);
        query.setSourceType("FILE");
        query.setRevisionStatus(KnowledgeRevisionStatus.DRAFT.name());
        List<KnowledgeRevisionPo> drafts = knowledgeRevisionDao.selectList(query);
        if (!drafts.isEmpty()) {
            return drafts.getFirst().getId();
        }
        return createDraft(knowledgeId, "FILE");
    }

    /**
     * Next revision_no for a topic: {@code v1}, {@code v2}, … when existing numbers follow that pattern;
     * otherwise a millisecond timestamp to avoid unique-key collisions.
     */
    private String nextRevisionNo(Long knowledgeId) {
        KnowledgeRevisionSelectDto query = new KnowledgeRevisionSelectDto();
        query.setKnowledgeId(knowledgeId);
        List<KnowledgeRevisionPo> revisions = knowledgeRevisionDao.selectList(query);
        if (revisions.isEmpty()) {
            return "v1";
        }
        int maxVn = 0;
        boolean hasVn = false;
        for (KnowledgeRevisionPo revision : revisions) {
            if (revision.getRevisionNo() == null) {
                continue;
            }
            Matcher matcher = REVISION_NO_VN.matcher(revision.getRevisionNo().trim());
            if (matcher.matches()) {
                hasVn = true;
                maxVn = Math.max(maxVn, Integer.parseInt(matcher.group(1)));
            }
        }
        if (hasVn) {
            return "v" + (maxVn + 1);
        }
        return Moments.now().format(REVISION_TS);
    }

    /** Latest FILE revision for the topic (by id); used only to compare against current content. */
    private KnowledgeRevisionPo findLatestFileRevision(Long knowledgeId) {
        KnowledgeRevisionSelectDto query = new KnowledgeRevisionSelectDto();
        query.setKnowledgeId(knowledgeId);
        query.setSourceType("FILE");
        return knowledgeRevisionDao.selectList(query).stream()
            .max(Comparator.comparing(KnowledgeRevisionPo::getId, Comparator.nullsLast(Long::compareTo)))
            .orElse(null);
    }

    private static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private void maybeSubmitReview(KnowledgeImportOptionsDto options, Long revisionId) {
        if (!Boolean.TRUE.equals(options.getSubmitReview())) {
            return;
        }
        KnowledgeRevisionPo revision = requireRevision(revisionId);
        KnowledgeReviewDto review = new KnowledgeReviewDto();
        review.setRevisionId(revisionId);
        review.setExpectedVersion(revision.getVersion());
        knowledgeReviewService.submit(review);
    }

    private KnowledgeItemPo findExistingByKey(String knowledgeKey) {
        KnowledgeItemSelectDto query = new KnowledgeItemSelectDto();
        query.setOrganId(knowledgeAccessService.requireOrganId());
        query.setKnowledgeKey(knowledgeKey);
        List<KnowledgeItemPo> found = knowledgeItemDao.selectList(query);
        return found.isEmpty() ? null : found.getFirst();
    }

    private KnowledgeRevisionPo requireRevision(Long revisionId) {
        KnowledgeRevisionPo revision = knowledgeRevisionDao.selectById(revisionId);
        Asserts.isTrue(Objects.nonNull(revision), SystemErrorCode.DATA_NOT_EXISTS, revisionId);
        return revision;
    }

    private static void assertKnowledgeKey(String knowledgeKey) {
        if (isBlank(knowledgeKey) || !KNOWLEDGE_KEY_PATTERN.matcher(knowledgeKey).matches()) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
    }

    private static List<ItemRow> parseItemCsv(InputStream inputStream) throws IOException {
        List<ItemRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            headerLine = stripBom(headerLine);
            List<String> headers = splitCsvLine(headerLine).stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
            int keyIdx = headers.indexOf("knowledge_key");
            int nameIdx = headers.indexOf("knowledge_name");
            int descriptionIdx = headers.indexOf("description");
            int priorityIdx = headers.indexOf("priority");
            int tagsIdx = headers.indexOf("tags");
            int categoryCodeIdx = headers.indexOf("category_code");
            if (keyIdx < 0 || nameIdx < 0) {
                throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = splitCsvLine(line);
                rows.add(new ItemRow(
                    cell(cols, keyIdx),
                    cell(cols, nameIdx),
                    descriptionIdx >= 0 ? cell(cols, descriptionIdx) : null,
                    parsePriority(priorityIdx >= 0 ? cell(cols, priorityIdx) : null),
                    tagsIdx >= 0 ? parseTags(cell(cols, tagsIdx)) : null,
                    categoryCodeIdx >= 0 ? blankToNull(cell(cols, categoryCodeIdx)) : null
                ));
            }
        }
        return rows;
    }

    private static List<FaqRow> parseFaqCsv(InputStream inputStream) throws IOException {
        List<FaqRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            headerLine = stripBom(headerLine);
            List<String> headers = splitCsvLine(headerLine).stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();
            int keyIdx = headers.indexOf("knowledge_key");
            int nameIdx = headers.indexOf("knowledge_name");
            int questionIdx = headers.indexOf("faq_question");
            int answerIdx = headers.indexOf("faq_answer");
            int descriptionIdx = headers.indexOf("description");
            int priorityIdx = headers.indexOf("priority");
            int categoryCodeIdx = headers.indexOf("category_code");
            if (keyIdx < 0 || nameIdx < 0 || questionIdx < 0 || answerIdx < 0) {
                throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = splitCsvLine(line);
                rows.add(new FaqRow(
                    cell(cols, keyIdx),
                    cell(cols, nameIdx),
                    cell(cols, questionIdx),
                    cell(cols, answerIdx),
                    descriptionIdx >= 0 ? cell(cols, descriptionIdx) : null,
                    parsePriority(priorityIdx >= 0 ? cell(cols, priorityIdx) : null),
                    categoryCodeIdx >= 0 ? blankToNull(cell(cols, categoryCodeIdx)) : null
                ));
            }
        }
        return rows;
    }

    private static Integer parsePriority(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(KnowledgeErrorCode.IMPORT_FILE_INVALID);
        }
    }

    /**
     * CSV tags cell: semicolon / pipe / comma separated tag_code list.
     */
    private static List<String> parseTags(String raw) {
        if (isBlank(raw)) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (String part : raw.split("[;|,]")) {
            String code = part.trim();
            if (!code.isEmpty()) {
                tags.add(code);
            }
        }
        return tags;
    }

    private static List<String> resolveTags(List<String> rowTags, List<String> optionTags) {
        if (rowTags != null && !rowTags.isEmpty()) {
            return rowTags;
        }
        return optionTags;
    }

    private Long resolveCategoryId(String categoryCode, Long optionCategoryId) {
        if (!isBlank(categoryCode)) {
            return knowledgeSaveSupport.requireActiveCategoryIdByCode(
                knowledgeAccessService.requireOrganId(), categoryCode);
        }
        return optionCategoryId;
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String cell(List<String> cols, int index) {
        if (index < 0 || index >= cols.size()) {
            return "";
        }
        return cols.get(index).trim();
    }

    private static List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private static String stripExtension(String fileName) {
        String name = Objects.requireNonNullElse(fileName, "");
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static void tally(KnowledgeImportResultVo result) {
        int success = 0;
        int skipped = 0;
        int failed = 0;
        for (KnowledgeImportItemVo item : result.getItems()) {
            if (STATUS_SUCCESS.equals(item.getItemStatus())) {
                success++;
            } else if (STATUS_SKIPPED.equals(item.getItemStatus())) {
                skipped++;
            } else {
                failed++;
            }
        }
        result.setSuccessCount(success);
        result.setSkippedCount(skipped);
        result.setFailedCount(failed);
    }

    private static KnowledgeImportItemVo success(KnowledgeImportItemVo item, Long knowledgeId, Long revisionId) {
        item.setItemStatus(STATUS_SUCCESS);
        item.setKnowledgeId(knowledgeId);
        item.setRevisionId(revisionId);
        return item;
    }

    private static KnowledgeImportItemVo skipped(KnowledgeImportItemVo item, String message) {
        item.setItemStatus(STATUS_SKIPPED);
        item.setErrorMessage(message);
        return item;
    }

    private static KnowledgeImportItemVo failed(KnowledgeImportItemVo item, BusinessException exception) {
        return failed(item, exception.getErrorCode(), exception.getErrorMessage());
    }

    private static KnowledgeImportItemVo failed(KnowledgeImportItemVo item, KnowledgeErrorCode errorCode, String message) {
        return failed(item, errorCode.code(), message == null || message.isBlank() ? errorCode.messageTemplate() : message);
    }

    private static KnowledgeImportItemVo failed(KnowledgeImportItemVo item, String errorCode, String message) {
        item.setItemStatus(STATUS_FAILED);
        item.setErrorCode(errorCode);
        item.setErrorMessage(message);
        return item;
    }

    private boolean hasRevisions(Long knowledgeId) {
        KnowledgeRevisionSelectDto query = new KnowledgeRevisionSelectDto();
        query.setKnowledgeId(knowledgeId);
        return !knowledgeRevisionDao.selectList(query).isEmpty();
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private record ItemRow(String knowledgeKey, String knowledgeName, String description, Integer priority,
                           List<String> tags, String categoryCode) {
    }

    private record FaqRow(String knowledgeKey, String knowledgeName, String question, String answer,
                          String description, Integer priority, String categoryCode) {
    }
}
