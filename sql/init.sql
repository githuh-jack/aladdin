-- ================================================================
-- Aladdin 系统初始化 SQL（全新库执行）
-- 结构来源：2026-09-29 线上库实际结构导出（43张表 + 触发器）
-- 组织方式：先建全部表结构（sys_ 系统表 + biz_ 业务表），
--           触发器与全部数据插入集中在文件最后
-- 依赖：MySQL 8.0；建议执行方式：
--       mysql -uroot -p --default-character-set=utf8mb4 < init.sql
-- 业务表 biz_user AUTO_INCREMENT 从 10000 起（与 sys_user 的 id 空间隔离）
-- ================================================================

-- ================================================================
-- 绗竴閮ㄥ垎锛氱郴缁熻〃(sys_)
-- ================================================================

-- 琛細应用表(sys_app)
CREATE TABLE IF NOT EXISTS `sys_app` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_name` varchar(100) NOT NULL COMMENT '应用名称',
  `app_code` varchar(50) NOT NULL COMMENT '应用编码',
  `description` varchar(500) DEFAULT '' COMMENT '应用描述',
  `allow_register` int(11) DEFAULT '1' COMMENT '是否准许注册 0否 1是',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '关联根部门ID',
  `menu_id` bigint(20) DEFAULT NULL COMMENT '关联菜单根节点ID',
  `init_sql` longtext COMMENT '应用相关表初始化DDL语句(分号分隔)',
  `tables_initialized` int(11) DEFAULT '0' COMMENT '相关表是否已初始化 0否 1是',
  `status` int(11) DEFAULT '1',
  `tenant_id` bigint(20) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_code` (`app_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用表';

-- 琛細sys_dept
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT '0',
  `dept_name` varchar(30) DEFAULT '',
  `sort` int(11) DEFAULT '0',
  `leader` varchar(20) DEFAULT '',
  `phone` varchar(20) DEFAULT '',
  `email` varchar(50) DEFAULT '',
  `status` int(11) DEFAULT '1',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_dict_data
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dict_type_id` bigint(20) DEFAULT NULL,
  `dict_label` varchar(100) DEFAULT '',
  `dict_value` varchar(100) DEFAULT '',
  `sort` int(11) DEFAULT '0',
  `css_class` varchar(100) DEFAULT '',
  `list_class` varchar(100) DEFAULT '',
  `status` int(11) DEFAULT '1',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_dict_type
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dict_name` varchar(100) DEFAULT '',
  `dict_type` varchar(100) DEFAULT '',
  `status` int(11) DEFAULT '1',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_login_log
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(64) DEFAULT '',
  `user_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(50) DEFAULT '',
  `login_name` varchar(50) DEFAULT '',
  `login_ip` varchar(128) DEFAULT '',
  `login_location` varchar(255) DEFAULT '',
  `login_type` varchar(2) DEFAULT '',
  `token` varchar(500) DEFAULT '',
  `session_id` varchar(64) DEFAULT '',
  `code` int(11) DEFAULT NULL,
  `message` varchar(255) DEFAULT '',
  `login_time` datetime DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細系统默认菜单表(sys_menu)
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父菜单ID',
  `menu_name` varchar(100) NOT NULL COMMENT '路由name(唯一)',
  `title` varchar(100) DEFAULT '' COMMENT '菜单标题(i18n key)',
  `icon` varchar(255) DEFAULT '' COMMENT '图标名或URL',
  `path` varchar(200) DEFAULT '' COMMENT '路由路径',
  `component` varchar(200) DEFAULT '' COMMENT '组件路径或布局名(BasicLayout/IFrameView/具体路径)',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `affix_tab` int(11) DEFAULT '0' COMMENT '是否固定tab 0否1是',
  `keep_alive` int(11) DEFAULT '0' COMMENT '是否缓存 0否1是',
  `badge_type` varchar(20) DEFAULT '' COMMENT '徽标类型(dot等)',
  `link` varchar(500) DEFAULT '' COMMENT '外链URL',
  `status` int(11) DEFAULT '1' COMMENT '状态 0停用 1启用',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统默认菜单表';

-- 琛細sys_operation_log
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(64) DEFAULT '',
  `user_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(50) DEFAULT '',
  `operation` varchar(50) DEFAULT '',
  `method` varchar(200) DEFAULT '',
  `request_url` varchar(255) DEFAULT '',
  `request_method` varchar(10) DEFAULT '',
  `request_param` text,
  `response_result` text,
  `ip` varchar(128) DEFAULT '',
  `cost` bigint(20) DEFAULT NULL,
  `status` int(11) DEFAULT '1',
  `error_msg` text,
  `operation_time` datetime DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_resource
CREATE TABLE IF NOT EXISTS `sys_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_name` varchar(50) NOT NULL,
  `parent_id` bigint(20) DEFAULT '0',
  `sort` int(11) DEFAULT '0',
  `path` varchar(200) DEFAULT '',
  `component` varchar(200) DEFAULT '',
  `resource_type` varchar(1) DEFAULT '',
  `perms` varchar(100) DEFAULT '',
  `icon` varchar(100) DEFAULT '',
  `status` int(11) DEFAULT '1',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_role
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_key` varchar(50) NOT NULL,
  `sort` int(11) DEFAULT '0',
  `data_scope` int(11) DEFAULT '1',
  `status` int(11) DEFAULT '1',
  `remark` varchar(64) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_role_dept
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL,
  `dept_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_role_resource
CREATE TABLE IF NOT EXISTS `sys_role_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(20) NOT NULL,
  `resource_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細sys_user
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(200) NOT NULL,
  `nickname` varchar(50) DEFAULT '',
  `real_name` varchar(50) DEFAULT '' COMMENT '姓名',
  `email` varchar(50) DEFAULT '',
  `phone` varchar(20) DEFAULT '',
  `avatar` varchar(200) DEFAULT '',
  `dept_id` bigint(20) DEFAULT NULL,
  `status` int(11) DEFAULT '1',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  `invite_code` varchar(16) DEFAULT '' COMMENT '注册时填写的邀请码',
  `pwd_change_time` datetime DEFAULT NULL COMMENT '密码最后修改时间',
  `pwd_force_change` int(11) DEFAULT '0' COMMENT '是否首次登录需修改密码 0否1是',
  `tenant_id` bigint(20) DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 琛細用户部门关联表(sys_user_dept)
CREATE TABLE IF NOT EXISTS `sys_user_dept` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `dept_id` bigint(20) NOT NULL,
  `is_primary` int(11) DEFAULT '0' COMMENT '是否主部门 0否 1是',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dept` (`user_id`,`dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户部门关联表';

-- 琛細sys_user_role
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ================================================================
-- 绗簩閮ㄥ垎锛氫笟鍔¤〃(biz_)
-- ================================================================

-- 琛細公告(biz_announcement)
CREATE TABLE IF NOT EXISTS `biz_announcement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text COMMENT '公告内容',
  `status` int(11) DEFAULT '1' COMMENT '0下架 1发布',
  `start_time` datetime DEFAULT NULL COMMENT '展示开始时间(空=不限)',
  `end_time` datetime DEFAULT NULL COMMENT '展示结束时间(空=不限)',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公告';

-- 琛細avatar review(biz_avatar_review)
CREATE TABLE IF NOT EXISTS `biz_avatar_review` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `file_name` varchar(100) DEFAULT '',
  `image_url` varchar(300) DEFAULT '',
  `status` tinyint(4) DEFAULT '0' COMMENT '0 pending 1 approved 2 rejected',
  `remark` varchar(200) DEFAULT '',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='avatar review';

-- 文竹功能已移除(biz_bamboo/biz_bamboo_log)，改为 biz_user.star_count 小星星

-- 琛細daily checkin(biz_checkin_record)
CREATE TABLE IF NOT EXISTS `biz_checkin_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `checkin_date` date DEFAULT NULL,
  `coin` int(11) DEFAULT '0',
  `streak_days` int(11) DEFAULT '1',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='daily checkin';

-- 琛細user coin account(biz_coin)
CREATE TABLE IF NOT EXISTS `biz_coin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `balance` int(11) DEFAULT '0' COMMENT 'coin balance',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user coin account';

-- 琛細铜钱流水(biz_coin_log)
CREATE TABLE IF NOT EXISTS `biz_coin_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `change_amount` int(11) NOT NULL COMMENT '变动数量(正负)',
  `balance_after` int(11) NOT NULL COMMENT '变动后余额',
  `log_type` varchar(20) NOT NULL COMMENT 'recharge/shop/send_letter/admin_adjust',
  `ref_id` bigint(20) DEFAULT NULL COMMENT '关联业务ID',
  `remark` varchar(200) DEFAULT '',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='铜钱流水';

-- 琛細comment(biz_comment)
CREATE TABLE IF NOT EXISTS `biz_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `target_type` varchar(20) NOT NULL COMMENT 'thought/note',
  `target_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `content` varchar(500) DEFAULT '',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='comment';

-- 琛細应用配置表(biz_config)
CREATE TABLE IF NOT EXISTS `biz_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT '' COMMENT '配置值',
  `remark` varchar(200) DEFAULT '' COMMENT '备注',
  `app_id` bigint(20) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用配置表';

-- 琛細credit log(biz_credit_log)
CREATE TABLE IF NOT EXISTS `biz_credit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `change_val` int(11) NOT NULL,
  `balance` int(11) DEFAULT NULL,
  `reason` varchar(200) DEFAULT '',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='credit log';

-- 琛細日记(biz_diary)
CREATE TABLE IF NOT EXISTS `biz_diary` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `title` varchar(200) DEFAULT '',
  `content` text,
  `weather` varchar(20) DEFAULT '' COMMENT '天气',
  `mood` varchar(20) DEFAULT '' COMMENT '心情',
  `write_date` date DEFAULT NULL COMMENT '日记日期',
  `is_public` int(11) DEFAULT '0' COMMENT '0私密 1公开',
  `abnormal` int(11) DEFAULT '0' COMMENT '0正常 1异常(后台标记，异常不上广场)',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日记';

-- 琛細邮箱验证码发送记录(biz_email_send_log)
CREATE TABLE IF NOT EXISTS `biz_email_send_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(128) NOT NULL COMMENT '收件邮箱',
  `scene` varchar(32) DEFAULT '' COMMENT '用途场景(register注册/bind绑定/reset重置密码等)',
  `code` varchar(10) DEFAULT '' COMMENT '验证码',
  `status` int(11) DEFAULT '0' COMMENT '0失败 1发送成功',
  `fail_reason` varchar(255) DEFAULT '' COMMENT '失败原因(发送失败时)',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `expire_time` datetime DEFAULT NULL COMMENT '验证码过期时间',
  `client_ip` varchar(64) DEFAULT '' COMMENT '请求IP',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_email` (`email`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮箱验证码发送记录';

-- 琛細信封(biz_envelope)
CREATE TABLE IF NOT EXISTS `biz_envelope` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT '',
  `image_url` varchar(500) DEFAULT '',
  `price` int(11) NOT NULL DEFAULT '0',
  `stock` int(11) DEFAULT '0',
  `envelope_type` varchar(20) DEFAULT '普通',
  `status` int(11) DEFAULT '1',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信封';

-- 琛細好友关系(biz_friend)
CREATE TABLE IF NOT EXISTS `biz_friend` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '发起方ID',
  `friend_id` bigint(20) NOT NULL COMMENT '被加方ID',
  `status` int(11) DEFAULT '0' COMMENT '0申请中 1已通过 2已拒绝 3已删除 4已拉黑',
  `apply_remark` varchar(200) DEFAULT '' COMMENT '申请备注',
  `add_time` datetime DEFAULT NULL COMMENT '通过时间',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系';

-- 琛細interaction like/fav(biz_interaction)
CREATE TABLE IF NOT EXISTS `biz_interaction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `target_type` varchar(20) NOT NULL COMMENT 'thought/note',
  `target_id` bigint(20) NOT NULL,
  `action` varchar(10) NOT NULL COMMENT 'like/fav',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='interaction like/fav';

-- 琛細邀请记录表(biz_invite_log)
CREATE TABLE IF NOT EXISTS `biz_invite_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '邀请人ID',
  `invite_code` varchar(16) DEFAULT '' COMMENT '使用的邀请码',
  `invited_user_id` bigint(20) DEFAULT NULL COMMENT '被邀请人ID',
  `reward_coins` int(11) DEFAULT '0' COMMENT '奖励铜钱(文)',
  `app_id` bigint(20) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邀请记录表';

-- 琛細invite record(biz_invite_record)
CREATE TABLE IF NOT EXISTS `biz_invite_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `inviter_id` bigint(20) NOT NULL,
  `invitee_id` bigint(20) NOT NULL,
  `invite_code` varchar(16) DEFAULT '',
  `reward_coins` int(11) DEFAULT '0',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inviter` (`inviter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='invite record';

-- 琛細信件(biz_letter)
CREATE TABLE IF NOT EXISTS `biz_letter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sender_id` bigint(20) NOT NULL COMMENT '发信人ID',
  `receiver_id` bigint(20) DEFAULT NULL COMMENT '收信人ID(地址信件时为空)',
  `receiver_address` varchar(200) DEFAULT '' COMMENT '收件地址(寄给非好友时填写)',
  `title` varchar(200) DEFAULT '' COMMENT '标题(可不填)',
  `content` text,
  `status` int(11) DEFAULT '0' COMMENT '0草稿 1已发送 2已读 3已删除',
  `stamp_id` bigint(20) DEFAULT NULL COMMENT '使用的邮票ID',
  `envelope_id` bigint(20) DEFAULT NULL COMMENT '使用的信封ID',
  `is_public` int(11) DEFAULT '0' COMMENT '0私密 1公开(可在信件广场展示)',
  `abnormal` int(11) DEFAULT '0' COMMENT '0正常 1异常(后台标记，异常不上广场)',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `arrival_time` datetime DEFAULT NULL COMMENT '到达时间(寄出时间+邮票送达天数，未到时间收件人不可见)',
  `return_time` datetime DEFAULT NULL COMMENT '退回到达寄件人时间(退回操作+邮票送达天数，到点寄件人可见已退回)',
  `return_reason` varchar(50) DEFAULT NULL COMMENT '退回原因(未找到指定的用户/对方拒收/审核不合格)',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信件';

-- 琛細应用登录日志表(biz_login_log)
CREATE TABLE IF NOT EXISTS `biz_login_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL,
  `user_name` varchar(64) DEFAULT '',
  `login_name` varchar(64) DEFAULT '',
  `login_ip` varchar(64) DEFAULT '' COMMENT '登录IP',
  `login_location` varchar(128) DEFAULT '',
  `login_type` varchar(20) DEFAULT 'login' COMMENT 'login/logout',
  `code` int(11) DEFAULT NULL COMMENT '状态码 20000成功 50000失败',
  `message` varchar(200) DEFAULT '' COMMENT '消息',
  `login_time` datetime DEFAULT NULL COMMENT '登录时间',
  `app_id` bigint(20) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用登录日志表';

-- 琛細其他笔记(biz_note)
CREATE TABLE IF NOT EXISTS `biz_note` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `title` varchar(200) DEFAULT '',
  `content` text,
  `note_type` varchar(30) DEFAULT '其他' COMMENT '笔记分类',
  `is_public` int(11) DEFAULT '0',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='其他笔记';

-- 琛細order(biz_order)
CREATE TABLE IF NOT EXISTS `biz_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `item_type` varchar(20) NOT NULL COMMENT 'stamp/envelope',
  `item_id` bigint(20) DEFAULT NULL,
  `item_name` varchar(100) DEFAULT '',
  `quantity` int(11) DEFAULT '1',
  `unit_price` int(11) DEFAULT '0',
  `total_price` int(11) DEFAULT '0',
  `status` tinyint(4) DEFAULT '1' COMMENT '1 done 2 refund',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='order';

-- 琛細奖励记录表(biz_reward_log)
CREATE TABLE IF NOT EXISTS `biz_reward_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `reward_type` varchar(50) NOT NULL COMMENT '奖励类型(register/daily_login/checkin7/checkin30/profile/first_letter/first_diary/invite/stamp)',
  `reward_coins` int(11) DEFAULT '0' COMMENT '奖励铜钱(文)',
  `reward_date` date DEFAULT NULL COMMENT '奖励日期',
  `remark` varchar(200) DEFAULT '' COMMENT '备注',
  `app_id` bigint(20) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_type_date` (`user_id`,`reward_type`,`reward_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖励记录表';

-- 琛細邮票(biz_stamp)
CREATE TABLE IF NOT EXISTS `biz_stamp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT '',
  `image_url` varchar(500) DEFAULT '',
  `price` int(11) NOT NULL DEFAULT '0' COMMENT '铜钱单价',
  `stock` int(11) DEFAULT '0' COMMENT '库存, -1不限',
  `stamp_type` varchar(20) DEFAULT '普通' COMMENT '普通/稀有/限量',
  `theme` varchar(50) DEFAULT '经典' COMMENT '邮票主题(收藏分类)',
  `series` varchar(100) DEFAULT '',
  `delivery_days` int(11) DEFAULT '3' COMMENT '默认送达时间(天)',
  `status` int(11) DEFAULT '1' COMMENT '0下架 1上架',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邮票';

-- 琛細系统邮件(biz_sys_mail)
CREATE TABLE IF NOT EXISTS `biz_sys_mail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '邮件标题',
  `content` text COMMENT '邮件内容',
  `arrival_time` datetime DEFAULT NULL COMMENT '到达时间(到点后用户信箱可见)',
  `coin_amount` int(11) DEFAULT '0' COMMENT '附赠铜钱数(0为无)',
  `stamp_id` bigint(20) DEFAULT NULL COMMENT '附赠邮票ID',
  `envelope_id` bigint(20) DEFAULT NULL COMMENT '附赠信封ID',
  `sender_id` bigint(20) DEFAULT NULL COMMENT '发送管理员ID',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统邮件';

-- 琛細应用用户表(biz_user)
CREATE TABLE IF NOT EXISTS `biz_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT '' COMMENT '昵称(笔名)',
  `real_name` varchar(50) DEFAULT '' COMMENT '姓名',
  `email` varchar(50) DEFAULT '',
  `phone` varchar(20) DEFAULT '',
  `avatar` varchar(200) DEFAULT '',
  `invite_code` varchar(16) DEFAULT '' COMMENT '注册时填写的邀请码',
  `user_no` bigint(20) DEFAULT NULL COMMENT '用户唯一编号(6位,从101322起,邮寄信件凭编号)',
  `coins` int(11) DEFAULT '0' COMMENT '铜钱余额(文)',
  `credit_score` int(11) DEFAULT '100' COMMENT '信用分',
  `star_count` int(11) DEFAULT '0' COMMENT '小星星累计数(每寄出一封信+1)',
  `status` int(11) DEFAULT '1',
  `app_id` bigint(20) DEFAULT NULL COMMENT '所属应用ID',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_user_no` (`user_no`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用用户表';

-- 琛細user envelope item(biz_user_envelope_item)
CREATE TABLE IF NOT EXISTS `biz_user_envelope_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `envelope_id` bigint(20) NOT NULL,
  `code` varchar(32) DEFAULT NULL,
  `status` int(11) DEFAULT '1' COMMENT '1unused 2used',
  `used_time` datetime DEFAULT NULL,
  `letter_id` bigint(20) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_envelope_id` (`envelope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user envelope item';

-- 琛細用户系统邮件(biz_user_mail)
CREATE TABLE IF NOT EXISTS `biz_user_mail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `mail_id` bigint(20) NOT NULL COMMENT '系统邮件ID',
  `user_id` bigint(20) NOT NULL COMMENT '接收用户ID',
  `claimed` int(11) DEFAULT '0' COMMENT '0未领取 1已领取',
  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mail_user` (`mail_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户系统邮件';

-- 琛細兑换码配置(biz_redeem_code)
CREATE TABLE IF NOT EXISTS `biz_redeem_code` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL COMMENT '兑换码',
  `coin_amount` int(11) NOT NULL DEFAULT '0' COMMENT '兑换铜钱数',
  `max_count` int(11) NOT NULL DEFAULT '1' COMMENT '可兑换总次数',
  `used_count` int(11) NOT NULL DEFAULT '0' COMMENT '已兑换次数',
  `stamp_id` bigint(20) DEFAULT NULL COMMENT '赠送邮票ID(空=仅铜钱)',
  `status` int(11) DEFAULT '1' COMMENT '0停用 1启用',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='兑换码配置';

-- 琛細兑换记录(biz_redeem_log)
CREATE TABLE IF NOT EXISTS `biz_redeem_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code_id` bigint(20) NOT NULL COMMENT '兑换码ID',
  `code` varchar(32) DEFAULT '' COMMENT '兑换码(快照)',
  `user_id` bigint(20) NOT NULL COMMENT '兑换用户ID',
  `coin_amount` int(11) NOT NULL DEFAULT '0' COMMENT '兑换铜钱数',
  `stamp_id` bigint(20) DEFAULT NULL COMMENT '获赠邮票ID(快照,空=无)',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_user` (`code_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='兑换记录';

-- 琛細user profile(biz_user_profile)
CREATE TABLE IF NOT EXISTS `biz_user_profile` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'sys_user.id',
  `invite_code` varchar(16) DEFAULT '' COMMENT 'invitation code',
  `gender` tinyint(4) DEFAULT '0' COMMENT '0 secret 1 male 2 female',
  `region` varchar(100) DEFAULT '' COMMENT 'region',
  `region_secret` tinyint(4) DEFAULT '0' COMMENT 'region secret 0 public 1 secret',
  `age` int(11) DEFAULT NULL,
  `age_secret` tinyint(4) DEFAULT '0' COMMENT 'age secret 0 public 1 secret',
  `signature` varchar(200) DEFAULT '' COMMENT 'signature',
  `avatar_url` varchar(300) DEFAULT '' COMMENT 'approved avatar url',
  `avatar_status` tinyint(4) DEFAULT '0' COMMENT '0 none 1 pending 2 approved 3 rejected',
  `credit_score` int(11) DEFAULT '100',
  `reward_flags` int(11) DEFAULT '0' COMMENT 'one-time reward flags 1 register 2 profile',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT NULL,
  `sys006` varchar(50) DEFAULT NULL,
  `sys007` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user profile';

-- 琛細user stamp item(biz_user_stamp_item)
CREATE TABLE IF NOT EXISTS `biz_user_stamp_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `stamp_id` bigint(20) NOT NULL,
  `code` varchar(32) DEFAULT NULL,
  `status` int(11) DEFAULT '1' COMMENT '1unused 2used',
  `used_time` datetime DEFAULT NULL,
  `letter_id` bigint(20) DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint(20) DEFAULT NULL,
  `sys004` bigint(20) DEFAULT NULL,
  `sys005` int(11) DEFAULT '1',
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_stamp_id` (`stamp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user stamp item';

-- ================================================================
-- 触发器：注册 sys_user 时初始化铜钱账户（金额取 biz_config: reward.register）
-- ================================================================
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

-- ================================================================
-- 第三部分：初始数据（全部数据插入集中在文件最后）
-- ================================================================

-- 数据：部门（应用根部门，DataInitRunner.initApps 按名称"传信纸船"判重，勿改名）
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `sort`, `leader`, `status`, `sys001`, `sys005`) VALUES
(1, 0, '传信纸船', 0, '', 1, NOW(), 1);

-- 数据：系统配置(奖励金额等；置于 sys_user 之前，确保注册奖励触发器能读到配置)
INSERT IGNORE INTO `biz_config` (`config_key`, `config_value`, `remark`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
('reward.register', '200', '注册奖励(文)，一次性', NOW(), 1, 1, 'system'),
('reward.daily_login', '10', '每日登录奖励(文)，每日限1次', NOW(), 1, 1, 'system'),
('reward.streak_7', '50', '连续签到7天额外奖励(文)', NOW(), 1, 1, 'system'),
('reward.streak_30', '100', '连续签到30天额外奖励(文)', NOW(), 1, 1, 'system'),
('reward.profile_complete', '50', '完善资料奖励(文)，一次性', NOW(), 1, 1, 'system'),
('reward.first_letter_daily', '10', '当日第一封信奖励(文)', NOW(), 1, 1, 'system'),
('reward.first_diary_daily', '10', '当日写日记奖励(文)', NOW(), 1, 1, 'system'),
('reward.invite_register', '30', '邀请码注册奖励(文)，发放给邀请人', NOW(), 1, 1, 'system'),
('price.stamp_basic', '30', '最基础单张邮票价格(文)', NOW(), 1, 1, 'system');

-- 数据：用户(密码明文123456，登录时自动加密为BCrypt)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `dept_id`, `tenant_id`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, 'admin', '123456', '超级管理员', 'admin@aladdin.com', '13800138000', 1, 1, 1, NOW(), 1, 1, 'system'),
(2, 'user01', '123456', '普通用户', 'user01@aladdin.com', '13800138001', 1, 1, 1, NOW(), 1, 1, 'system');

-- 数据：角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `sort`, `data_scope`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '超级管理员', 'admin', 1, 1, 1, NOW(), 1, 1, 'system'),
(2, '普通用户', 'user', 2, 5, 1, NOW(), 1, 1, 'system');

-- 数据：资源(菜单M/按钮F)
INSERT INTO `sys_resource` (`id`, `resource_name`, `parent_id`, `sort`, `path`, `component`, `resource_type`, `perms`, `icon`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
-- 一级菜单与系统管理子菜单
(1, '系统管理', 0, 1, '/system', NULL, 'M', '', 'system', 1, NOW(), 1, 1, 'system'),
(2, '用户管理', 1, 1, '/system/user', 'system/user/index', 'M', 'system:user:list', 'user', 1, NOW(), 1, 1, 'system'),
(3, '角色管理', 1, 2, '/system/role', 'system/role/index', 'M', 'system:role:list', 'peoples', 1, NOW(), 1, 1, 'system'),
(4, '资源管理', 1, 3, '/system/resource', 'system/resource/index', 'M', 'system:resource:list', 'tree-table', 1, NOW(), 1, 1, 'system'),
(5, '部门管理', 1, 4, '/system/dept', 'system/dept/index', 'M', 'system:dept:list', 'tree', 1, NOW(), 1, 1, 'system'),
(6, '字典管理', 1, 5, '/system/dict', 'system/dict/index', 'M', 'system:dict:list', 'dict', 1, NOW(), 1, 1, 'system'),
-- 用户/角色/资源/部门/字典 按钮权限
(7, '用户新增', 2, 1, '', '', 'F', 'system:user:add', '', 1, NOW(), 1, 1, 'system'),
(8, '用户修改', 2, 2, '', '', 'F', 'system:user:edit', '', 1, NOW(), 1, 1, 'system'),
(9, '用户删除', 2, 3, '', '', 'F', 'system:user:remove', '', 1, NOW(), 1, 1, 'system'),
(10, '角色新增', 3, 1, '', '', 'F', 'system:role:add', '', 1, NOW(), 1, 1, 'system'),
(11, '角色修改', 3, 2, '', '', 'F', 'system:role:edit', '', 1, NOW(), 1, 1, 'system'),
(12, '角色删除', 3, 3, '', '', 'F', 'system:role:remove', '', 1, NOW(), 1, 1, 'system'),
(13, '资源新增', 4, 1, '', '', 'F', 'system:resource:add', '', 1, NOW(), 1, 1, 'system'),
(14, '资源修改', 4, 2, '', '', 'F', 'system:resource:edit', '', 1, NOW(), 1, 1, 'system'),
(15, '资源删除', 4, 3, '', '', 'F', 'system:resource:remove', '', 1, NOW(), 1, 1, 'system'),
(16, '部门新增', 5, 1, '', '', 'F', 'system:dept:add', '', 1, NOW(), 1, 1, 'system'),
(17, '部门修改', 5, 2, '', '', 'F', 'system:dept:edit', '', 1, NOW(), 1, 1, 'system'),
(18, '部门删除', 5, 3, '', '', 'F', 'system:dept:remove', '', 1, NOW(), 1, 1, 'system'),
(19, '字典新增', 6, 1, '', '', 'F', 'system:dict:add', '', 1, NOW(), 1, 1, 'system'),
(20, '字典修改', 6, 2, '', '', 'F', 'system:dict:edit', '', 1, NOW(), 1, 1, 'system'),
(21, '字典删除', 6, 3, '', '', 'F', 'system:dict:remove', '', 1, NOW(), 1, 1, 'system'),
-- 系统管理扩展子菜单
(22, '菜单管理', 1, 6, '/system/menu', 'system/menu/index', 'M', 'system:menu:list', 'menu', 1, NOW(), 1, 1, 'system'),
(23, '权限配置', 1, 7, '/system/permission', 'system/permission/index', 'M', 'system:permission:list', 'safety', 1, NOW(), 1, 1, 'system'),
(24, '用户角色管理', 1, 8, '/system/user-role', 'system/user-role/index', 'M', 'system:userrole:list', 'team', 1, NOW(), 1, 1, 'system'),
(25, '角色组织管理', 1, 9, '/system/role-dept', 'system/role-dept/index', 'M', 'system:roledept:list', 'cluster', 1, NOW(), 1, 1, 'system'),
(26, '角色资源管理', 1, 10, '/system/role-resource', 'system/role-resource/index', 'M', 'system:roleresource:list', 'apartment', 1, NOW(), 1, 1, 'system'),
(27, '操作日志', 1, 11, '/system/oper-log', 'system/oper-log/index', 'M', 'system:operlog:list', 'file-text', 1, NOW(), 1, 1, 'system'),
(28, '登录日志', 1, 12, '/system/login-log', 'system/login-log/index', 'M', 'system:loginlog:list', 'login', 1, NOW(), 1, 1, 'system'),
-- 菜单管理按钮
(29, '菜单新增', 22, 1, '', '', 'F', 'system:menu:add', '', 1, NOW(), 1, 1, 'system'),
(30, '菜单修改', 22, 2, '', '', 'F', 'system:menu:edit', '', 1, NOW(), 1, 1, 'system'),
(31, '菜单删除', 22, 3, '', '', 'F', 'system:menu:remove', '', 1, NOW(), 1, 1, 'system'),
-- 仪表盘(Dashboard)菜单
(32, '仪表盘', 0, 0, '/dashboard', NULL, 'M', '', 'lucide:layout-dashboard', 1, NOW(), 1, 1, 'system'),
(33, '分析页', 32, 1, '/dashboard/analytics', 'dashboard/analytics/index', 'M', '', 'lucide:area-chart', 1, NOW(), 1, 1, 'system'),
(34, '工作台', 32, 2, '/dashboard/workspace', 'dashboard/workspace/index', 'M', '', 'carbon:workspace', 1, NOW(), 1, 1, 'system'),
-- 应用管理菜单及按钮
(35, '应用管理', 1, 13, '/system/app', 'system/app/index', 'M', 'system:app:list', 'ant-design:appstore-outlined', 1, NOW(), 1, 1, 'system'),
(36, '应用新增', 35, 1, '', '', 'F', 'system:app:add', '', 1, NOW(), 1, 1, 'system'),
(37, '应用修改', 35, 2, '', '', 'F', 'system:app:edit', '', 1, NOW(), 1, 1, 'system'),
(38, '应用删除', 35, 3, '', '', 'F', 'system:app:remove', '', 1, NOW(), 1, 1, 'system'),
-- 传信纸船应用菜单
(39, '传信纸船', 0, 9, '/app/paper_boat', NULL, 'M', '', 'ant-design:appstore-outlined', 1, NOW(), 1, 1, 'system'),
(40, '应用用户', 39, 1, '/app/paper_boat/user', 'app/paper-boat/user/index', 'M', 'app:paperBoat:list', 'lucide:users', 1, NOW(), 1, 1, 'system'),
(41, '信件管理', 39, 2, '/app/paper_boat/letter', 'app/paper-boat/letter/index', 'M', 'app:paperBoat:list', 'lucide:mail', 1, NOW(), 1, 1, 'system'),
(42, '铜钱流水', 39, 3, '/app/paper_boat/coin', 'app/paper-boat/coin/index', 'M', 'app:paperBoat:list', 'lucide:coins', 1, NOW(), 1, 1, 'system'),
(43, '奖励记录', 39, 4, '/app/paper_boat/reward', 'app/paper-boat/reward/index', 'M', 'app:paperBoat:list', 'lucide:gift', 1, NOW(), 1, 1, 'system'),
(44, '邀请记录', 39, 5, '/app/paper_boat/invite', 'app/paper-boat/invite/index', 'M', 'app:paperBoat:list', 'lucide:user-plus', 1, NOW(), 1, 1, 'system'),
(46, '应用配置', 39, 7, '/app/paper_boat/config', 'app/paper-boat/config/index', 'M', 'app:paperBoat:list', 'lucide:settings', 1, NOW(), 1, 1, 'system'),
(47, '头像审核', 39, 8, '/app/paper_boat/avatar', 'biz/avatar/index', 'M', 'app:paperBoat:list', 'lucide:image', 1, NOW(), 1, 1, 'system'),
(48, '邮票商城', 39, 9, '/app/paper_boat/stamp', 'biz/shop/index', 'M', 'app:paperBoat:list', 'lucide:stamp', 1, NOW(), 1, 1, 'system'),
(49, '邮票列表', 39, 10, '/app/paper_boat/stamp_items', 'biz/stamp_items/index', 'M', 'app:paperBoat:list', 'lucide:list', 1, NOW(), 1, 1, 'system'),
(50, '订单管理', 39, 11, '/app/paper_boat/order', 'biz/order/index', 'M', 'app:paperBoat:list', 'lucide:shopping-cart', 1, NOW(), 1, 1, 'system'),
(51, '公告配置', 39, 12, '/app/paper_boat/announcement', 'biz/announcement/index', 'M', 'app:paperBoat:list', 'lucide:megaphone', 1, NOW(), 1, 1, 'system'),
(52, '系统邮件', 39, 13, '/app/paper_boat/sysmail', 'biz/sysmail/index', 'M', 'app:paperBoat:list', 'lucide:mail-plus', 1, NOW(), 1, 1, 'system'),
(53, '兑换码', 39, 14, '/app/paper_boat/redeem', 'biz/redeem/index', 'M', 'app:paperBoat:list', 'lucide:ticket', 1, NOW(), 1, 1, 'system');

-- 数据：用户-角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2);

-- 数据：角色-资源关联(admin角色拥有全部资源)
INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15),
(1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21),
(1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27), (1, 28),
(1, 29), (1, 30), (1, 31), (1, 32), (1, 33), (1, 34),
(1, 35), (1, 36), (1, 37), (1, 38),
(1, 39), (1, 40), (1, 41), (1, 42), (1, 43), (1, 44), (1, 46),
(1, 47), (1, 48), (1, 49), (1, 50), (1, 51), (1, 52), (1, 53),
(2, 1), (2, 2), (2, 3), (2, 5), (2, 6), (2, 32), (2, 33), (2, 34);

-- 数据：字典类型(与 DataInitRunner.initDictData 播种的4类对齐)
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '性别', 'sys_user_sex', 1, NOW(), 1, 1, 'system'),
(2, '状态', 'sys_normal_disable', 1, NOW(), 1, 1, 'system'),
(3, '资源类型', 'sys_resource_type', 1, NOW(), 1, 1, 'system'),
(4, '数据权限', 'sys_data_scope', 1, NOW(), 1, 1, 'system');

-- 数据：字典数据
INSERT INTO `sys_dict_data` (`dict_type_id`, `dict_label`, `dict_value`, `sort`, `list_class`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '男', '0', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(1, '女', '1', 2, 'danger', 1, NOW(), 1, 1, 'system'),
(1, '未知', '2', 3, 'info', 1, NOW(), 1, 1, 'system'),
(2, '正常', '1', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(2, '停用', '0', 2, 'danger', 1, NOW(), 1, 1, 'system'),
(3, '菜单', 'M', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(3, '接口', 'A', 2, 'success', 1, NOW(), 1, 1, 'system'),
(3, '按钮', 'F', 3, 'warning', 1, NOW(), 1, 1, 'system'),
(4, '全部数据', '1', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(4, '自定义数据', '2', 2, 'success', 1, NOW(), 1, 1, 'system'),
(4, '本部门数据', '3', 3, 'warning', 1, NOW(), 1, 1, 'system'),
(4, '本部门及以下', '4', 4, 'info', 1, NOW(), 1, 1, 'system'),
(4, '仅本人数据', '5', 5, 'danger', 1, NOW(), 1, 1, 'system');

-- 数据：系统公告（sys_notice 表已废弃，公告改用 biz_announcement）
INSERT INTO `biz_announcement` (`id`, `title`, `content`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '系统上线通知', '系统已正式上线，欢迎使用！', 1, NOW(), 1, 1, 'system');

-- 数据：默认菜单(资源表有数据时不启用，仅作资源为空时的兜底)
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `title`, `icon`, `path`, `component`, `sort`, `affix_tab`, `keep_alive`, `badge_type`, `link`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, 0, 'Dashboard1', 'page.dashboard.title', 'lucide:layout-dashboard', '/dashboard', 'BasicLayout', -1, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(2, 1, 'Analytics', 'page.dashboard.analytics', 'lucide:area-chart', '/analytics', '/dashboard/analytics/index', 0, 1, 0, '', '', 1, NOW(), 1, 1, 'system'),
(3, 1, 'Workspace', 'page.dashboard.workspace', 'carbon:workspace', '/workspace', '/dashboard/workspace/index', 0, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(4, 0, 'Demos', 'demos.title', 'ic:baseline-view-in-ar', '/demos', 'BasicLayout', 1000, 0, 1, '', '', 1, NOW(), 1, 1, 'system'),
(5, 4, 'AntDesignDemos', 'demos.antd', '', '/demos/ant-design', '/demos/antd/index', 0, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(6, 0, 'VbenProject', 'demos.vben.title', 'https://cdn.jsdelivr.net/gh/vbenjs/static@0.1.0/source/vben-logo.svg', '/vben-admin', 'BasicLayout', 9998, 0, 0, 'dot', '', 1, NOW(), 1, 1, 'system'),
(7, 6, 'VbenDocument', 'demos.vben.document', 'lucide:book-open-text', '/vben-admin/document', 'IFrameView', 0, 0, 0, '', 'https://doc.vben.pro', 1, NOW(), 1, 1, 'system'),
(8, 6, 'VbenGithub', 'Github', 'mdi:github', '/vben-admin/github', 'IFrameView', 0, 0, 0, '', 'https://github.com/vbenjs/vue-vben-admin', 1, NOW(), 1, 1, 'system'),
(9, 0, 'VbenAbout', 'demos.vben.about', 'lucide:copyright', '/vben-admin/about', '/_core/about/index', 9999, 0, 0, '', '', 1, NOW(), 1, 1, 'system');

-- 数据：应用(传信纸船，根部门与菜单根节点见 sys_resource id=39)
INSERT INTO `sys_app` (`id`, `app_name`, `app_code`, `description`, `allow_register`, `dept_id`, `menu_id`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '传信纸船', 'paper_boat', '阿拉丁传信纸船应用', 1, 1, 39, 1, NOW(), 1, 1, 'system');

-- 数据：默认邮票
INSERT INTO `biz_stamp` (`name`, `description`, `image_url`, `price`, `stock`, `stamp_type`, `theme`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
('普通梅花邮票', '最常见的梅花图案邮票', '', 30, 100, '普通', '花草', 1, NOW(), 1, 1, 'system'),
('青花瓷邮票', '精美青花瓷图案', '', 50, 50, '稀有', '文化', 1, NOW(), 1, 1, 'system'),
('限量纪念邮票', '十周年纪念限量版', '', 200, 10, '限量', '纪念', 1, NOW(), 1, 1, 'system');

-- 数据：默认信封
INSERT INTO `biz_envelope` (`name`, `description`, `image_url`, `price`, `stock`, `envelope_type`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
('普通信封', '牛皮纸普通信封', '', 5, 200, '普通', 1, NOW(), 1, 1, 'system'),
('精美印花信封', '印花装饰信封', '', 20, 100, '精美', 1, NOW(), 1, 1, 'system');
