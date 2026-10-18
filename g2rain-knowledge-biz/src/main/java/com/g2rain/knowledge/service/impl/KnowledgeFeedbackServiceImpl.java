package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeFeedbackConverter;
import com.g2rain.knowledge.dao.KnowledgeFeedbackDao;
import com.g2rain.knowledge.dao.po.KnowledgeFeedbackPo;
import com.g2rain.knowledge.dto.KnowledgeFeedbackDto;
import com.g2rain.knowledge.dto.KnowledgeFeedbackSelectDto;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeFeedbackService;
import com.g2rain.knowledge.vo.KnowledgeFeedbackVo;
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
 * 知识反馈表服务实现类
 * 表名: knowledge_feedback
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeFeedbackServiceImpl")
public class KnowledgeFeedbackServiceImpl implements KnowledgeFeedbackService {

    @Resource(name = "knowledgeFeedbackDao")
    private KnowledgeFeedbackDao knowledgeFeedbackDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    private IdGenerator idGenerator;

    @Qualifier("idGenerator")
    @Autowired(required = false)
    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public List<KnowledgeFeedbackVo> selectList(KnowledgeFeedbackSelectDto selectDto) {
        return knowledgeFeedbackDao.selectList(selectDto)
            .stream()
            .map(KnowledgeFeedbackConverter.INSTANCE::po2vo)
            .toList();
    }

    @Override
    public PageData<KnowledgeFeedbackVo> selectPage(PageSelectListDto<KnowledgeFeedbackSelectDto> selectDto) {
        Page<KnowledgeFeedbackPo> page = PageContext.of(selectDto.getPageNum(), selectDto.getPageSize(), () -> {
            knowledgeFeedbackDao.selectList(selectDto.getQuery());
        });
        List<KnowledgeFeedbackVo> result = page.getResult()
            .stream()
            .map(KnowledgeFeedbackConverter.INSTANCE::po2vo)
            .toList();
        return PageData.of(page.getPageNum(), page.getPageSize(), page.getTotal(), result);
    }

    @Override
    public Long save(KnowledgeFeedbackDto dto) {
        Long organId = knowledgeAccessService.requireOrganId();
        Long id = dto.getId();
        boolean creating = Objects.isNull(id) || id == 0;
        if (creating) {
            dto.setOrganId(organId);
            Validations.validateSave(dto);
            KnowledgeFeedbackPo entity = KnowledgeFeedbackConverter.INSTANCE.dto2po(dto);
            entity.setId(idGenerator.generateId());
            entity.setOrganId(organId);
            LocalDateTime now = Moments.now();
            entity.setUpdateTime(now);
            entity.setCreateTime(now);
            Asserts.greaterThan(knowledgeFeedbackDao.insert(entity), 0, SystemErrorCode.CREATE_DATA_ERROR);
            return entity.getId();
        }
        KnowledgeFeedbackPo existing = knowledgeFeedbackDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        Validations.validateSave(dto);
        KnowledgeFeedbackPo entity = KnowledgeFeedbackConverter.INSTANCE.dto2po(dto);
        entity.setId(existing.getId());
        entity.setOrganId(existing.getOrganId());
        entity.setCreateTime(existing.getCreateTime());
        entity.setUpdateTime(Moments.now());
        Asserts.greaterThan(knowledgeFeedbackDao.update(entity), 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeFeedbackPo existing = knowledgeFeedbackDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        knowledgeAccessService.assertSameOrgan(existing.getOrganId());
        return knowledgeFeedbackDao.delete(id);
    }
}