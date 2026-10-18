package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeAnswerTemplateConverter;
import com.g2rain.knowledge.dao.KnowledgeAnswerTemplateDao;
import com.g2rain.knowledge.dao.po.KnowledgeAnswerTemplatePo;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateDto;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateSelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeAnswerTemplateService;
import com.g2rain.knowledge.vo.KnowledgeAnswerTemplateVo;
import com.g2rain.mybatis.pagination.PageContext;
import com.g2rain.mybatis.pagination.model.Page;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 知识回复模板表服务实现类
 * 表名: knowledge_answer_template
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeAnswerTemplateServiceImpl")
public class KnowledgeAnswerTemplateServiceImpl implements KnowledgeAnswerTemplateService {

    @Resource(name = "knowledgeAnswerTemplateDao")
    private KnowledgeAnswerTemplateDao knowledgeAnswerTemplateDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeAnswerTemplateVo> selectList(KnowledgeAnswerTemplateSelectDto selectDto) {
        return knowledgeAnswerTemplateDao.selectList(selectDto)
            .stream()
            .map(KnowledgeAnswerTemplateConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeAnswerTemplateVo> selectPage(PageSelectListDto<KnowledgeAnswerTemplateSelectDto> selectDto) {
        Page<KnowledgeAnswerTemplatePo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeAnswerTemplateDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeAnswerTemplateVo> result = page.getResult()
            .stream()
            .map(KnowledgeAnswerTemplateConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeAnswerTemplateDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;

        if (creating) {
            dto.setOrganId(organId);
            if (isBlank(dto.getStatus())) {
                dto.setStatus(KnowledgeEnablementStatus.ACTIVE.name());
            }
            Validations.validateSave(dto);
            KnowledgeAnswerTemplatePo entity = KnowledgeAnswerTemplateConverter.INSTANCE.dto2po(dto);
            entity.setId(idGenerator.generateId());
            entity.setOrganId(organId);
            LocalDateTime now = Moments.now();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            Asserts.greaterThan(knowledgeAnswerTemplateDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
            return entity.getId();
        }

        KnowledgeAnswerTemplatePo existing = knowledgeAnswerTemplateDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        Validations.validateSave(dto);

        KnowledgeAnswerTemplatePo entity = KnowledgeAnswerTemplateConverter.INSTANCE.dto2po(dto);
        entity.setId(existing.getId());
        entity.setOrganId(existing.getOrganId());
        entity.setCreateTime(existing.getCreateTime());
        entity.setUpdateTime(Moments.now());
        if (Objects.isNull(entity.getStatus())) {
            entity.setStatus(existing.getStatus());
        }
        Asserts.greaterThan(knowledgeAnswerTemplateDao.update(entity), 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeAnswerTemplatePo existing = knowledgeAnswerTemplateDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeAnswerTemplateDao.delete(id);
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
