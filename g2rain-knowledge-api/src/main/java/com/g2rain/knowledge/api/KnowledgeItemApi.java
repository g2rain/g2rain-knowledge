package com.g2rain.knowledge.api;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeImportOptionsDto;
import com.g2rain.knowledge.dto.KnowledgeItemSelectDto;
import com.g2rain.knowledge.vo.KnowledgeImportResultVo;
import com.g2rain.knowledge.vo.KnowledgeItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * 知识主题表API接口
 * 表名: knowledge_item
 *
 * @author G2rain Generator
 */
@Tag(name = "知识主题表", description = "知识主题表相关接口")
public interface KnowledgeItemApi {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return 数据列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询知识主题表列表", description = "根据查询条件返回知识主题表列表")
    Result<List<KnowledgeItemVo>> selectList(KnowledgeItemSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页数据
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询知识主题表列表", description = "分页查询知识主题表列表")
    Result<PageData<KnowledgeItemVo>> selectPage(PageSelectListDto<KnowledgeItemSelectDto> selectDto);

    @PostMapping(value = "/import/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量导入主题 CSV", description = "CSV 创建 DOCUMENT 主题与空 FILE 草稿；可选列 tags（分号分隔 tag_code，行级优先于表单 tags）；knowledge_key 已存在则跳过")
    Result<KnowledgeImportResultVo> importItems(KnowledgeImportOptionsDto options,
                                                @RequestParam("file") MultipartFile file);

    @PostMapping(value = "/import/faq", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量导入 FAQ", description = "CSV 批量创建 FAQ 主题与草稿；标签/优先级写入主题；knowledge_key 已存在则跳过")
    Result<KnowledgeImportResultVo> importFaq(KnowledgeImportOptionsDto options,
                                              @RequestParam("file") MultipartFile file);

    @PostMapping(value = "/import/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量挂接文件", description = "按文件名 stem 挂接到已有 DOCUMENT 主题的 FILE 草稿；主题不存在则失败")
    Result<KnowledgeImportResultVo> importFiles(KnowledgeImportOptionsDto options,
                                                @RequestParam("files") List<MultipartFile> files);
}