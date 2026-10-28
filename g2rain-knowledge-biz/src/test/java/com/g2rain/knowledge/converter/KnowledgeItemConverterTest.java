package com.g2rain.knowledge.converter;

import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.vo.KnowledgeItemVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeItemConverterTest {

    @Test
    void po2voExposesVersionForVisibilityCommand() {
        KnowledgeItemPo item = new KnowledgeItemPo();
        item.setVersion(7);

        KnowledgeItemVo vo = KnowledgeItemConverter.INSTANCE.po2vo(item);

        assertEquals(7, vo.getVersion());
    }
}
