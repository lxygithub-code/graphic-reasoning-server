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



2026-10-26
Update:清理国考试题重复的数据SQL脚本
-- ==============================================================================
-- 题库及关联业务表脏数据清理完整脚本
-- 版本：MySQL 8.0+
-- 说明：包含备份、清理重复题、清理孤儿数据、修复练习记录统计
-- 备份日期：2026-10-26
-- ==============================================================================

SET NAMES utf8mb4;

-- ==============================================================================
-- 第一阶段：全表备份（强烈建议执行！）
-- ==============================================================================
CREATE TABLE t_question_bak_20261026 LIKE t_question;
INSERT INTO t_question_bak_20261026 SELECT * FROM t_question;

-- 请根据实际表名取消下面注释并执行备份
CREATE TABLE t_favorite_bak_20261026 LIKE t_user_favorite;
INSERT INTO t_favorite_bak_20261026 SELECT * FROM t_user_favorite;

CREATE TABLE t_wrong_question_bak_20261026 LIKE t_user_wrong_question;
INSERT INTO t_wrong_question_bak_20261026 SELECT * FROM t_user_wrong_question;

CREATE TABLE t_user_question_bak_20261026 LIKE t_user_question;
INSERT INTO t_user_question_bak_20261026 SELECT * FROM t_user_question;

CREATE TABLE t_question_analysis_bak_20261026 LIKE t_question_analysis;
INSERT INTO t_question_analysis_bak_20261026 SELECT * FROM t_question_analysis;

CREATE TABLE t_practice_detail_bak_20261026 LIKE t_practice_detail;
INSERT INTO t_practice_detail_bak_20261026 SELECT * FROM t_practice_detail;

CREATE TABLE t_practice_record_bak_20261026 LIKE t_practice_record;
INSERT INTO t_practice_record_bak_20261026 SELECT * FROM t_practice_record;


-- ==============================================================================
-- 第二阶段：开启事务，准备清理数据
-- ==============================================================================
START TRANSACTION;

-- ------------------------------------------------------------------------------
-- 2.1 找出国考中重复的题目，保留 id 最小的，将多余的要删的 ID 存入临时表
-- ------------------------------------------------------------------------------
CREATE TEMPORARY TABLE tmp_duplicate_q_ids AS
WITH CTE AS (
SELECT
id,
ROW_NUMBER() OVER (
PARTITION BY content, correct_option, category
ORDER BY id ASC
) AS rn
FROM t_question
WHERE exam_type = 'guokao'
AND deleted = 0
)
SELECT id FROM CTE WHERE rn > 1;


-- ==============================================================================
-- 第三阶段：清理关联表数据（先清理因“重复”要删的，再清理“孤儿”）
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 3.1 清理因“重复题”而关联的脏数据
-- ------------------------------------------------------------------------------
-- 清理收藏表
DELETE FROM t_favorite
WHERE question_id IN (SELECT id FROM tmp_duplicate_q_ids);

-- 清理错题表
DELETE FROM t_wrong_question
WHERE question_id IN (SELECT id FROM tmp_duplicate_q_ids);

-- 清理解析表
DELETE FROM t_question_analysis
WHERE question_id IN (SELECT id FROM tmp_duplicate_q_ids);

-- 清理用户做题状态表
DELETE FROM t_user_question
WHERE question_id IN (SELECT id FROM tmp_duplicate_q_ids);

-- 清理练习明细表
DELETE FROM t_practice_detail
WHERE question_id IN (SELECT id FROM tmp_duplicate_q_ids);


-- ------------------------------------------------------------------------------
-- 3.2 清理主题库表（删除重复的题目）
-- ------------------------------------------------------------------------------
DELETE FROM t_question
WHERE id IN (SELECT id FROM tmp_duplicate_q_ids);

-- 清理临时表
DROP TEMPORARY TABLE tmp_duplicate_q_ids;


-- ------------------------------------------------------------------------------
-- 3.3 清理因“主表已删”导致的孤儿数据（兜底清理）
-- ------------------------------------------------------------------------------
-- 清理孤儿收藏记录
DELETE f FROM t_user_favorite f
LEFT JOIN t_question q ON f.question_id = q.id
WHERE q.id IS NULL;

-- 清理孤儿错题记录
DELETE w FROM t_user_wrong_question w
LEFT JOIN t_question q ON w.question_id = q.id
WHERE q.id IS NULL;

-- 清理孤儿解析记录
DELETE qa FROM t_question_analysis qa
LEFT JOIN t_question q ON qa.question_id = q.id
WHERE q.id IS NULL;

-- 清理孤儿用户状态记录
DELETE uq FROM t_user_question uq
LEFT JOIN t_question q ON uq.question_id = q.id
WHERE q.id IS NULL;

-- 清理孤儿练习明细记录
DELETE pd FROM t_practice_detail pd
LEFT JOIN t_question q ON pd.question_id = q.id
WHERE q.id IS NULL;


-- ==============================================================================
-- 第四阶段：修复练习记录主表（清理空记录并重算正确率）
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 4.1 删除没有任何明细的练习记录
-- ------------------------------------------------------------------------------
DELETE pr FROM t_practice_record pr
LEFT JOIN t_practice_detail pd ON pr.id = pd.record_id
WHERE pd.id IS NULL;

-- ------------------------------------------------------------------------------
-- 4.2 重新计算并更新剩余练习记录的统计数据
-- ------------------------------------------------------------------------------
UPDATE t_practice_record pr
JOIN (
SELECT
record_id,
COUNT(*) AS new_total,
SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) AS new_correct,
SUM(duration) AS new_duration
FROM t_practice_detail
GROUP BY record_id
) calc ON pr.id = calc.record_id
SET
pr.total_count = calc.new_total,
pr.correct_count = calc.new_correct,
pr.total_duration = calc.new_duration,
-- 使用 ROUND 控制保留两位小数，避免产生警告
pr.accuracy = IF(calc.new_total = 0, 0, ROUND((calc.new_correct / calc.new_total) * 100, 2));


-- ==============================================================================
-- 第五阶段：提交事务
-- ==============================================================================
COMMIT;

-- 如果需要回滚，请将 COMMIT; 替换为 ROLLBACK;
