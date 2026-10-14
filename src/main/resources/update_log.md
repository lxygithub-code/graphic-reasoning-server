2026-10-24
ADD:用户评论管理 与 管理员消息提醒

SQL:

ALTER TABLE `t_question_comment`
ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0隐藏' AFTER `like_count`,
ADD COLUMN `audit_remark` VARCHAR(255) DEFAULT '' COMMENT '管理备注' AFTER `status`;

CREATE TABLE `t_admin_message` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`admin_id` BIGINT DEFAULT NULL COMMENT '管理员ID，NULL=所有管理员都可见',
`type` VARCHAR(32) NOT NULL COMMENT '类型：comment/question/system',
`title` VARCHAR(200) NOT NULL COMMENT '消息标题',
`content` VARCHAR(500) DEFAULT '' COMMENT '摘要内容',
`ref_id` BIGINT DEFAULT NULL COMMENT '关联业务ID（如评论ID）',
`ref_question_id` BIGINT DEFAULT NULL COMMENT '关联题目ID（跳转用）',
`is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `idx_admin_read` (`admin_id`, `is_read`),
KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员消息表';