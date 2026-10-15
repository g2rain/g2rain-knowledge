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
        Validations.validateSave(dto);

        // 转换 DTO 为 PO
        KnowledgeTagPo entity = KnowledgeTagConverter.INSTANCE.dto2po(dto);

        // 判断是新增还是更新
        Long id = entity.getId();
        if (Objects.isNull(id) || id == 0) {
            // 新增：使用IdGenerator生成主键
            entity.setId(idGenerator.generateId());
            LocalDateTime now = Moments.now();
            entity.setUpdateTime(now);
            entity.setCreateTime(now);
            int success = knowledgeTagDao.insert(entity);
            Asserts.greaterThan(success, 0, SystemErrorCode.CREATE_DATA_ERROR);
        } else {
            KnowledgeTagPo existing = knowledgeTagDao.selectById(id);
            Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
            // 更新：直接更新
            entity.setUpdateTime(Moments.now());
            int success = knowledgeTagDao.update(entity);
            Asserts.greaterThan(success, 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        }

        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeTagPo existing = knowledgeTagDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        return knowledgeTagDao.delete(id);
    }
}