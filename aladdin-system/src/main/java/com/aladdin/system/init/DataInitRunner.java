package com.aladdin.system.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

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
            initApps();
            initPaperBoatMenus();
            log.info("数据初始化完成");
        } catch (Exception e) {
            log.warn("数据初始化异常: {}", e.getMessage());
        }
    }

    /**
     * 初始化应用数据(传信纸船)
     * 幂等：应用/根部门/菜单根节点不存在时创建，并存储应用相关表初始化DDL
     */
    private void initApps() {
        // 根部门：传信纸船
        Integer deptCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dept WHERE parent_id = 0 AND dept_name = '传信纸船'", Integer.class);
        if (deptCount == null || deptCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_dept (parent_id, dept_name, sort, status, sys001, sys003, sys005, sys006) " +
                    "VALUES (0, '传信纸船', 0, 1, NOW(), 1, 1, 'system')");
            log.info("初始化根部门: 传信纸船");
        }
        Long deptId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_dept WHERE parent_id = 0 AND dept_name = '传信纸船' ORDER BY id LIMIT 1", Long.class);
        // 菜单根节点：传信纸船(应用专属菜单挂在其下)
        Integer menuCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_resource WHERE parent_id = 0 AND path = '/app/paper_boat'", Integer.class);
        if (menuCount == null || menuCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_resource (resource_name, parent_id, sort, path, component, resource_type, perms, icon, status, sys001, sys003, sys005, sys006) " +
                    "VALUES ('传信纸船', 0, 9, '/app/paper_boat', NULL, 'M', '', 'ant-design:appstore-outlined', 1, NOW(), 1, 1, 'system')");
            log.info("初始化应用菜单根节点: 传信纸船");
        }
        Long menuId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_resource WHERE parent_id = 0 AND path = '/app/paper_boat' ORDER BY id LIMIT 1", Long.class);
        // 应用：传信纸船(存储相关表初始化DDL)
        Integer appCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_app WHERE app_code = 'paper_boat'", Integer.class);
        if (appCount == null || appCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_app (app_name, app_code, description, allow_register, dept_id, menu_id, init_sql, tables_initialized, status, sys001, sys003, sys005, sys006) " +
                            "VALUES ('传信纸船', 'paper_boat', '阿拉丁传信纸船应用', 1, ?, ?, ?, 0, 1, NOW(), 1, 1, 'system')",
                    deptId, menuId, buildPaperBoatInitSql());
            log.info("初始化应用: 传信纸船");
        }
        // 兼容升级：已存在的应用行补菜单节点与初始化语句
        jdbcTemplate.update("UPDATE sys_app SET menu_id = ?, init_sql = ? " +
                        "WHERE app_code = 'paper_boat' AND (menu_id IS NULL OR init_sql IS NULL OR init_sql = '')",
                menuId, buildPaperBoatInitSql());
        // admin角色授权应用菜单根节点(NOT EXISTS判重，表无(role_id,resource_id)唯一键，INSERT IGNORE无法防重)
        if (menuId != null) {
            jdbcTemplate.update("INSERT INTO sys_role_resource (role_id, resource_id) " +
                    "SELECT 1, ? FROM DUAL WHERE NOT EXISTS " +
                    "(SELECT 1 FROM sys_role_resource WHERE role_id = 1 AND resource_id = ?)", menuId, menuId);
        }
    }

    /**
     * 初始化传信纸船应用子菜单(挂在 /app/paper_boat 根节点下，幂等按path判重)
     */
    private void initPaperBoatMenus() {
        Long rootId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_resource WHERE parent_id = 0 AND path = '/app/paper_boat' ORDER BY id LIMIT 1", Long.class);
        if (rootId == null) {
            log.warn("传信纸船菜单根节点不存在，跳过子菜单初始化");
            return;
        }
        List<String[]> menus = List.of(
                new String[]{"/app/paper_boat/user", "app/paper-boat/user/index", "应用用户", "lucide:users", "1"},
                new String[]{"/app/paper_boat/letter", "app/paper-boat/letter/index", "信件管理", "lucide:mail", "2"},
                new String[]{"/app/paper_boat/coin", "app/paper-boat/coin/index", "铜钱流水", "lucide:coins", "3"},
                new String[]{"/app/paper_boat/reward", "app/paper-boat/reward/index", "奖励记录", "lucide:gift", "4"},
                new String[]{"/app/paper_boat/invite", "app/paper-boat/invite/index", "邀请记录", "lucide:user-plus", "5"},
                new String[]{"/app/paper_boat/bambooLog", "app/paper-boat/bamboo-log/index", "文竹记录", "lucide:sprout", "6"},
                new String[]{"/app/paper_boat/config", "app/paper-boat/config/index", "应用配置", "lucide:settings", "7"});
        for (String[] m : menus) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_resource WHERE path = ? AND parent_id = ?", Integer.class, m[0], rootId);
            if (count == null || count == 0) {
                jdbcTemplate.update("INSERT INTO sys_resource (resource_name, parent_id, sort, path, component, resource_type, perms, icon, status, sys001, sys003, sys005, sys006) " +
                                "VALUES (?, ?, ?, ?, ?, 'M', 'app:paperBoat:list', ?, 1, NOW(), 1, 1, 'system')",
                        m[2], rootId, Integer.parseInt(m[4]), m[0], m[1], m[3]);
                Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM sys_resource WHERE path = ? AND parent_id = ? ORDER BY id DESC LIMIT 1", Long.class, m[0], rootId);
                jdbcTemplate.update("INSERT IGNORE INTO sys_role_resource (role_id, resource_id) VALUES (1, ?)", menuId);
                log.info("初始化传信纸船子菜单: {}", m[2]);
            }
        }
    }

    /**
     * 传信纸船应用相关表初始化DDL(biz_user为应用用户表，其余统一biz_前缀)
     */
    private String buildPaperBoatInitSql() {
        List<String> ddls = List.of(
                """
                CREATE TABLE IF NOT EXISTS `biz_user` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `username` varchar(50) NOT NULL COMMENT '用户名',
                  `password` varchar(100) NOT NULL COMMENT '密码',
                  `nickname` varchar(50) DEFAULT '' COMMENT '昵称(笔名)',
                  `real_name` varchar(50) DEFAULT '' COMMENT '姓名',
                  `email` varchar(50) DEFAULT '',
                  `phone` varchar(20) DEFAULT '',
                  `avatar` varchar(200) DEFAULT '',
                  `invite_code` varchar(16) DEFAULT '' COMMENT '注册时填写的邀请码',
                  `user_no` bigint DEFAULT NULL COMMENT '用户唯一编号(6位,从101322起,邮寄信件凭编号)',
                  `coins` int DEFAULT 0 COMMENT '铜钱余额(文)',
                  `credit_score` int DEFAULT 100 COMMENT '信用分',
                  `status` int DEFAULT 1,
                  `app_id` bigint DEFAULT NULL COMMENT '所属应用ID',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_username` (`username`),
                  UNIQUE KEY `uk_user_no` (`user_no`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用用户表'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_letter` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `sender_id` bigint NOT NULL COMMENT '发信人ID',
                  `receiver_id` bigint DEFAULT NULL COMMENT '收信人ID(地址信件时为空)',
                  `receiver_address` varchar(200) DEFAULT '' COMMENT '收件地址(寄给非好友时填写)',
                  `title` varchar(200) DEFAULT '' COMMENT '标题(可不填)',
                  `content` text,
                  `status` int DEFAULT 0 COMMENT '0草稿 1已发送 2已读 3已删除',
                  `stamp_id` bigint DEFAULT NULL COMMENT '使用的邮票ID',
                  `envelope_id` bigint DEFAULT NULL COMMENT '使用的信封ID',
                  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
                  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
                  `arrival_time` datetime DEFAULT NULL COMMENT '到达时间(寄出时间+邮票送达天数，未到时间收件人不可见)',
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信件'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日记'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='其他笔记'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_stamp` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(100) NOT NULL,
                  `description` varchar(500) DEFAULT '',
                  `image_url` varchar(500) DEFAULT '',
                  `price` int NOT NULL DEFAULT 0 COMMENT '铜钱单价',
                  `stock` int DEFAULT 0 COMMENT '库存, -1不限',
                  `stamp_type` varchar(20) DEFAULT '普通' COMMENT '普通/稀有/限量',
                  `theme` varchar(50) DEFAULT '经典' COMMENT '邮票主题(收藏分类)',
                  `series` varchar(100) DEFAULT '' COMMENT '套系主题(空为散票)',
                  `delivery_days` int DEFAULT 3 COMMENT '默认送达时间(天)',
                  `status` int DEFAULT 1 COMMENT '0下架 1上架',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮票'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_user_stamp_item` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `user_id` bigint NOT NULL,
                  `stamp_id` bigint NOT NULL COMMENT '邮票模板ID',
                  `code` varchar(32) DEFAULT NULL COMMENT '邮票唯一编码(ST+12位序号，不对外展示)',
                  `status` int DEFAULT 1 COMMENT '1未使用 2已使用',
                  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
                  `letter_id` bigint DEFAULT NULL COMMENT '消耗该邮票的信件ID',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_code` (`code`),
                  KEY `idx_user_id` (`user_id`),
                  KEY `idx_stamp_id` (`stamp_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户邮票实例(一票一行)'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信封'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_user_envelope_item` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `user_id` bigint NOT NULL,
                  `envelope_id` bigint NOT NULL COMMENT '信封模板ID',
                  `code` varchar(32) DEFAULT NULL COMMENT '信封唯一编码(EN+12位序号，不对外展示)',
                  `status` int DEFAULT 1 COMMENT '1未使用 2已使用',
                  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
                  `letter_id` bigint DEFAULT NULL COMMENT '消耗该信封的信件ID',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_code` (`code`),
                  KEY `idx_user_id` (`user_id`),
                  KEY `idx_envelope_id` (`envelope_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信封实例(一封一行)'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='铜钱流水'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_config` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `config_key` varchar(100) NOT NULL COMMENT '配置键',
                  `config_value` varchar(500) DEFAULT '' COMMENT '配置值',
                  `remark` varchar(200) DEFAULT '' COMMENT '备注',
                  `app_id` bigint DEFAULT NULL,
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_config_key` (`config_key`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用配置表'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_invite_log` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `user_id` bigint NOT NULL COMMENT '邀请人ID',
                  `invite_code` varchar(16) DEFAULT '' COMMENT '使用的邀请码',
                  `invited_user_id` bigint DEFAULT NULL COMMENT '被邀请人ID',
                  `reward_coins` int DEFAULT 0 COMMENT '奖励铜钱(文)',
                  `app_id` bigint DEFAULT NULL,
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请记录表'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_reward_log` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `user_id` bigint NOT NULL,
                  `reward_type` varchar(50) NOT NULL COMMENT '奖励类型(register/daily_login/checkin7/checkin30/profile/first_letter/first_diary/invite/stamp)',
                  `reward_coins` int DEFAULT 0 COMMENT '奖励铜钱(文)',
                  `reward_date` date DEFAULT NULL COMMENT '奖励日期',
                  `remark` varchar(200) DEFAULT '' COMMENT '备注',
                  `app_id` bigint DEFAULT NULL,
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  KEY `idx_user_type_date` (`user_id`, `reward_type`, `reward_date`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励记录表'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户文竹'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文竹生长记录'
                """,
                """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_announcement` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `title` varchar(200) NOT NULL COMMENT '公告标题',
                  `content` text COMMENT '公告内容',
                  `status` int DEFAULT 1 COMMENT '0下架 1发布',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_sys_mail` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `title` varchar(200) NOT NULL COMMENT '邮件标题',
                  `content` text COMMENT '邮件内容',
                  `coin_amount` int DEFAULT 0 COMMENT '附赠铜钱数(0为无)',
                  `stamp_id` bigint DEFAULT NULL COMMENT '附赠邮票ID',
                  `envelope_id` bigint DEFAULT NULL COMMENT '附赠信封ID',
                  `sender_id` bigint DEFAULT NULL COMMENT '发送管理员ID',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统邮件'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_user_mail` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `mail_id` bigint NOT NULL COMMENT '系统邮件ID',
                  `user_id` bigint NOT NULL COMMENT '接收用户ID',
                  `claimed` int DEFAULT 0 COMMENT '0未领取 1已领取',
                  `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_mail_user` (`mail_id`, `user_id`),
                  KEY `idx_user_id` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户系统邮件'
                """);
        return String.join(";\n\n", ddls) + ";";
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
                "(31, '菜单删除', 22, 3, '', '', 'F', 'system:menu:remove', '', 1, NOW(), 1, 1, 'system'), " +
                // 仪表盘(Dashboard)菜单：分析页/工作台，首次登录落地 analytics
                "(32, '仪表盘', 0, 0, '/dashboard', NULL, 'M', '', 'lucide:layout-dashboard', 1, NOW(), 1, 1, 'system'), " +
                "(33, '分析页', 32, 1, '/dashboard/analytics', 'dashboard/analytics/index', 'M', '', 'lucide:area-chart', 1, NOW(), 1, 1, 'system'), " +
                "(34, '工作台', 32, 2, '/dashboard/workspace', 'dashboard/workspace/index', 'M', '', 'carbon:workspace', 1, NOW(), 1, 1, 'system'), " +
                // 应用管理菜单及按钮
                "(35, '应用管理', 1, 13, '/system/app', 'system/app/index', 'M', 'system:app:list', 'ant-design:appstore-outlined', 1, NOW(), 1, 1, 'system'), " +
                "(36, '应用新增', 35, 1, '', '', 'F', 'system:app:add', '', 1, NOW(), 1, 1, 'system'), " +
                "(37, '应用修改', 35, 2, '', '', 'F', 'system:app:edit', '', 1, NOW(), 1, 1, 'system'), " +
                "(38, '应用删除', 35, 3, '', '', 'F', 'system:app:remove', '', 1, NOW(), 1, 1, 'system')");
        log.info("初始化资源权限数据完成(含系统管理菜单1-31、仪表盘32-34、应用管理35-38)");
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
        // 清理历史重复授权行(唯一键缺失时期 initApps 每次启动都会重复插入)
        jdbcTemplate.update("DELETE rr FROM sys_role_resource rr " +
                "JOIN sys_role_resource rr2 ON rr.role_id = rr2.role_id AND rr.resource_id = rr2.resource_id AND rr.id > rr2.id");
        // admin角色：拥有全部资源1-38(幂等，缺失的自动补齐)
        jdbcTemplate.update("INSERT INTO sys_role_resource (role_id, resource_id) " +
                "SELECT 1, r.id FROM sys_resource r WHERE r.sys005 = 1 AND r.id BETWEEN 1 AND 38 " +
                "AND NOT EXISTS (SELECT 1 FROM sys_role_resource x WHERE x.role_id = 1 AND x.resource_id = r.id)");
        // user角色：仪表盘/工作台/分析页 + 系统管理基础菜单
        jdbcTemplate.update("INSERT INTO sys_role_resource (role_id, resource_id) " +
                "SELECT 2, r.id FROM sys_resource r WHERE r.sys005 = 1 AND r.id IN (1, 2, 3, 5, 6, 32, 33, 34) " +
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
