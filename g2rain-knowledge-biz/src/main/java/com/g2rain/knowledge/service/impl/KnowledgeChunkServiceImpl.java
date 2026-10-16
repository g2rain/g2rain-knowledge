package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeChunkConverter;
import com.g2rain.knowledge.dao.KnowledgeChunkDao;
import com.g2rain.knowledge.dao.po.KnowledgeChunkPo;
import com.g2rain.knowledge.dto.KnowledgeChunkDto;
import com.g2rain.knowledge.dto.KnowledgeChunkSelectDto;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeChunkService;
import com.g2rain.knowledge.vo.KnowledgeChunkVo;
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
 * 知识切片清单表服务实现类
 * 表名: knowledge_chunk
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeChunkServiceImpl")
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    @Resource(name = "knowledgeChunkDao")
    private KnowledgeChunkDao knowledgeChunkDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeChunkVo> selectList(KnowledgeChunkSelectDto selectDto) {
        return knowledgeChunkDao.selectList(selectDto)
            .stream()
            .map(KnowledgeChunkConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeChunkVo> selectPage(PageSelectListDto<KnowledgeChunkSelectDto> selectDto) {
        Page<KnowledgeChunkPo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeChunkDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeChunkVo> result = page.getResult()
            .stream()
            .map(KnowledgeChunkConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeChunkDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;
        if (creating) {
            dto.setOrganId(organId);
            Validations.validateSave(dto);
            KnowledgeChunkPo entity = KnowledgeChunkConverter.INSTANCE.dto2po(dto);
            entity.setId(idGenerator.generateId());
            entity.setOrganId(organId);
            LocalDateTime now = Moments.now();
            entity.setUpdateTime(now);
            entity.setCreateTime(now);
            Asserts.greaterThan(knowledgeChunkDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
            return entity.getId();
        }
        KnowledgeChunkPo existing = knowledgeChunkDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        Validations.validateSave(dto);
        KnowledgeChunkPo entity = KnowledgeChunkConverter.INSTANCE.dto2po(dto);
        entity.setId(existing.getId());
        entity.setOrganId(existing.getOrganId());
        entity.setCreateTime(existing.getCreateTime());
        entity.setUpdateTime(Moments.now());
        Asserts.greaterThan(knowledgeChunkDao.update(entity), 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeChunkPo existing = knowledgeChunkDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeChunkDao.delete(id);
    }
}