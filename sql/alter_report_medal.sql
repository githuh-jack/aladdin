-- =====================================================================
-- 存量库升级脚本：信件举报 + 奖章模块 + 禁闭处罚 (2026-10-14)
-- 全新库直接执行 sql/init.sql 即可，无需本脚本
-- =====================================================================

-- 1. 信件举报表
CREATE TABLE IF NOT EXISTS `biz_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `letter_id` bigint(20) NOT NULL COMMENT '被举报信件ID',
  `reporter_id` bigint(20) NOT NULL COMMENT '举报人ID(收件人)',
  `reported_id` bigint(20) NOT NULL COMMENT '被举报人ID(信件发件人)',
  `category` varchar(30) NOT NULL COMMENT '举报选项(垃圾广告/辱骂攻击/色情低俗/违法违规/诈骗钓鱼/泄露隐私/其他)',
  `description` varchar(500) DEFAULT '' COMMENT '其他说明(选其他时必填)',
  `status` int(11) DEFAULT '0' COMMENT '0待处理 1已处理',
  `reward_coins` int(11) DEFAULT '0' COMMENT '奖励举报人铜钱数',
  `reward_stamp_id` bigint(20) DEFAULT NULL COMMENT '奖励举报人邮票ID',
  `reward_credit` int(11) DEFAULT '0' COMMENT '奖励举报人信用分',
  `reward_medal_id` bigint(20) DEFAULT NULL COMMENT '奖励举报人奖章ID',
  `confine_days` int(11) DEFAULT '0' COMMENT '被举报人禁闭天数(0=不禁闭)',
  `confine_started` int(11) DEFAULT '0' COMMENT '禁闭是否已起算(0待领取处罚邮件 1已起算)',
  `deduct_credit` int(11) DEFAULT '0' COMMENT '被举报人扣减信用分',
  `reporter_mail_id` bigint(20) DEFAULT NULL COMMENT '发给举报人的回复邮件ID',
  `reported_mail_id` bigint(20) DEFAULT NULL COMMENT '发给被举报人的处罚通知邮件ID',
  `admin_id` bigint(20) DEFAULT NULL COMMENT '处理管理员ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(200) DEFAULT '' COMMENT '处理备注',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_letter_reporter` (`letter_id`,`reporter_id`),
  KEY `idx_reported_id` (`reported_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信件举报';

-- 2. 奖章定义表
CREATE TABLE IF NOT EXISTS `biz_medal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '奖章名称',
  `icon` varchar(500) DEFAULT '' COMMENT '奖章图标URL',
  `description` varchar(200) DEFAULT '' COMMENT '奖章说明',
  `status` int(11) DEFAULT '1' COMMENT '0停用 1启用',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖章定义';

-- 3. 用户奖章表
CREATE TABLE IF NOT EXISTS `biz_user_medal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `medal_id` bigint(20) NOT NULL COMMENT '奖章ID',
  `source` varchar(30) DEFAULT '' COMMENT '来源(report_reward举报奖励)',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_medal` (`user_id`,`medal_id`),
  KEY `idx_medal_id` (`medal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户奖章';

-- 4. biz_user 禁闭字段
ALTER TABLE `biz_user` ADD COLUMN `confine_days` int(11) DEFAULT '0' COMMENT '禁闭天数(0=无，举报处罚不能寄信)';
ALTER TABLE `biz_user` ADD COLUMN `confine_start` datetime DEFAULT NULL COMMENT '禁闭起算时间(领取/查看处罚通知邮件时写入)';

-- 5. 预置奖章
INSERT IGNORE INTO `biz_medal` (`id`, `name`, `icon`, `description`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '正义信使', '', '举报违规信件有功，颁发给正义的举报人', 1, NOW(), 1, 1, 'system'),
(2, '邮政之光', '', '维护纸船信箱纯净的荣誉勋章', 1, NOW(), 1, 1, 'system'),
(3, '守信之星', '', '诚实守信、信用良好的用户勋章', 1, NOW(), 1, 1, 'system');
