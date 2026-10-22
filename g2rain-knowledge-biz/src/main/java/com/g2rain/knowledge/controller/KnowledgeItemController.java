package com.g2rain.knowledge.controller;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.api.KnowledgeItemApi;
import com.g2rain.knowledge.dto.KnowledgeImportOptionsDto;
import com.g2rain.knowledge.dto.KnowledgeItemDto;
import com.g2rain.knowledge.dto.KnowledgeItemSelectDto;
import com.g2rain.knowledge.service.KnowledgeItemService;
import com.g2rain.knowledge.vo.KnowledgeImportResultVo;
import com.g2rain.knowledge.vo.KnowledgeItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识主题表控制器
 * 表名: knowledge_item
 *
 * @author G2rain Generator
 */
@RestController
@RequestMapping("/knowledge_item")
public class KnowledgeItemController implements KnowledgeItemApi {

    @Resource(name = "knowledgeItemServiceImpl")
    private KnowledgeItemService knowledgeItemService;

    @Override
    public Result<List<KnowledgeItemVo>> selectList(KnowledgeItemSelectDto selectDto) {
        return Result.success(knowledgeItemService.selectList(selectDto));
    }

    @Override
    public Result<PageData<KnowledgeItemVo>> selectPage(PageSelectListDto<KnowledgeItemSelectDto> selectDto) {
        return Result.successPage(knowledgeItemService.selectPage(selectDto));
    }

    @PostMapping("/save")
    @LoginGuard
    @Operation(summary = "新增或更新知识主题表信息", description = "新增或更新知识主题表基础信息")
    public Result<Long> save(@RequestBody KnowledgeItemDto dto) {
        return Result.success(knowledgeItemService.save(dto));
    }

    @DeleteMapping("/{id}")
    @LoginGuard
    @Operation(summary = "删除知识主题表记录", description = "根据主键删除知识主题表记录")
    public Result<Integer> delete(@Parameter(description = "知识主题表标识") @PathVariable Long id) {
        return Result.success(knowledgeItemService.delete(id));
    }

    @Override
    @LoginGuard
    @PostMapping(value = "/import/faq", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<KnowledgeImportResultVo> importFaq(
        @ModelAttribute KnowledgeImportOptionsDto options,
        @RequestParam("file") MultipartFile file) {
        return Result.success(knowledgeItemService.importFaq(options == null ? new KnowledgeImportOptionsDto() : options, file));
    }

    @Override
    @LoginGuard
    @PostMapping(value = "/import/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<KnowledgeImportResultVo> importFiles(
        @ModelAttribute KnowledgeImportOptionsDto options,
        @RequestParam("files") List<MultipartFile> files) {
        return Result.success(knowledgeItemService.importFiles(options == null ? new KnowledgeImportOptionsDto() : options, files));
    }
}
