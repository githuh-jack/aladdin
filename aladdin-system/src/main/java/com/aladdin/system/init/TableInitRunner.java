package com.aladdin.system.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表结构初始化
 * 启动时检查表是否存在，不存在则自动创建
 *
 * @author cles
 * @date 2026/06/11
 */
@Component
@Order(value = 1)
public class TableInitRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TableInitRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public TableInitRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final Map<String, String> TABLE_DDL = new LinkedHashMap<>();

    static {
        TABLE_DDL.put("sys_dept", """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_user", """
            CREATE TABLE IF NOT EXISTS `sys_user` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `username` varchar(50) NOT NULL,
              `password` varchar(200) NOT NULL,
              `nickname` varchar(50) DEFAULT '',
              `real_name` varchar(50) DEFAULT '' COMMENT '姓名',
              `email` varchar(50) DEFAULT '',
              `phone` varchar(20) DEFAULT '',
              `avatar` varchar(200) DEFAULT '',
              `invite_code` varchar(16) DEFAULT '' COMMENT '注册时填写的邀请码',
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_role", """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_resource", """
            CREATE TABLE IF NOT EXISTS `sys_resource` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `resource_name` varchar(50) NOT NULL,
              `parent_id` bigint DEFAULT 0,
              `sort` int DEFAULT 0,
              `path` varchar(200) DEFAULT '',
              `component` varchar(200) DEFAULT '',
              `resource_type` varchar(1) DEFAULT '',
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_user_role", """
            CREATE TABLE IF NOT EXISTS `sys_user_role` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `user_id` bigint NOT NULL,
              `role_id` bigint NOT NULL,
              PRIMARY KEY (`id`),
              KEY `idx_user_id` (`user_id`),
              KEY `idx_role_id` (`role_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_role_resource", """
            CREATE TABLE IF NOT EXISTS `sys_role_resource` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `role_id` bigint NOT NULL,
              `resource_id` bigint NOT NULL,
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_role_resource` (`role_id`, `resource_id`),
              KEY `idx_resource_id` (`resource_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_role_dept", """
            CREATE TABLE IF NOT EXISTS `sys_role_dept` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `role_id` bigint NOT NULL,
              `dept_id` bigint NOT NULL,
              PRIMARY KEY (`id`),
              KEY `idx_role_id` (`role_id`),
              KEY `idx_dept_id` (`dept_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_dict_type", """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_dict_data", """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_login_log", """
            CREATE TABLE IF NOT EXISTS `sys_login_log` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `trace_id` varchar(64) DEFAULT '',
              `user_id` bigint DEFAULT NULL,
              `user_name` varchar(50) DEFAULT '',
              `login_name` varchar(50) DEFAULT '',
              `login_ip` varchar(128) DEFAULT '',
              `login_location` varchar(255) DEFAULT '',
              `login_type` varchar(2) DEFAULT '',
              `token` varchar(500) DEFAULT '',
              `session_id` varchar(64) DEFAULT '',
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_operation_log", """
            CREATE TABLE IF NOT EXISTS `sys_operation_log` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `trace_id` varchar(64) DEFAULT '',
              `user_id` bigint DEFAULT NULL,
              `user_name` varchar(50) DEFAULT '',
              `operation` varchar(50) DEFAULT '',
              `method` varchar(200) DEFAULT '',
              `request_url` varchar(255) DEFAULT '',
              `request_method` varchar(10) DEFAULT '',
              `request_param` text,
              `response_result` text,
              `ip` varchar(128) DEFAULT '',
              `cost` bigint DEFAULT NULL,
              `status` int DEFAULT 1,
              `error_msg` text,
              `operation_time` datetime DEFAULT NULL,
              `sys001` datetime DEFAULT NULL,
              `sys002` datetime DEFAULT NULL,
              `sys003` bigint DEFAULT NULL,
              `sys004` bigint DEFAULT NULL,
              `sys005` int DEFAULT 1,
              `sys006` varchar(64) DEFAULT '',
              `sys007` varchar(64) DEFAULT '',
              PRIMARY KEY (`id`),
              KEY `idx_user_id` (`user_id`),
              KEY `idx_operation_time` (`operation_time`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        TABLE_DDL.put("sys_menu", """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统默认菜单表'
            """);

        TABLE_DDL.put("sys_app", """
            CREATE TABLE IF NOT EXISTS `sys_app` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `app_name` varchar(100) NOT NULL COMMENT '应用名称',
              `app_code` varchar(50) NOT NULL COMMENT '应用编码',
              `description` varchar(500) DEFAULT '' COMMENT '应用描述',
              `allow_register` int DEFAULT 1 COMMENT '是否准许注册 0否 1是',
              `dept_id` bigint DEFAULT NULL COMMENT '关联根部门ID',
              `menu_id` bigint DEFAULT NULL COMMENT '关联菜单根节点ID',
              `init_sql` longtext COMMENT '应用相关表初始化DDL语句(分号分隔)',
              `tables_initialized` int DEFAULT 0 COMMENT '相关表是否已初始化 0否 1是',
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
              UNIQUE KEY `uk_app_code` (`app_code`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用表'
            """);

        TABLE_DDL.put("sys_user_dept", """
            CREATE TABLE IF NOT EXISTS `sys_user_dept` (
              `id` bigint NOT NULL AUTO_INCREMENT,
              `user_id` bigint NOT NULL,
              `dept_id` bigint NOT NULL,
              `is_primary` int DEFAULT 0 COMMENT '是否主部门 0否 1是',
              `sys001` datetime DEFAULT NULL,
              `sys002` datetime DEFAULT NULL,
              `sys003` bigint DEFAULT NULL,
              `sys004` bigint DEFAULT NULL,
              `sys005` int DEFAULT 1,
              `sys006` varchar(64) DEFAULT '',
              `sys007` varchar(64) DEFAULT '',
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_user_dept` (`user_id`, `dept_id`),
              KEY `idx_dept_id` (`dept_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户部门关联表'
            """);
    }

    @Override
    public void run(ApplicationArguments args) {
        initTables();
    }

    private void initTables() {
        int created = 0;
        for (Map.Entry<String, String> entry : TABLE_DDL.entrySet()) {
            String tableName = entry.getKey();
            String ddl = entry.getValue();
            if (!tableExists(tableName)) {
                try {
                    jdbcTemplate.execute(ddl);
                    created++;
                    log.info("创建表: {}", tableName);
                } catch (Exception e) {
                    log.warn("创建表失败: {} - {}", tableName, e.getMessage());
                }
            }
        }
        if (created > 0) {
            log.info("表结构初始化完成，新建 {} 张表", created);
        } else {
            log.info("所有表已存在，跳过建表");
        }
    }

    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                    Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
