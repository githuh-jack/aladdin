package com.aladdin.system.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据初始化
 *
 * @author cles
 * @date 2026/06/11
 */
@Component
@Order(value = 2)
public class DataInitRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DataInitRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            resetDevPassword();
            initRole();
            initResources();
            initUser();
            initRoleResource();
            initUserRole();
            initDictData();
            initMenus();
            log.info("数据初始化完成");
        } catch (Exception e) {
            log.warn("数据初始化异常: {}", e.getMessage());
        }
    }

    /**
     * 初始化默认菜单数据
     * 默认菜单存储在sys_menu表，不受角色权限影响，对应前端路由视图
     */
    private void initMenus() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO sys_menu (id, parent_id, menu_name, title, icon, path, component, sort, affix_tab, keep_alive, badge_type, link, status, sys001, sys003, sys005, sys006) VALUES " +
                // 1. Dashboard 仪表盘
                "(1, 0, 'Dashboard1', 'page.dashboard.title', 'lucide:layout-dashboard', '/dashboard', 'BasicLayout', -1, 0, 0, '', '', 1, NOW(), 1, 1, 'system'), " +
                "(2, 1, 'Analytics', 'page.dashboard.analytics', 'lucide:area-chart', '/analytics', '/dashboard/analytics/index', 0, 1, 0, '', '', 1, NOW(), 1, 1, 'system'), " +
                "(3, 1, 'Workspace', 'page.dashboard.workspace', 'carbon:workspace', '/workspace', '/dashboard/workspace/index', 0, 0, 0, '', '', 1, NOW(), 1, 1, 'system'), " +
                // 2. Demos 演示
                "(4, 0, 'Demos', 'demos.title', 'ic:baseline-view-in-ar', '/demos', 'BasicLayout', 1000, 0, 1, '', '', 1, NOW(), 1, 1, 'system'), " +
                "(5, 4, 'AntDesignDemos', 'demos.antd', '', '/demos/ant-design', '/demos/antd/index', 0, 0, 0, '', '', 1, NOW(), 1, 1, 'system'), " +
                // 3. VbenProject 项目链接
                "(6, 0, 'VbenProject', 'demos.vben.title', 'https://cdn.jsdelivr.net/gh/vbenjs/static@0.1.0/source/vben-logo.svg', '/vben-admin', 'BasicLayout', 9998, 0, 0, 'dot', '', 1, NOW(), 1, 1, 'system'), " +
                "(7, 6, 'VbenDocument', 'demos.vben.document', 'lucide:book-open-text', '/vben-admin/document', 'IFrameView', 0, 0, 0, '', 'https://doc.vben.pro', 1, NOW(), 1, 1, 'system'), " +
                "(8, 6, 'VbenGithub', 'Github', 'mdi:github', '/vben-admin/github', 'IFrameView', 0, 0, 0, '', 'https://github.com/vbenjs/vue-vben-admin', 1, NOW(), 1, 1, 'system'), " +
                // 4. About 关于
                "(9, 0, 'VbenAbout', 'demos.vben.about', 'lucide:copyright', '/vben-admin/about', '/_core/about/index', 9999, 0, 0, '', '', 1, NOW(), 1, 1, 'system')");
        log.info("初始化默认菜单数据完成");
    }

    private void initRole() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE id = 1", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO sys_role (id, role_name, role_key, sort, data_scope, status, sys001, sys003, sys005, sys006) VALUES " +
                "(1, '超级管理员', 'admin', 1, 1, 1, NOW(), 1, 1, 'system'), " +
                "(2, '普通用户', 'user', 2, 5, 1, NOW(), 1, 1, 'system')");
        log.info("初始化角色数据完成");
    }

    private void resetDevPassword() {
        jdbcTemplate.update("UPDATE sys_user SET password = '123456' WHERE username IN ('admin', 'user01') AND password LIKE '$2a$%'");
        log.info("重置开发环境密码完成");
    }

    private void initResources() {
        // 幂等补齐 id 1-31 全量资源(菜单M + 按钮F)，与 sql/init.sql 保持一致
        // INSERT IGNORE 按 id 主键幂等：已存在的行不覆盖，缺失的自动补齐
        jdbcTemplate.update("INSERT IGNORE INTO sys_resource (id, resource_name, parent_id, sort, path, component, resource_type, perms, icon, status, sys001, sys003, sys005, sys006) VALUES " +
                // 一级菜单与系统管理子菜单
                "(1, '系统管理', 0, 1, '/system', NULL, 'M', '', 'system', 1, NOW(), 1, 1, 'system'), " +
                "(2, '用户管理', 1, 1, '/system/user', 'system/user/index', 'M', 'system:user:list', 'user', 1, NOW(), 1, 1, 'system'), " +
                "(3, '角色管理', 1, 2, '/system/role', 'system/role/index', 'M', 'system:role:list', 'peoples', 1, NOW(), 1, 1, 'system'), " +
                "(4, '资源管理', 1, 3, '/system/resource', 'system/resource/index', 'M', 'system:resource:list', 'tree-table', 1, NOW(), 1, 1, 'system'), " +
                "(5, '部门管理', 1, 4, '/system/dept', 'system/dept/index', 'M', 'system:dept:list', 'tree', 1, NOW(), 1, 1, 'system'), " +
                "(6, '字典管理', 1, 5, '/system/dict', 'system/dict/index', 'M', 'system:dict:list', 'dict', 1, NOW(), 1, 1, 'system'), " +
                // 用户/角色/资源/部门/字典 按钮权限
                "(7, '用户新增', 2, 1, '', '', 'F', 'system:user:add', '', 1, NOW(), 1, 1, 'system'), " +
                "(8, '用户修改', 2, 2, '', '', 'F', 'system:user:edit', '', 1, NOW(), 1, 1, 'system'), " +
                "(9, '用户删除', 2, 3, '', '', 'F', 'system:user:remove', '', 1, NOW(), 1, 1, 'system'), " +
                "(10, '角色新增', 3, 1, '', '', 'F', 'system:role:add', '', 1, NOW(), 1, 1, 'system'), " +
                "(11, '角色修改', 3, 2, '', '', 'F', 'system:role:edit', '', 1, NOW(), 1, 1, 'system'), " +
                "(12, '角色删除', 3, 3, '', '', 'F', 'system:role:remove', '', 1, NOW(), 1, 1, 'system'), " +
                "(13, '资源新增', 4, 1, '', '', 'F', 'system:resource:add', '', 1, NOW(), 1, 1, 'system'), " +
                "(14, '资源修改', 4, 2, '', '', 'F', 'system:resource:edit', '', 1, NOW(), 1, 1, 'system'), " +
                "(15, '资源删除', 4, 3, '', '', 'F', 'system:resource:remove', '', 1, NOW(), 1, 1, 'system'), " +
                "(16, '部门新增', 5, 1, '', '', 'F', 'system:dept:add', '', 1, NOW(), 1, 1, 'system'), " +
                "(17, '部门修改', 5, 2, '', '', 'F', 'system:dept:edit', '', 1, NOW(), 1, 1, 'system'), " +
                "(18, '部门删除', 5, 3, '', '', 'F', 'system:dept:remove', '', 1, NOW(), 1, 1, 'system'), " +
                "(19, '字典新增', 6, 1, '', '', 'F', 'system:dict:add', '', 1, NOW(), 1, 1, 'system'), " +
                "(20, '字典修改', 6, 2, '', '', 'F', 'system:dict:edit', '', 1, NOW(), 1, 1, 'system'), " +
                "(21, '字典删除', 6, 3, '', '', 'F', 'system:dict:remove', '', 1, NOW(), 1, 1, 'system'), " +
                // 系统管理扩展子菜单
                "(22, '菜单管理', 1, 6, '/system/menu', 'system/menu/index', 'M', 'system:menu:list', 'menu', 1, NOW(), 1, 1, 'system'), " +
                "(23, '权限配置', 1, 7, '/system/permission', 'system/permission/index', 'M', 'system:permission:list', 'safety', 1, NOW(), 1, 1, 'system'), " +
                "(24, '用户角色管理', 1, 8, '/system/user-role', 'system/user-role/index', 'M', 'system:userrole:list', 'team', 1, NOW(), 1, 1, 'system'), " +
                "(25, '角色组织管理', 1, 9, '/system/role-dept', 'system/role-dept/index', 'M', 'system:roledept:list', 'cluster', 1, NOW(), 1, 1, 'system'), " +
                "(26, '角色资源管理', 1, 10, '/system/role-resource', 'system/role-resource/index', 'M', 'system:roleresource:list', 'apartment', 1, NOW(), 1, 1, 'system'), " +
                "(27, '操作日志', 1, 11, '/system/oper-log', 'system/oper-log/index', 'M', 'system:operlog:list', 'file-text', 1, NOW(), 1, 1, 'system'), " +
                "(28, '登录日志', 1, 12, '/system/login-log', 'system/login-log/index', 'M', 'system:loginlog:list', 'login', 1, NOW(), 1, 1, 'system'), " +
                // 菜单管理按钮
                "(29, '菜单新增', 22, 1, '', '', 'F', 'system:menu:add', '', 1, NOW(), 1, 1, 'system'), " +
                "(30, '菜单修改', 22, 2, '', '', 'F', 'system:menu:edit', '', 1, NOW(), 1, 1, 'system'), " +
                "(31, '菜单删除', 22, 3, '', '', 'F', 'system:menu:remove', '', 1, NOW(), 1, 1, 'system')");
        log.info("初始化资源权限数据完成(含系统管理菜单1-31)");
    }

    private void initUser() {
        // 确保admin用户存在
        Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = 'admin'", Integer.class);
        if (adminCount == null || adminCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, email, phone, dept_id, status, sys001, sys003, sys005, sys006) VALUES " +
                    "('admin', '123456', '超级管理员', 'admin@aladdin.com', '13800138000', 1, 1, NOW(), 1, 1, 'system')");
            // 关联admin角色
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) " +
                    "SELECT id, 1 FROM sys_user WHERE username = 'admin'");
            log.info("初始化超级管理员完成");
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = 'user01'", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, email, phone, dept_id, status, sys001, sys003, sys005, sys006) VALUES " +
                "('user01', '123456', '普通用户', 'user01@aladdin.com', '13800138001', 2, 1, NOW(), 1, 1, 'system')");
        log.info("初始化普通用户完成");
    }

    private void initRoleResource() {
        // admin角色：拥有全部资源1-31(幂等，缺失的自动补齐)
        jdbcTemplate.update("INSERT INTO sys_role_resource (role_id, resource_id) " +
                "SELECT 1, r.id FROM sys_resource r WHERE r.sys005 = 1 AND r.id BETWEEN 1 AND 31 " +
                "AND NOT EXISTS (SELECT 1 FROM sys_role_resource x WHERE x.role_id = 1 AND x.resource_id = r.id)");
        // user角色：系统管理/用户/角色/部门/字典基础菜单
        jdbcTemplate.update("INSERT INTO sys_role_resource (role_id, resource_id) " +
                "SELECT 2, r.id FROM sys_resource r WHERE r.sys005 = 1 AND r.id IN (1, 2, 3, 5, 6) " +
                "AND NOT EXISTS (SELECT 1 FROM sys_role_resource x WHERE x.role_id = 2 AND x.resource_id = r.id)");
        log.info("初始化角色资源关联完成");
    }

    private void initUserRole() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = 2", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2)");
        log.info("初始化用户角色关联完成");
    }

    private void initDictData() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict_type", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO sys_dict_type (id, dict_name, dict_type, status, sys001, sys003, sys005, sys006) VALUES " +
                "(1, '性别', 'sys_user_sex', 1, NOW(), 1, 1, 'system'), " +
                "(2, '状态', 'sys_normal_disable', 1, NOW(), 1, 1, 'system'), " +
                "(3, '资源类型', 'sys_resource_type', 1, NOW(), 1, 1, 'system'), " +
                "(4, '数据权限', 'sys_data_scope', 1, NOW(), 1, 1, 'system')");

        jdbcTemplate.update("INSERT INTO sys_dict_data (dict_type_id, dict_label, dict_value, sort, list_class, status, sys001, sys003, sys005, sys006) VALUES " +
                "(1, '男', '0', 1, 'primary', 1, NOW(), 1, 1, 'system'), " +
                "(1, '女', '1', 2, 'danger', 1, NOW(), 1, 1, 'system'), " +
                "(1, '未知', '2', 3, 'info', 1, NOW(), 1, 1, 'system'), " +
                "(2, '正常', '1', 1, 'primary', 1, NOW(), 1, 1, 'system'), " +
                "(2, '停用', '0', 2, 'danger', 1, NOW(), 1, 1, 'system'), " +
                "(3, '菜单', 'M', 1, 'primary', 1, NOW(), 1, 1, 'system'), " +
                "(3, '接口', 'A', 2, 'success', 1, NOW(), 1, 1, 'system'), " +
                "(3, '按钮', 'F', 3, 'warning', 1, NOW(), 1, 1, 'system'), " +
                "(4, '全部数据', '1', 1, 'primary', 1, NOW(), 1, 1, 'system'), " +
                "(4, '自定义数据', '2', 2, 'success', 1, NOW(), 1, 1, 'system'), " +
                "(4, '本部门数据', '3', 3, 'warning', 1, NOW(), 1, 1, 'system'), " +
                "(4, '本部门及以下', '4', 4, 'info', 1, NOW(), 1, 1, 'system'), " +
                "(4, '仅本人数据', '5', 5, 'danger', 1, NOW(), 1, 1, 'system')");
        log.info("初始化字典数据完成");
    }
}
