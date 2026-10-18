package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Accesses the G2rain scoped principal without accepting tenant identifiers
 * from knowledge command bodies.
 */
@Service
public class KnowledgeAccessServiceImpl implements KnowledgeAccessService {
    @Override
    public Long requireOrganId() {
        Long organId = PrincipalContextHolder.getOrganId();
        if (Objects.isNull(organId)) {
            throw new BusinessException(SystemErrorCode.UNAUTHENTICATED, "缺少机构主体");
        }
        return organId;
    }

    @Override
    public Long currentUserId() {
        return PrincipalContextHolder.getUserId();
    }

    @Override
    public Long requireUserId() {
        Long userId = currentUserId();
        if (Objects.isNull(userId)) {
            throw new BusinessException(SystemErrorCode.UNAUTHENTICATED, "缺少用户主体");
        }
        return userId;
    }

    @Override
    public void assertSameOrgan(Long recordOrganId) {
        if (!Objects.equals(requireOrganId(), recordOrganId)) {
            throw new BusinessException(SystemErrorCode.UNAUTHORIZED, "跨机构知识数据");
        }
    }
}
