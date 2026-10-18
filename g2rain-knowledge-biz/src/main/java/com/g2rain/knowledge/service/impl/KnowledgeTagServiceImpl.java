package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeTagConverter;
import com.g2rain.knowledge.dao.KnowledgeTagDao;
import com.g2rain.knowledge.dao.po.KnowledgeTagPo;
import com.g2rain.knowledge.dto.KnowledgeTagDto;
import com.g2rain.knowledge.dto.KnowledgeTagSelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeTagService;
import com.g2rain.knowledge.vo.KnowledgeTagVo;
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
 * 知识标签字典表服务实现类
 * 表名: knowledge_tag
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeTagServiceImpl")
public class KnowledgeTagServiceImpl implements KnowledgeTagService {

    @Resource(name = "knowledgeTagDao")
    private KnowledgeTagDao knowledgeTagDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeTagVo> selectList(KnowledgeTagSelectDto selectDto) {
        return knowledgeTagDao.selectList(selectDto)
            .stream()
            .map(KnowledgeTagConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeTagVo> selectPage(PageSelectListDto<KnowledgeTagSelectDto> selectDto) {
        Page<KnowledgeTagPo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeTagDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeTagVo> result = page.getResult()
            .stream()
            .map(KnowledgeTagConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeTagDto dto) {
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
            KnowledgeTagPo entity = KnowledgeTagConverter.INSTANCE.dto2po(dto);
            entity.setId(idGenerator.generateId());
            entity.setOrganId(organId);
            LocalDateTime now = Moments.now();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            Asserts.greaterThan(knowledgeTagDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
            return entity.getId();
        }

        KnowledgeTagPo existing = knowledgeTagDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        Validations.validateSave(dto);

        KnowledgeTagPo entity = KnowledgeTagConverter.INSTANCE.dto2po(dto);
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
        Asserts.greaterThan(knowledgeTagDao.update(entity), 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeTagPo existing = knowledgeTagDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeTagDao.delete(id);
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
