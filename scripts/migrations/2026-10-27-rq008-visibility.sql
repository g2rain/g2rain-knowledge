-- RQ-008: knowledge_item.visibility (idempotent-ish for MySQL 8+)
-- Run once on existing databases; do not re-run g2rain-knowledge.sql DROP scripts.

ALTER TABLE `knowledge_item`
    ADD COLUMN `visibility` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE'
        COMMENT '共享范围[PRIVATE:仅本机构, PUBLIC:公共知识]' AFTER `tags_json`;

ALTER TABLE `knowledge_item`
    ADD INDEX `idx_visibility_del` (`visibility`, (IF(delete_flag = 0, 0, NULL)));
