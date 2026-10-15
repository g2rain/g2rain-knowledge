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
        Validations.validateSave(dto);

        // 转换 DTO 为 PO
        KnowledgeFeedbackPo entity = KnowledgeFeedbackConverter.INSTANCE.dto2po(dto);

        // 判断是新增还是更新
        Long id = entity.getId();
        if (Objects.isNull(id) || id == 0) {
            // 新增：使用IdGenerator生成主键
            entity.setId(idGenerator.generateId());
            LocalDateTime now = Moments.now();
            entity.setUpdateTime(now);
            entity.setCreateTime(now);
            int success = knowledgeFeedbackDao.insert(entity);
            Asserts.greaterThan(success, 0, SystemErrorCode.CREATE_DATA_ERROR);
        } else {
            KnowledgeFeedbackPo existing = knowledgeFeedbackDao.selectById(id);
            Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
            // 更新：直接更新
            entity.setUpdateTime(Moments.now());
            int success = knowledgeFeedbackDao.update(entity);
            Asserts.greaterThan(success, 0, SystemErrorCode.UPDATE_DATA_ERROR, id);
        }

        return entity.getId();
    }

    @Override
    public int delete(Long id) {
        KnowledgeFeedbackPo existing = knowledgeFeedbackDao.selectById(id);
        Asserts.isTrue(Objects.nonNull(existing), SystemErrorCode.DATA_NOT_EXISTS, id);
        return knowledgeFeedbackDao.delete(id);
    }
}