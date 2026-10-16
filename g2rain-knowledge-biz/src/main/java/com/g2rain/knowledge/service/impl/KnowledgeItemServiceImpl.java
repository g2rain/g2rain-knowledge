package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeItemConverter;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dto.KnowledgeItemDto;
import com.g2rain.knowledge.dto.KnowledgeItemSelectDto;
import com.g2rain.knowledge.dto.KnowledgeRevisionSelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeItemService;
import com.g2rain.knowledge.service.KnowledgeSaveSupport;
import com.g2rain.knowledge.vo.KnowledgeItemVo;
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
 * 知识主题表服务实现类
 * 表名: knowledge_item
 *
 * @author G2rain Generator
 */
@Service(value = "knowledgeItemServiceImpl")
public class KnowledgeItemServiceImpl implements KnowledgeItemService {

    @Resource(name = "knowledgeItemDao")
    private KnowledgeItemDao knowledgeItemDao;
    @Resource
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;
    @Resource
    private KnowledgeSaveSupport knowledgeSaveSupport;

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

    private boolean hasRevisions(Long knowledgeId) {
        KnowledgeRevisionSelectDto query = new KnowledgeRevisionSelectDto();
        query.setKnowledgeId(knowledgeId);
        return !knowledgeRevisionDao.selectList(query).isEmpty();
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
