package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeRevisionApi;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.dto.KnowledgeRevisionDto;
import com.g2rain.knowledge.dto.KnowledgeRevisionSelectDto;
import com.g2rain.knowledge.dto.KnowledgeReviewDto;
import com.g2rain.knowledge.service.KnowledgeDraftService;
import com.g2rain.knowledge.service.KnowledgeReviewService;
import com.g2rain.knowledge.service.KnowledgeRevisionService;
import com.g2rain.knowledge.service.source.KnowledgeSourceDownload;
import com.g2rain.knowledge.vo.KnowledgeRevisionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识版本表控制器
 * 表名: knowledge_revision
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_revision")
public class KnowledgeRevisionController implements KnowledgeRevisionApi {

    @Resource(name = "knowledgeRevisionServiceImpl")
    private KnowledgeRevisionService knowledgeRevisionService;
    @Resource
    private KnowledgeReviewService knowledgeReviewService;
    @Resource
    private KnowledgeDraftService knowledgeDraftService;

    @Override
    public Result<List<KnowledgeRevisionVo>> selectList(KnowledgeRevisionSelectDto selectDto) {
        return Result.success(knowledgeRevisionService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeRevisionVo>> selectPage(PageSelectListDto<KnowledgeRevisionSelectDto> selectDto) {
        return Result.successPage(knowledgeRevisionService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识版本表信息", description = "新增或更新知识版本表基础信息")
    public Result<Long> save(@RequestBody KnowledgeRevisionDto dto) {
        return Result.success(knowledgeRevisionService.save(dto));
    }

    @DeleteMapping("/{id}")
    @LoginGuard
    @Operation(summary = "删除知识版本表记录", description = "根据主键删除知识版本表记录")
    public Result<Integer> delete(@Parameter(description = "知识版本表标识") @PathVariable Long id) {
        return Result.success(knowledgeRevisionService.delete(id));
    }

    @GetMapping("/{revisionId}/source/download")
    @LoginGuard
    @Operation(summary = "下载知识版本原文件", description = "仅 FILE 且已上传原文件；返回附件二进制流")
    public ResponseEntity<StreamingResponseBody> downloadSource(
        @Parameter(description = "知识版本标识") @PathVariable Long revisionId) throws IOException {
        KnowledgeSourceDownload download = knowledgeRevisionService.downloadSource(revisionId);
        try {
            ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(disposition);
            headers.setCacheControl("no-store");
            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(download.contentType());
            } catch (Exception exception) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
            StreamingResponseBody body = outputStream -> {
                try (InputStream content = download.content()) {
                    content.transferTo(outputStream);
                }
            };
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(body);
        } catch (RuntimeException | Error exception) {
            download.close();
            throw exception;
        }
    }

    @Override
    @LoginGuard
    public Result<Void> submitReview(@Valid @RequestBody KnowledgeReviewDto dto) {
        knowledgeReviewService.submit(dto);
        return Result.success();
    }

    @Override
    @LoginGuard
    public Result<Long> approveReview(@Valid @RequestBody KnowledgeReviewDto dto) {
        return Result.success(knowledgeReviewService.approve(dto));
    }

    @Override
    @LoginGuard
    public Result<Void> rejectReview(@Valid @RequestBody KnowledgeReviewDto dto) {
        knowledgeReviewService.reject(dto);
        return Result.success();
    }

    @PostMapping("/{revisionId}/replace-file")
    @LoginGuard
    @Operation(summary = "替换知识版本源文件", description = "仅可编辑的 FILE 草稿；服务端生成存储路径与 SHA-256")
    public Result<Void> replaceFile(
        @Parameter(description = "知识版本标识") @PathVariable Long revisionId,
        @Parameter(description = "乐观锁版本") @RequestParam Integer expectedVersion,
        @Parameter(description = "源文件（PDF / Markdown / HTML）") @RequestParam("file") MultipartFile file) throws IOException {
        SourceMetadata metadata = new SourceMetadata(file.getOriginalFilename(), file.getContentType(), file.getSize());
        try (InputStream content = file.getInputStream()) {
            knowledgeDraftService.replaceFile(revisionId, expectedVersion, content, metadata);
        }
        return Result.success();
    }
}
