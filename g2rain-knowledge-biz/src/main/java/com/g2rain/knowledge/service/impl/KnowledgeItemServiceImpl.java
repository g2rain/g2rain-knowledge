package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.utils.Asserts;
import com.g2rain.common.utils.Moments;
import com.g2rain.common.validation.Validations;
import com.g2rain.knowledge.converter.KnowledgeItemConverter;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dto.KnowledgeItemDto;
import com.g2rain.knowledge.dto.KnowledgeItemSelectDto;
import com.g2rain.knowledge.service.KnowledgeItemService;
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
        Validations.validateSave(dto);

        // 转换 DTO 为 PO
        KnowledgeItemPo entity = KnowledgeItemConverter.INSTANCE.dto2po(dto);

        // 判断是新增还是更新
        Long id = entity.getId();
        if (Objects.isNull(id) || id == 0) {
            // 新增：使用IdGenerator生成主键
            entity.setId(idGenerator.generateId());
            LocalDateTime now = Moments.now();
            entity.setUpdateTime(now);
            entity.setCreateTime(now);
            int success = knowledgeItemDao.insert(entity);
            Asserts.greaterThan(success, 0, SystemErrorCode.CREATE_DATA_ERROR);
        } else {
            KnowledgeItemPo existing = knowledgeItemDao.selectById(id);
            Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
            // 更新：直接更新
            entity.setUpdateTime(Moments.now());
            int success = knowledgeItemDao.update(entity);
            Asserts.greaterThan(success, 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        }

        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeItemPo existing = knowledgeItemDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        return knowledgeItemDao.delete(id);
    }
}