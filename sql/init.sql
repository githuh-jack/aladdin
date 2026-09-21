-- ================================================================
-- Aladdin 系统初始化 SQL
-- 说明：包含全部建表与初始数据，幂等可重复执行
-- ================================================================

-- ==============================
-- 表：部门
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT 0,
  `dept_name` varchar(30) DEFAULT '',
  `sort` int DEFAULT 0,
  `leader` varchar(20) DEFAULT '',
  `phone` varchar(20) DEFAULT '',
  `email` varchar(50) DEFAULT '',
  `status` int DEFAULT 1,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：用户
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(200) NOT NULL,
  `nickname` varchar(50) DEFAULT '',
  `email` varchar(50) DEFAULT '',
  `phone` varchar(20) DEFAULT '',
  `avatar` varchar(200) DEFAULT '',
  `dept_id` bigint DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `status` int DEFAULT 1,
  `pwd_change_time` datetime DEFAULT NULL COMMENT '密码最后修改时间',
  `pwd_force_change` int DEFAULT 0 COMMENT '是否首次登录需修改密码 0否1是',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：角色
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `role_key` varchar(50) NOT NULL,
  `sort` int DEFAULT 0,
  `data_scope` int DEFAULT 1 COMMENT '数据权限范围 1全部 2自定义 3本部门 4本部门及以下 5仅本人',
  `status` int DEFAULT 1,
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：资源(菜单/接口/按钮)
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resource_name` varchar(50) NOT NULL,
  `parent_id` bigint DEFAULT 0,
  `sort` int DEFAULT 0,
  `path` varchar(200) DEFAULT '',
  `component` varchar(200) DEFAULT '',
  `resource_type` varchar(1) DEFAULT '' COMMENT 'M菜单 A接口 F按钮',
  `perms` varchar(100) DEFAULT '',
  `icon` varchar(100) DEFAULT '',
  `status` int DEFAULT 1,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：用户-角色关联
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：角色-资源关联
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_role_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `resource_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：角色-部门关联
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：字典类型
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_name` varchar(100) DEFAULT '',
  `dict_type` varchar(100) DEFAULT '',
  `status` int DEFAULT 1,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：字典数据
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type_id` bigint DEFAULT NULL,
  `dict_label` varchar(100) DEFAULT '',
  `dict_value` varchar(100) DEFAULT '',
  `sort` int DEFAULT 0,
  `css_class` varchar(100) DEFAULT '',
  `list_class` varchar(100) DEFAULT '',
  `status` int DEFAULT 1,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：密码历史记录
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_password_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `password` varchar(200) NOT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：IP黑白名单
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_ip_list` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ip_address` varchar(50) NOT NULL,
  `list_type` int NOT NULL COMMENT '1黑名单 2白名单',
  `remark` varchar(200) DEFAULT '',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：文件上传记录
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `original_name` varchar(200) DEFAULT '',
  `file_name` varchar(200) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_size` bigint DEFAULT 0,
  `file_type` varchar(50) DEFAULT '',
  `storage_type` varchar(20) DEFAULT 'local' COMMENT 'local/minio',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：Open-API应用
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_open_api_app` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_name` varchar(100) NOT NULL,
  `access_key` varchar(64) NOT NULL,
  `secret_key` varchar(128) NOT NULL,
  `bind_user_id` bigint DEFAULT NULL,
  `status` int DEFAULT 1,
  `tenant_id` bigint DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_access_key` (`access_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：站内信
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text,
  `msg_type` int NOT NULL COMMENT '1系统通知 2私信 3待办 4提醒',
  `send_type` int NOT NULL COMMENT '1单发 2群发',
  `sender_id` bigint DEFAULT 0 COMMENT '发送者ID，0表示系统',
  `tenant_id` bigint DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：站内信接收记录
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_message_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `read_status` int DEFAULT 0 COMMENT '0未读 1已读',
  `read_time` datetime DEFAULT NULL,
  `handle_status` int DEFAULT 0 COMMENT '0未处理 1已处理 2已忽略',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：用户组
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_user_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_name` varchar(100) NOT NULL,
  `group_key` varchar(100) NOT NULL,
  `sort` int DEFAULT 0,
  `status` int DEFAULT 1,
  `tenant_id` bigint DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_key` (`group_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：用户组-角色关联
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_user_group_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：用户组-用户关联
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_user_group_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：系统通知公告
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text,
  `notice_type` int NOT NULL COMMENT '1通知 2公告',
  `status` int DEFAULT 0 COMMENT '0草稿 1已发布 2已撤回',
  `publish_time` datetime DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：租户
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_name` varchar(100) NOT NULL,
  `tenant_code` varchar(50) NOT NULL,
  `contact_name` varchar(50) DEFAULT '',
  `contact_phone` varchar(20) DEFAULT '',
  `domain` varchar(200) DEFAULT '',
  `expire_time` datetime DEFAULT NULL,
  `status` int DEFAULT 1,
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================
-- 表：系统默认菜单
-- 说明：默认菜单不受角色权限影响，存储前端路由所需的菜单数据
-- ==============================
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT 0 COMMENT '父菜单ID',
  `menu_name` varchar(100) NOT NULL COMMENT '路由name(唯一)',
  `title` varchar(100) DEFAULT '' COMMENT '菜单标题(i18n key)',
  `icon` varchar(255) DEFAULT '' COMMENT '图标名或URL',
  `path` varchar(200) DEFAULT '' COMMENT '路由路径',
  `component` varchar(200) DEFAULT '' COMMENT '组件路径或布局名(BasicLayout/IFrameView/具体路径)',
  `sort` int DEFAULT 0 COMMENT '排序',
  `affix_tab` int DEFAULT 0 COMMENT '是否固定tab 0否1是',
  `keep_alive` int DEFAULT 0 COMMENT '是否缓存 0否1是',
  `badge_type` varchar(20) DEFAULT '' COMMENT '徽标类型(dot等)',
  `link` varchar(500) DEFAULT '' COMMENT '外链URL',
  `status` int DEFAULT 1 COMMENT '状态 0停用 1启用',
  `sys001` datetime DEFAULT NULL,
  `sys002` datetime DEFAULT NULL,
  `sys003` bigint DEFAULT NULL,
  `sys004` bigint DEFAULT NULL,
  `sys005` int DEFAULT 1,
  `sys006` varchar(64) DEFAULT '',
  `sys007` varchar(64) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统默认菜单表';

-- ================================================================
-- 初始数据
-- ================================================================

-- ==============================
-- 数据：部门
-- ==============================
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `sort`, `leader`, `status`, `sys001`, `sys005`) VALUES
(1, 0, '总公司', 0, 'admin', 1, NOW(), 1),
(2, 1, '研发部门', 1, 'admin', 1, NOW(), 1),
(3, 1, '市场部门', 2, 'admin', 1, NOW(), 1);

-- ==============================
-- 数据：租户
-- ==============================
INSERT INTO `sys_tenant` (`id`, `tenant_name`, `tenant_code`, `contact_name`, `contact_phone`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '默认租户', 'default', '管理员', '13800138000', 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：用户
-- ==============================
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `dept_id`, `tenant_id`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, 'admin', '123456', '超级管理员', 'admin@aladdin.com', '13800138000', 1, 1, 1, NOW(), 1, 1, 'system'),
(2, 'user01', '123456', '普通用户', 'user01@aladdin.com', '13800138001', 2, 1, 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：角色
-- ==============================
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `sort`, `data_scope`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '超级管理员', 'admin', 1, 1, 1, NOW(), 1, 1, 'system'),
(2, '普通用户', 'user', 2, 5, 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：资源(菜单M/按钮F)
-- ==============================
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
(31, '菜单删除', 22, 3, '', '', 'F', 'system:menu:remove', '', 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：用户-角色关联
-- ==============================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2);

-- ==============================
-- 数据：角色-资源关联(admin角色拥有全部资源)
-- ==============================
INSERT INTO `sys_role_resource` (`role_id`, `resource_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15),
(1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21),
(1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27), (1, 28),
(1, 29), (1, 30), (1, 31),
(2, 1), (2, 2), (2, 3), (2, 5), (2, 6);

-- ==============================
-- 数据：字典类型
-- ==============================
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '性别', 'sys_user_sex', 1, NOW(), 1, 1, 'system'),
(2, '状态', 'sys_normal_disable', 1, NOW(), 1, 1, 'system'),
(3, '资源类型', 'sys_resource_type', 1, NOW(), 1, 1, 'system'),
(4, '数据权限', 'sys_data_scope', 1, NOW(), 1, 1, 'system'),
(5, '操作状态', 'sys_oper_status', 1, NOW(), 1, 1, 'system'),
(6, '登录类型', 'sys_login_type', 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：字典数据
-- ==============================
INSERT INTO `sys_dict_data` (`dict_type_id`, `dict_label`, `dict_value`, `sort`, `list_class`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
-- 性别
(1, '男', '0', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(1, '女', '1', 2, 'danger', 1, NOW(), 1, 1, 'system'),
(1, '未知', '2', 3, 'info', 1, NOW(), 1, 1, 'system'),
-- 状态
(2, '正常', '1', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(2, '停用', '0', 2, 'danger', 1, NOW(), 1, 1, 'system'),
-- 资源类型
(3, '菜单', 'M', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(3, '接口', 'A', 2, 'success', 1, NOW(), 1, 1, 'system'),
(3, '按钮', 'F', 3, 'warning', 1, NOW(), 1, 1, 'system'),
-- 数据权限
(4, '全部数据', '1', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(4, '自定义数据', '2', 2, 'success', 1, NOW(), 1, 1, 'system'),
(4, '本部门数据', '3', 3, 'warning', 1, NOW(), 1, 1, 'system'),
(4, '本部门及以下', '4', 4, 'info', 1, NOW(), 1, 1, 'system'),
(4, '仅本人数据', '5', 5, 'danger', 1, NOW(), 1, 1, 'system'),
-- 操作状态
(5, '成功', '20000', 1, 'success', 1, NOW(), 1, 1, 'system'),
(5, '失败', '50000', 2, 'danger', 1, NOW(), 1, 1, 'system'),
-- 登录类型
(6, '登录', 'login', 1, 'primary', 1, NOW(), 1, 1, 'system'),
(6, '登出', 'logout', 2, 'warning', 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：Open-API应用
-- ==============================
INSERT INTO `sys_open_api_app` (`id`, `app_name`, `access_key`, `secret_key`, `bind_user_id`, `status`, `tenant_id`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '默认应用', 'ak_default_20260612', 'sk_default_secret_key_20260612', 1, 1, 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：用户组
-- ==============================
INSERT INTO `sys_user_group` (`id`, `group_name`, `group_key`, `sort`, `status`, `tenant_id`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '管理员组', 'admin_group', 1, 1, 1, NOW(), 1, 1, 'system'),
(2, '普通用户组', 'user_group', 2, 1, 1, NOW(), 1, 1, 'system');

INSERT INTO `sys_user_group_role` (`group_id`, `role_id`) VALUES
(1, 1),
(2, 2);

INSERT INTO `sys_user_group_user` (`group_id`, `user_id`) VALUES
(1, 1),
(2, 2);

-- ==============================
-- 数据：系统公告
-- ==============================
INSERT INTO `sys_notice` (`id`, `title`, `content`, `notice_type`, `status`, `publish_time`, `tenant_id`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
(1, '系统上线通知', '系统已正式上线，欢迎使用！', 2, 1, NOW(), 1, NOW(), 1, 1, 'system');

-- ==============================
-- 数据：默认菜单
-- 对齐前端 web-antd 的路由视图，保证登录后能正常展示菜单
-- ==============================
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `title`, `icon`, `path`, `component`, `sort`, `affix_tab`, `keep_alive`, `badge_type`, `link`, `status`, `sys001`, `sys003`, `sys005`, `sys006`) VALUES
-- Dashboard 仪表盘
(1, 0, 'Dashboard1', 'page.dashboard.title', 'lucide:layout-dashboard', '/dashboard', 'BasicLayout', -1, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(2, 1, 'Analytics', 'page.dashboard.analytics', 'lucide:area-chart', '/analytics', '/dashboard/analytics/index', 0, 1, 0, '', '', 1, NOW(), 1, 1, 'system'),
(3, 1, 'Workspace', 'page.dashboard.workspace', 'carbon:workspace', '/workspace', '/dashboard/workspace/index', 0, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
-- Demos 演示
(4, 0, 'Demos', 'demos.title', 'ic:baseline-view-in-ar', '/demos', 'BasicLayout', 1000, 0, 1, '', '', 1, NOW(), 1, 1, 'system'),
(5, 4, 'AntDesignDemos', 'demos.antd', '', '/demos/ant-design', '/demos/antd/index', 0, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
-- VbenProject 项目链接
(6, 0, 'VbenProject', 'demos.vben.title', 'https://cdn.jsdelivr.net/gh/vbenjs/static@0.1.0/source/vben-logo.svg', '/vben-admin', 'BasicLayout', 9998, 0, 0, 'dot', '', 1, NOW(), 1, 1, 'system'),
(7, 6, 'VbenDocument', 'demos.vben.document', 'lucide:book-open-text', '/vben-admin/document', 'IFrameView', 0, 0, 0, '', 'https://doc.vben.pro', 1, NOW(), 1, 1, 'system'),
(8, 6, 'VbenGithub', 'Github', 'mdi:github', '/vben-admin/github', 'IFrameView', 0, 0, 0, '', 'https://github.com/vbenjs/vue-vben-admin', 1, NOW(), 1, 1, 'system'),
-- About 关于
(9, 0, 'VbenAbout', 'demos.vben.about', 'lucide:copyright', '/vben-admin/about', '/_core/about/index', 9999, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
-- 系统管理(一级菜单)
(100, 0, 'System', 'page.system.title', 'lucide:settings', '/system', 'BasicLayout', 100, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(101, 100, 'SystemDept', 'page.system.dept', 'lucide:network', '/system/dept', '/system/dept/index', 1, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(102, 100, 'SystemDict', 'page.system.dict', 'lucide:book', '/system/dict', '/system/dict/index', 2, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(103, 100, 'SystemMenu', 'page.system.menu', 'lucide:menu', '/system/menu', '/system/menu/index', 3, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(104, 100, 'SystemResource', 'page.system.resource', 'lucide:layout-grid', '/system/resource', '/system/resource/index', 4, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(105, 100, 'SystemRole', 'page.system.role', 'lucide:users', '/system/role', '/system/role/index', 5, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(106, 100, 'SystemPermission', 'page.system.permission', 'lucide:shield', '/system/permission', '/system/permission/index', 6, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(107, 100, 'SystemUser', 'page.system.user', 'lucide:user', '/system/user', '/system/user/index', 7, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(108, 100, 'SystemUserRole', 'page.system.userRole', 'lucide:user-cog', '/system/user-role', '/system/user-role/index', 8, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(109, 100, 'SystemRoleDept', 'page.system.roleDept', 'lucide:git-branch', '/system/role-dept', '/system/role-dept/index', 9, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(110, 100, 'SystemRoleResource', 'page.system.roleResource', 'lucide:boxes', '/system/role-resource', '/system/role-resource/index', 10, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(111, 100, 'SystemOperLog', 'page.system.operLog', 'lucide:file-clock', '/system/oper-log', '/system/oper-log/index', 11, 0, 0, '', '', 1, NOW(), 1, 1, 'system'),
(112, 100, 'SystemLoginLog', 'page.system.loginLog', 'lucide:log-in', '/system/login-log', '/system/login-log/index', 12, 0, 0, '', '', 1, NOW(), 1, 1, 'system');
