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



ADD:记录小程序日活/时活

SQL:
ALTER TABLE t_user
ADD COLUMN `last_active_time` DATETIME DEFAULT NULL COMMENT '最近活跃时间' AFTER `last_login_time`;


CREATE TABLE `t_user_daily_active` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`user_id` BIGINT NOT NULL,
`active_date` DATE NOT NULL COMMENT '活跃日期',
`request_count` INT NOT NULL DEFAULT 1 COMMENT '当日请求次数',
`first_time` DATETIME NOT NULL COMMENT '当日首次活跃时间',
`last_time` DATETIME NOT NULL COMMENT '当日最后活跃时间',
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_date` (`user_id`, `active_date`),
KEY `idx_date` (`active_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日活跃表';


CREATE TABLE `t_user_hourly_active` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`user_id` BIGINT NOT NULL,
`active_date` DATE NOT NULL COMMENT '活跃日期',
`active_hour` TINYINT NOT NULL COMMENT '活跃小时 0-23',
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_date_hour` (`user_id`, `active_date`, `active_hour`),
KEY `idx_date_hour` (`active_date`, `active_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每小时活跃表';



2026-10-15

ADD:小程序新增意见箱

SQL：

CREATE TABLE `t_feedback` (
`id` BIGINT NOT NULL AUTO_INCREMENT,
`user_id` BIGINT NOT NULL COMMENT '提交用户ID',
`content` TEXT NOT NULL COMMENT '意见内容',
`status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已解决',
`reply_content` TEXT DEFAULT NULL COMMENT '管理员回复内容',
`reply_admin_id` BIGINT DEFAULT NULL COMMENT '回复的管理员ID',
`reply_admin_name` VARCHAR(100) DEFAULT '' COMMENT '回复的管理员名称（快照）',
`reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
`user_read` TINYINT NOT NULL DEFAULT 1 COMMENT '用户是否已读回复 0未读 1已读',
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`deleted` TINYINT NOT NULL DEFAULT 0,
PRIMARY KEY (`id`),
KEY `idx_user` (`user_id`),
KEY `idx_status` (`status`),
KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户意见反馈表';
