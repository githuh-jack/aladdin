-- ================================================================
-- Aladdin 业务模块初始化 SQL
-- 说明：信件/好友/日记/感想/其他/邮票/信封/铜钱/订单
-- 依赖：sys_user 表（来自 init.sql，需先执行）
-- 幂等可重复执行
-- ================================================================

-- ==============================
-- 表：信件
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_letter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL COMMENT '发信人ID',
  `receiver_id` bigint DEFAULT NULL COMMENT '收信人ID(地址信件时为空)',
  `receiver_address` varchar(200) DEFAULT '' COMMENT '收件地址(寄给非好友时填写)',
  `title` varchar(200) NOT NULL,
  `content` text,
  `status` int DEFAULT 0 COMMENT '0草稿 1已发送 2已读 3已删除',
  `stamp_id` bigint DEFAULT NULL COMMENT '使用的邮票ID',
  `envelope_id` bigint DEFAULT NULL COMMENT '使用的信封ID',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信件';

-- ==============================
-- 表：好友关系
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '发起方ID',
  `friend_id` bigint NOT NULL COMMENT '被加方ID',
  `status` int DEFAULT 0 COMMENT '0申请中 1已通过 2已拒绝 3已删除 4已拉黑',
  `apply_remark` varchar(200) DEFAULT '' COMMENT '申请备注',
  `add_time` datetime DEFAULT NULL COMMENT '通过时间',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系';

-- ==============================
-- 表：日记
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_diary` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) DEFAULT '',
  `content` text,
  `weather` varchar(20) DEFAULT '' COMMENT '天气',
  `mood` varchar(20) DEFAULT '' COMMENT '心情',
  `write_date` date DEFAULT NULL COMMENT '日记日期',
  `is_public` int DEFAULT 0 COMMENT '0私密 1公开',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记';

-- ==============================
-- 表：其他笔记
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_note` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(200) DEFAULT '',
  `content` text,
  `note_type` varchar(30) DEFAULT '其他' COMMENT '笔记分类',
  `is_public` int DEFAULT 0,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='其他笔记';

-- ==============================
-- 表：邮票
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_stamp` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT '',
  `image_url` varchar(500) DEFAULT '',
  `price` int NOT NULL DEFAULT 0 COMMENT '铜钱单价',
  `stock` int DEFAULT 0 COMMENT '库存, -1不限',
  `stamp_type` varchar(20) DEFAULT '普通' COMMENT '普通/稀有/限量',
  `theme` varchar(50) DEFAULT '经典' COMMENT '邮票主题(收藏分类)',
  `status` int DEFAULT 1 COMMENT '0下架 1上架',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮票';

-- ==============================
-- 表：用户持有邮票
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_user_stamp` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `stamp_id` bigint NOT NULL,
  `count` int DEFAULT 0 COMMENT '可用数量',
  `used_count` int DEFAULT 0 COMMENT '已使用数量',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_stamp` (`user_id`, `stamp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持有邮票';

-- ==============================
-- 表：信封
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_envelope` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT '',
  `image_url` varchar(500) DEFAULT '',
  `price` int NOT NULL DEFAULT 0,
  `stock` int DEFAULT 0,
  `envelope_type` varchar(20) DEFAULT '普通',
  `status` int DEFAULT 1,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信封';

-- ==============================
-- 表：用户持有信封
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_user_envelope` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `envelope_id` bigint NOT NULL,
  `count` int DEFAULT 0,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_envelope` (`user_id`, `envelope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持有信封';

-- ==============================
-- 表：用户铜钱账户
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_coin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `balance` int DEFAULT 0 COMMENT '当前铜钱余额',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户铜钱账户';

-- ==============================
-- 表：铜钱流水
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_coin_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `change_amount` int NOT NULL COMMENT '变动数量(正负)',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `log_type` varchar(20) NOT NULL COMMENT 'recharge/shop/send_letter/admin_adjust',
  `ref_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `remark` varchar(200) DEFAULT '',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='铜钱流水';

-- ==============================
-- 表：订单
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `item_type` varchar(20) NOT NULL COMMENT 'stamp/envelope',
  `item_id` bigint NOT NULL,
  `item_name` varchar(100) DEFAULT '' COMMENT '冗余商品名',
  `quantity` int NOT NULL DEFAULT 1,
  `unit_price` int NOT NULL,
  `total_price` int NOT NULL,
  `status` int DEFAULT 1 COMMENT '1已完成 2已退款',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- ==============================
-- 表：互动(点赞/收藏)
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_interaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `target_type` varchar(20) NOT NULL COMMENT 'thought/note',
  `target_id` bigint NOT NULL,
  `action` varchar(10) NOT NULL COMMENT 'like/fav',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interaction` (`user_id`, `target_type`, `target_id`, `action`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动(点赞/收藏)';

-- ==============================
-- 表：评论
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_type` varchar(20) NOT NULL COMMENT 'thought/note',
  `target_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) NOT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论';

-- ==============================
-- 表：用户资料
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_user_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID(sys_user.id)',
  `invite_code` varchar(16) DEFAULT '' COMMENT '邀请码',
  `gender` int DEFAULT 0 COMMENT '性别 0保密 1男 2女',
  `region` varchar(100) DEFAULT '' COMMENT '地区',
  `region_secret` int DEFAULT 1 COMMENT '地区是否保密 0公开 1保密',
  `age` int DEFAULT NULL COMMENT '年龄',
  `age_secret` int DEFAULT 1 COMMENT '年龄是否保密 0公开 1保密',
  `signature` varchar(200) DEFAULT '' COMMENT '个性签名',
  `avatar_url` varchar(500) DEFAULT '' COMMENT '头像URL(审核通过后的生效头像)',
  `avatar_status` int DEFAULT 0 COMMENT '头像状态 0无 1待审 2已通过 3已拒绝',
  `credit_score` int DEFAULT 100 COMMENT '信用分',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料';

-- ==============================
-- 表：头像审核记录
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_avatar_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `file_name` varchar(200) DEFAULT '' COMMENT '存储文件名',
  `image_url` varchar(500) DEFAULT '' COMMENT '访问URL',
  `status` int DEFAULT 0 COMMENT '审核状态 0待审 1通过 2拒绝',
  `remark` varchar(200) DEFAULT '' COMMENT '审核备注',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='头像审核记录';

-- ==============================
-- 表：信用分变动记录
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_credit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `change_val` int DEFAULT 0 COMMENT '变动值(正加负减)',
  `balance` int DEFAULT 0 COMMENT '变动后余额',
  `reason` varchar(200) DEFAULT '' COMMENT '变动原因',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分变动记录';

-- ==============================
-- 表：邀请记录
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_invite_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inviter_id` bigint NOT NULL COMMENT '邀请人ID',
  `invitee_id` bigint NOT NULL COMMENT '被邀请人ID',
  `invite_code` varchar(16) DEFAULT '' COMMENT '使用的邀请码',
  `reward_coins` int DEFAULT 0 COMMENT '邀请人获得的铜钱奖励',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invitee_id` (`invitee_id`),
  KEY `idx_inviter_id` (`inviter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请记录';

-- ==============================
-- 表：系统配置(奖励金额/价格等，后台可改)
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(200) DEFAULT '' COMMENT '配置值',
  `remark` varchar(200) DEFAULT '' COMMENT '说明',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

-- ==============================
-- 表：用户登录日志(后台查看)
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `user_name` varchar(64) DEFAULT '',
  `login_name` varchar(64) DEFAULT '',
  `login_ip` varchar(64) DEFAULT '',
  `login_location` varchar(128) DEFAULT '',
  `login_type` varchar(32) DEFAULT '',
  `code` int DEFAULT NULL,
  `message` varchar(255) DEFAULT '',
  `login_time` datetime DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志';

-- ==============================
-- 表：文竹(每个用户一株)与生长记录(仅后台)
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_bamboo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `height_cm` int DEFAULT 0 COMMENT '当前高度(cm)',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户文竹';

CREATE TABLE IF NOT EXISTS `biz_bamboo_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `biz_type` varchar(20) DEFAULT '' COMMENT 'letter/diary',
  `add_cm` int DEFAULT 0 COMMENT '本次生长(cm)',
  `content_len` int DEFAULT 0 COMMENT '内容字数',
  `ref_id` bigint DEFAULT NULL COMMENT '关联信件/日记ID',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sys001` (`sys001`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文竹生长记录';

-- ==============================
-- 表：每日签到/登录记录
-- ==============================
CREATE TABLE IF NOT EXISTS `biz_checkin_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `checkin_date` date NOT NULL COMMENT '签到日期',
  `coin` int DEFAULT 0 COMMENT '本次获得铜钱',
  `streak_days` int DEFAULT 1 COMMENT '连续签到天数',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `checkin_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到记录';

-- ==============================
-- 初始数据：奖励与价格配置
-- ==============================
INSERT IGNORE INTO `biz_config` (`config_key`, `config_value`, `remark`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
('reward.register', '200', '注册奖励(文)，一次性', NOW(), 1, 1, 'system'),
('reward.daily_login', '10', '每日登录奖励(文)，每日限1次', NOW(), 1, 1, 'system'),
('reward.streak_7', '50', '连续签到7天额外奖励(文)', NOW(), 1, 1, 'system'),
('reward.streak_30', '100', '连续签到30天额外奖励(文)', NOW(), 1, 1, 'system'),
('reward.profile_complete', '50', '完善资料奖励(文)，一次性', NOW(), 1, 1, 'system'),
('reward.first_letter_daily', '10', '当日第一封信奖励(文)', NOW(), 1, 1, 'system'),
('reward.first_diary_daily', '10', '当日写日记奖励(文)', NOW(), 1, 1, 'system'),
('reward.invite_register', '30', '邀请码注册奖励(文)，发放给邀请人', NOW(), 1, 1, 'system'),
('price.stamp_basic', '30', '最基础单张邮票价格(文)', NOW(), 1, 1, 'system'),
('bamboo.letter.thresholds', '100,300,600,1000,1500', '写信文竹生长字数阈值，每达一档+1cm，单封最多5cm', NOW(), 1, 1, 'system'),
('bamboo.diary.threshold', '100', '日记文竹生长字数阈值(字)', NOW(), 1, 1, 'system'),
('bamboo.diary.daily_max_cm', '1', '日记文竹每日最多生长(cm)', NOW(), 1, 1, 'system');

-- ==============================
-- 存量库迁移（幂等）：信件收件地址 / 收信人可空 / 邮票主题与使用量
-- ==============================
DROP PROCEDURE IF EXISTS `biz_migrate`;
DELIMITER $$
CREATE PROCEDURE `biz_migrate`()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_letter' AND COLUMN_NAME = 'receiver_address') THEN
    ALTER TABLE `biz_letter`
      ADD COLUMN `receiver_address` varchar(200) DEFAULT '' COMMENT '收件地址(寄给非好友时填写)' AFTER `receiver_id`,
      MODIFY COLUMN `receiver_id` bigint DEFAULT NULL COMMENT '收信人ID(地址信件时为空)';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_letter' AND COLUMN_NAME = 'receiver_id'
               AND IS_NULLABLE = 'NO') THEN
    ALTER TABLE `biz_letter` MODIFY COLUMN `receiver_id` bigint DEFAULT NULL COMMENT '收信人ID(地址信件时为空)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_stamp' AND COLUMN_NAME = 'theme') THEN
    ALTER TABLE `biz_stamp` ADD COLUMN `theme` varchar(50) DEFAULT '经典' COMMENT '邮票主题(收藏分类)' AFTER `stamp_type`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_user_stamp' AND COLUMN_NAME = 'used_count') THEN
    ALTER TABLE `biz_user_stamp` ADD COLUMN `used_count` int DEFAULT 0 COMMENT '已使用数量' AFTER `count`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_user_profile' AND COLUMN_NAME = 'avatar_status') THEN
    ALTER TABLE `biz_user_profile` ADD COLUMN `avatar_status` int DEFAULT 0 COMMENT '头像状态 0无 1待审 2已通过 3已拒绝' AFTER `avatar_url`;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'invite_code') THEN
    ALTER TABLE `sys_user` ADD COLUMN `invite_code` varchar(16) DEFAULT '' COMMENT '注册时填写的邀请码';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_invite_record' AND COLUMN_NAME = 'reward_credit')
     AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_invite_record' AND COLUMN_NAME = 'reward_coins') THEN
    ALTER TABLE `biz_invite_record` CHANGE COLUMN `reward_credit` `reward_coins` int DEFAULT 0 COMMENT '邀请人获得的铜钱奖励';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_user_profile' AND COLUMN_NAME = 'reward_flags') THEN
    ALTER TABLE `biz_user_profile` ADD COLUMN `reward_flags` int DEFAULT 0 COMMENT '一次性奖励标记位 1注册 2完善资料' AFTER `credit_score`;
  END IF;
END$$
DELIMITER ;
CALL `biz_migrate`();
DROP PROCEDURE IF EXISTS `biz_migrate`;

-- ==============================
-- 初始数据：默认邮票/信封
-- ==============================
INSERT INTO `biz_stamp` (`name`, `description`, `image_url`, `price`, `stock`, `stamp_type`, `theme`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
('普通梅花邮票', '最常见的梅花图案邮票', '', 30, 100, '普通', '花草', 1, NOW(), 1, 1, 'system'),
('青花瓷邮票', '精美青花瓷图案', '', 50, 50, '稀有', '文化', 1, NOW(), 1, 1, 'system'),
('限量纪念邮票', '十周年纪念限量版', '', 200, 10, '限量', '纪念', 1, NOW(), 1, 1, 'system');

INSERT INTO `biz_envelope` (`name`, `description`, `image_url`, `price`, `stock`, `envelope_type`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
('普通信封', '牛皮纸普通信封', '', 5, 200, '普通', 1, NOW(), 1, 1, 'system'),
('精美印花信封', '印花装饰信封', '', 20, 100, '精美', 1, NOW(), 1, 1, 'system');

-- ==============================
-- 触发器：注册新用户时初始化铜钱账户（注册奖励，金额取 biz_config: reward.register，默认200）
-- ==============================
DROP TRIGGER IF EXISTS `tr_sys_user_after_insert`;
DELIMITER $$
CREATE TRIGGER `tr_sys_user_after_insert`
AFTER INSERT ON `sys_user`
FOR EACH ROW
BEGIN
  DECLARE v_reward INT DEFAULT 200;
  SELECT CAST(config_value AS UNSIGNED) INTO v_reward FROM `biz_config` WHERE `config_key` = 'reward.register' LIMIT 1;
  INSERT INTO `biz_coin` (`user_id`, `balance`, `sys001`, `sys003`, `sys005`, `sys006`)
  VALUES (NEW.id, v_reward, NOW(), 1, 1, 'system')
  ON DUPLICATE KEY UPDATE `balance` = `balance`;
  INSERT INTO `biz_coin_log` (`user_id`, `change_amount`, `balance_after`, `log_type`, `remark`, `sys001`, `sys003`, `sys005`, `sys006`)
  VALUES (NEW.id, v_reward, v_reward, 'recharge', CONCAT('注册奖励', v_reward, '文'), NOW(), 1, 1, 'system');
END$$
DELIMITER ;
