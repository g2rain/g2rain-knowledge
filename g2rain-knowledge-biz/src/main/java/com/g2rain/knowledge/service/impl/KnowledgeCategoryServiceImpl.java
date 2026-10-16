package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeCategoryConverter;
import com.g2rain.knowledge.dao.KnowledgeCategoryDao;
import com.g2rain.knowledge.dao.po.KnowledgeCategoryPo;
import com.g2rain.knowledge.dto.KnowledgeCategoryDto;
import com.g2rain.knowledge.dto.KnowledgeCategorySelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeCategoryService;
import com.g2rain.knowledge.vo.KnowledgeCategoryVo;
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
 * 知识分类表服务实现类
 * 表名: knowledge_category
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeCategoryServiceImpl")
public class KnowledgeCategoryServiceImpl implements KnowledgeCategoryService {

    @Resource(name = "knowledgeCategoryDao")
    private KnowledgeCategoryDao knowledgeCategoryDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeCategoryVo> selectList(KnowledgeCategorySelectDto selectDto) {
        return knowledgeCategoryDao.selectList(selectDto)
            .stream()
            .map(KnowledgeCategoryConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeCategoryVo> selectPage(PageSelectListDto<KnowledgeCategorySelectDto> selectDto) {
        Page<KnowledgeCategoryPo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeCategoryDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeCategoryVo> result = page.getResult()
            .stream()
            .map(KnowledgeCategoryConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeCategoryDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;

        if (creating) {
            dto.setOrganId(organId);
            if (isBlank(dto.getStatus())) {
                dto.setStatus(KnowledgeEnablementStatus.ACTIVE.name());
            }
            if (Objects.isNull(dto.getSortOrder())) {
                dto.setSortOrder(0);
            }
            Validations.validateSave(dto);
            KnowledgeCategoryPo entity = KnowledgeCategoryConverter.INSTANCE.dto2po(dto);
            entity.setId(idGenerator.generateId());
            entity.setOrganId(organId);
            LocalDateTime now = Moments.now();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            Asserts.greaterThan(knowledgeCategoryDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
            return entity.getId();
        }

        KnowledgeCategoryPo existing = knowledgeCategoryDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        Validations.validateSave(dto);

        KnowledgeCategoryPo entity = KnowledgeCategoryConverter.INSTANCE.dto2po(dto);
        entity.setId(existing.getId());
        entity.setOrganId(existing.getOrganId());
        entity.setCreateTime(existing.getCreateTime());
        entity.setUpdateTime(Moments.now());
        if (Objects.isNull(entity.getStatus())) {
            entity.setStatus(existing.getStatus());
        }
        if (Objects.isNull(entity.getSortOrder())) {
            entity.setSortOrder(existing.getSortOrder());
        }
        Asserts.greaterThan(knowledgeCategoryDao.update(entity), 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeCategoryPo existing = knowledgeCategoryDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeCategoryDao.delete(id);
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
