package com.aladdin.system.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public DataInitRunner(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
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
            initPm();
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
                new String[]{"/app/paper_boat/config", "app/paper-boat/config/index", "应用配置", "lucide:settings", "7"},
                new String[]{"/app/paper_boat/avatar", "biz/avatar/index", "头像审核", "lucide:image", "8"},
                new String[]{"/app/paper_boat/stamp", "biz/shop/index", "邮票商城", "lucide:stamp", "9"},
                new String[]{"/app/paper_boat/stamp_items", "biz/stamp_items/index", "邮票列表", "lucide:list", "10"},
                new String[]{"/app/paper_boat/order", "biz/order/index", "订单管理", "lucide:shopping-cart", "11"},
                new String[]{"/app/paper_boat/announcement", "biz/announcement/index", "公告配置", "lucide:megaphone", "12"},
                new String[]{"/app/paper_boat/sysmail", "biz/sysmail/index", "系统邮件", "lucide:mail-plus", "13"},
                new String[]{"/app/paper_boat/redeem", "biz/redeem/index", "兑换码", "lucide:ticket", "14"},
                new String[]{"/app/paper_boat/feedback", "biz/feedback/index", "问题反馈", "lucide:message-square", "15"},
                new String[]{"/app/paper_boat/report", "biz/report/index", "举报处理", "lucide:shield-alert", "16"},
                new String[]{"/app/paper_boat/medal", "biz/medal/index", "勋章管理", "lucide:medal", "17"});
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
                  `star_count` int DEFAULT 0 COMMENT '小星星累计数(每寄出一封信+1)',
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
                  `is_public` int DEFAULT 0 COMMENT '0私密 1公开(可在信件广场展示)',
                  `abnormal` int DEFAULT 0 COMMENT '0正常 1异常(后台标记，异常不上广场)',
                  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
                  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
                  `arrival_time` datetime DEFAULT NULL COMMENT '到达时间(寄出时间+邮票送达天数，未到时间收件人不可见)',
                  `return_time` datetime DEFAULT NULL COMMENT '退回到达寄件人时间(退回操作+邮票送达天数，到点寄件人可见已退回)',
                  `return_reason` varchar(50) DEFAULT NULL COMMENT '退回原因(未找到指定的用户/对方拒收/审核不合格)',
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
                  `abnormal` int DEFAULT 0 COMMENT '0正常 1异常(后台标记，异常不上广场)',
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
                CREATE TABLE IF NOT EXISTS `biz_invite_record` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `inviter_id` bigint NOT NULL COMMENT '邀请人ID',
                  `invitee_id` bigint NOT NULL COMMENT '被邀请人ID',
                  `invite_code` varchar(16) DEFAULT '' COMMENT '使用的邀请码',
                  `reward_coins` int DEFAULT 0 COMMENT '奖励铜钱(文)',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(50) DEFAULT NULL,
                  `sys007` varchar(50) DEFAULT NULL,
                  PRIMARY KEY (`id`),
                  KEY `idx_inviter` (`inviter_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请记录'
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
                  `start_time` datetime DEFAULT NULL COMMENT '展示开始时间(空=不限)',
                  `end_time` datetime DEFAULT NULL COMMENT '展示结束时间(空=不限)',
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
                  `arrival_time` datetime DEFAULT NULL COMMENT '到达时间(到点后用户信箱可见)',
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
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_redeem_code` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `code` varchar(32) NOT NULL COMMENT '兑换码',
                  `coin_amount` int NOT NULL DEFAULT 0 COMMENT '兑换铜钱数',
                  `max_count` int NOT NULL DEFAULT 1 COMMENT '可兑换总次数',
                  `used_count` int NOT NULL DEFAULT 0 COMMENT '已兑换次数',
                  `stamp_id` bigint DEFAULT NULL COMMENT '赠送邮票ID(空=仅铜钱)',
                  `status` int DEFAULT 1 COMMENT '0停用 1启用',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_code` (`code`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换码配置'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_redeem_log` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `code_id` bigint NOT NULL COMMENT '兑换码ID',
                  `code` varchar(32) DEFAULT '' COMMENT '兑换码(快照)',
                  `user_id` bigint NOT NULL COMMENT '兑换用户ID',
                  `coin_amount` int NOT NULL DEFAULT 0 COMMENT '兑换铜钱数',
                  `stamp_id` bigint DEFAULT NULL COMMENT '获赠邮票ID(快照,空=无)',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_code_user` (`code_id`, `user_id`),
                  KEY `idx_user_id` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换记录'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_feedback` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `user_id` bigint NOT NULL COMMENT '反馈用户ID',
                  `content` varchar(1000) NOT NULL COMMENT '反馈内容',
                  `contact` varchar(50) DEFAULT '' COMMENT '联系方式(选填)',
                  `reply` varchar(1000) DEFAULT NULL COMMENT '处理回复',
                  `status` int DEFAULT 0 COMMENT '0待处理 1已处理',
                  `coin_amount` int DEFAULT 0 COMMENT '赠送铜钱数(0=未赠送)',
                  `admin_id` bigint DEFAULT NULL COMMENT '处理管理员ID',
                  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  KEY `idx_user_id` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题反馈'
                """,
                """
                CREATE TABLE IF NOT EXISTS `biz_email_send_log` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `email` varchar(128) NOT NULL COMMENT '收件邮箱',
                  `scene` varchar(32) DEFAULT '' COMMENT '用途场景(register注册/bind绑定/reset重置密码等)',
                  `code` varchar(10) DEFAULT '' COMMENT '验证码',
                  `status` int DEFAULT 0 COMMENT '0失败 1发送成功',
                  `fail_reason` varchar(255) DEFAULT '' COMMENT '失败原因(发送失败时)',
                  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
                  `expire_time` datetime DEFAULT NULL COMMENT '验证码过期时间',
                  `client_ip` varchar(64) DEFAULT '' COMMENT '请求IP',
                  `sys001` datetime DEFAULT NULL,
                  `sys002` datetime DEFAULT NULL,
                  `sys003` bigint DEFAULT NULL,
                  `sys004` bigint DEFAULT NULL,
                  `sys005` int DEFAULT 1,
                  `sys006` varchar(64) DEFAULT '',
                  `sys007` varchar(64) DEFAULT '',
                  PRIMARY KEY (`id`),
                  KEY `idx_email` (`email`),
                  KEY `idx_send_time` (`send_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码发送记录'
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
                "('user01', '123456', '普通用户', 'user01@aladdin.com', '13800138001', 1, 1, NOW(), 1, 1, 'system')");
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

    // ==================================================================================
    // 项目管理系统(pm)：仿禅道简易项目管理系统，独立小系统归属系统管理应用
    // pm_user 独立用户体系(id从200000起)，角色: admin/owner/product/manager/dev/tester
    // ==================================================================================

    /**
     * 初始化项目管理系统：建表 -> 应用+菜单 -> 演示数据(仅首启pm_user为空时)
     */
    private void initPm() {
        initPmTables();
        initPmAppAndMenus();
        initPmFlows();
        initPmDemoData();
    }

    /**
     * 初始化需求/缺陷默认流程定义(幂等，pm_flow_def为空时播种)
     * story_flow: 开始 -> 产品评审 -> 研发实现 -> 测试验证 -> 结束
     * bug_flow:   开始 -> 研发修复 -> 测试验证 -> 结束(缺陷无需经理确认，可直接指定修复人或由研发领取)
     */
    private void initPmFlows() {
        Integer defCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pm_flow_def", Integer.class);
        if (defCount == null || defCount == 0) {
            jdbcTemplate.update("INSERT INTO pm_flow_def (code, name, biz_type, description, status, sys001, sys003, sys005, sys006) " +
                    "VALUES ('story_flow', '需求评审流程', 'story', '需求: 评审->开发->测试->完成', 1, NOW(), 1, 1, 'pm'), " +
                    "('bug_flow', '缺陷处理流程', 'bug', '缺陷: 修复->验证->完成', 1, NOW(), 1, 1, 'pm')");
            Long storyDefId = queryId("pm_flow_def", "code", "story_flow");
            Long bugDefId = queryId("pm_flow_def", "code", "bug_flow");
            // story流程节点
            jdbcTemplate.update("INSERT INTO pm_flow_node (def_id, node_key, node_name, node_type, sort, handler_role, actions, approve_to, reject_to, biz_status, sys001, sys003, sys005, sys006) VALUES " +
                    "(" + storyDefId + ", 'start', '开始', 'start', 1, '*', 'approve', 'review', NULL, NULL, NOW(), 1, 1, 'pm'), " +
                    "(" + storyDefId + ", 'review', '产品评审', 'approval', 2, 'product', 'approve,reject,forward', 'develop', NULL, 'active', NOW(), 1, 1, 'pm'), " +
                    "(" + storyDefId + ", 'develop', '研发实现', 'task', 3, 'dev', 'complete,forward', 'test', NULL, 'active', NOW(), 1, 1, 'pm'), " +
                    "(" + storyDefId + ", 'test', '测试验证', 'approval', 4, 'tester', 'approve,reject,forward', 'end', 'develop', NULL, NOW(), 1, 1, 'pm'), " +
                    "(" + storyDefId + ", 'end', '结束', 'end', 5, '*', 'approve', NULL, NULL, 'closed', NOW(), 1, 1, 'pm')");
            // bug流程节点(无经理确认节点)
            jdbcTemplate.update("INSERT INTO pm_flow_node (def_id, node_key, node_name, node_type, sort, handler_role, actions, approve_to, reject_to, biz_status, sys001, sys003, sys005, sys006) VALUES " +
                    "(" + bugDefId + ", 'start', '开始', 'start', 1, '*', 'approve', 'fix', NULL, NULL, NOW(), 1, 1, 'pm'), " +
                    "(" + bugDefId + ", 'fix', '研发修复', 'task', 2, 'dev', 'complete,forward', 'verify', NULL, 'active', NOW(), 1, 1, 'pm'), " +
                    "(" + bugDefId + ", 'verify', '测试验证', 'approval', 3, 'tester', 'approve,reject,forward', 'end', 'fix', NULL, NOW(), 1, 1, 'pm'), " +
                    "(" + bugDefId + ", 'end', '结束', 'end', 4, '*', 'approve', NULL, NULL, 'closed', NOW(), 1, 1, 'pm')");
            log.info("初始化PM默认流程定义: 需求评审流程/缺陷处理流程");
        }
        migrateBugFlow();
    }

    /**
     * 兼容升级：历史库的bug_flow含"经理确认"节点，按新流程改为 开始->研发修复->测试验证->结束
     */
    private void migrateBugFlow() {
        try {
            Long bugDefId = jdbcTemplate.queryForObject(
                    "SELECT id FROM pm_flow_def WHERE code = 'bug_flow' ORDER BY id LIMIT 1", Long.class);
            if (bugDefId == null) {
                return;
            }
            Integer confirmCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pm_flow_node WHERE def_id = ? AND node_key = 'confirm'", Integer.class, bugDefId);
            if (confirmCount == null || confirmCount == 0) {
                return;
            }
            jdbcTemplate.update("UPDATE pm_flow_node SET approve_to = 'fix' WHERE def_id = ? AND node_key = 'start'", bugDefId);
            // 迁移停在确认节点的运行中实例到修复节点，并记录日志
            List<Long> runningIds = jdbcTemplate.queryForList(
                    "SELECT id FROM pm_flow_instance WHERE def_id = ? AND status = 'running' AND current_node = 'confirm'",
                    Long.class, bugDefId);
            jdbcTemplate.update("UPDATE pm_flow_instance SET current_node = 'fix' " +
                    "WHERE def_id = ? AND status = 'running' AND current_node = 'confirm'", bugDefId);
            for (Long instId : runningIds) {
                jdbcTemplate.update("INSERT INTO pm_flow_log (instance_id, from_node, action, to_node, remark, sys001, sys003, sys005, sys006) " +
                        "VALUES (?, 'confirm', 'auto', 'fix', '流程升级：取消经理确认节点，自动流转至研发修复', NOW(), 1, 1, 'pm')", instId);
            }
            jdbcTemplate.update("DELETE FROM pm_flow_node WHERE def_id = ? AND node_key = 'confirm'", bugDefId);
            jdbcTemplate.update("UPDATE pm_flow_node SET sort = sort - 1 WHERE def_id = ? AND sort > 2", bugDefId);
            jdbcTemplate.update("UPDATE pm_flow_def SET description = '缺陷: 修复->验证->完成' WHERE id = ?", bugDefId);
            log.info("bug_flow已升级: 移除经理确认节点，迁移{}个运行中实例", runningIds.size());
        } catch (Exception e) {
            log.warn("bug_flow迁移失败: {}", e.getMessage());
        }
    }

    private void initPmTables() {
        for (String ddl : buildPmInitSql().split(";\n")) {
            String trimmed = ddl.trim();
            if (!trimmed.isEmpty()) {
                try {
                    jdbcTemplate.execute(trimmed);
                } catch (Exception e) {
                    log.warn("创建pm表失败: {}", e.getMessage());
                }
            }
        }
        migratePmColumns();
    }

    /**
     * 兼容升级：为已有库补齐新增列(CREATE TABLE IF NOT EXISTS不会更新已存在的表)
     */
    private void migratePmColumns() {
        List<String> alters = List.of(
                "ALTER TABLE pm_bug ADD COLUMN resolve_remark varchar(500) DEFAULT '' COMMENT '解决说明'",
                "ALTER TABLE pm_bug ADD COLUMN remark varchar(500) DEFAULT '' COMMENT '转发/领取说明'",
                "ALTER TABLE pm_task ADD COLUMN finished_time datetime DEFAULT NULL COMMENT '完成时间'",
                "ALTER TABLE pm_project ADD COLUMN phase varchar(20) DEFAULT 'research' COMMENT '项目阶段'");
        for (String sql : alters) {
            try {
                jdbcTemplate.execute(sql);
                log.info("pm列升级: {}", sql);
            } catch (Exception ignored) {
                // 列已存在
            }
        }
    }

    private void initPmAppAndMenus() {
        // 菜单根节点：项目管理
        Integer rootCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_resource WHERE parent_id = 0 AND path = '/app/pm'", Integer.class);
        if (rootCount == null || rootCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_resource (resource_name, parent_id, sort, path, component, resource_type, perms, icon, status, sys001, sys003, sys005, sys006) " +
                    "VALUES ('项目管理', 0, 8, '/app/pm', NULL, 'M', '', 'lucide:kanban-square', 1, NOW(), 1, 1, 'system')");
            log.info("初始化应用菜单根节点: 项目管理(/app/pm)");
        }
        Long rootId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_resource WHERE parent_id = 0 AND path = '/app/pm' ORDER BY id LIMIT 1", Long.class);
        // 子菜单(幂等按path+parent判重)
        List<String[]> menus = List.of(
                new String[]{"/app/pm/dept", "app/pm/dept/index", "部门管理", "lucide:building-2", "1"},
                new String[]{"/app/pm/user", "app/pm/user/index", "用户管理", "lucide:users", "2"},
                new String[]{"/app/pm/program", "app/pm/program/index", "项目集管理", "lucide:layers", "3"},
                new String[]{"/app/pm/product", "app/pm/product/index", "产品管理", "lucide:package", "4"},
                new String[]{"/app/pm/module", "app/pm/module/index", "模块维护", "lucide:list-tree", "5"},
                new String[]{"/app/pm/plan", "app/pm/plan/index", "计划维护", "lucide:calendar-days", "6"},
                new String[]{"/app/pm/story", "app/pm/story/index", "需求管理", "lucide:lightbulb", "7"},
                new String[]{"/app/pm/release", "app/pm/release/index", "发布管理", "lucide:rocket", "8"},
                new String[]{"/app/pm/project", "app/pm/project/index", "项目管理", "lucide:kanban-square", "9"},
                new String[]{"/app/pm/task", "app/pm/task/index", "任务管理", "lucide:square-check-big", "10"},
                new String[]{"/app/pm/bug", "app/pm/bug/index", "缺陷管理", "lucide:bug", "11"},
                new String[]{"/app/pm/case", "app/pm/case/index", "用例管理", "lucide:flask-conical", "12"},
                new String[]{"/app/pm/commit", "app/pm/commit/index", "代码提交", "lucide:git-commit-horizontal", "13"},
                new String[]{"/app/pm/flow", "app/pm/flow/index", "流程配置", "lucide:git-merge", "14"},
                new String[]{"/app/pm/flowtodo", "app/pm/flowtodo/index", "流程待办", "lucide:list-filter", "15"},
                new String[]{"/app/pm/workbench", "app/pm/workbench/index", "工作台", "lucide:layout-dashboard", "16"},
                new String[]{"/app/pm/stat", "app/pm/stat/index", "工时统计", "lucide:chart-column", "17"},
                new String[]{"/app/pm/worklog", "app/pm/worklog/index", "工时日历", "lucide:calendar-clock", "18"},
                new String[]{"/app/pm/file", "app/pm/file/index", "项目文件", "lucide:folder-open", "19"});
        for (String[] m : menus) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_resource WHERE path = ? AND parent_id = ?", Integer.class, m[0], rootId);
            if (count == null || count == 0) {
                jdbcTemplate.update("INSERT INTO sys_resource (resource_name, parent_id, sort, path, component, resource_type, perms, icon, status, sys001, sys003, sys005, sys006) " +
                                "VALUES (?, ?, ?, ?, ?, 'M', 'pm:list', ?, 1, NOW(), 1, 1, 'system')",
                        m[2], rootId, Integer.parseInt(m[4]), m[0], m[1], m[3]);
                Long menuId = jdbcTemplate.queryForObject(
                        "SELECT id FROM sys_resource WHERE path = ? AND parent_id = ? ORDER BY id DESC LIMIT 1", Long.class, m[0], rootId);
                jdbcTemplate.update("INSERT IGNORE INTO sys_role_resource (role_id, resource_id) VALUES (1, ?)", menuId);
                log.info("初始化项目管理子菜单: {}", m[2]);
            }
        }
        // 授权根节点给admin角色
        if (rootId != null) {
            jdbcTemplate.update("INSERT INTO sys_role_resource (role_id, resource_id) " +
                    "SELECT 1, ? FROM DUAL WHERE NOT EXISTS " +
                    "(SELECT 1 FROM sys_role_resource WHERE role_id = 1 AND resource_id = ?)", rootId, rootId);
        }
        // 应用注册
        Integer appCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_app WHERE app_code = 'pm'", Integer.class);
        if (appCount == null || appCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_app (app_name, app_code, description, allow_register, menu_id, init_sql, tables_initialized, status, sys001, sys003, sys005, sys006) " +
                            "VALUES ('项目管理系统', 'pm', '仿禅道简易项目管理系统(独立用户体系)', 0, ?, ?, 1, 1, NOW(), 1, 1, 'system')",
                    rootId, buildPmInitSql());
            log.info("初始化应用: 项目管理系统(pm)");
        }
    }

    /**
     * 演示数据：仅当pm_user为空时播种(首启初始化)
     * 账号密码统一 123456：admin/owner01/pm01/mgr01/dev01/dev02/qa01
     */
    private void initPmDemoData() {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pm_user", Integer.class);
        if (userCount == null || userCount > 0) {
            return;
        }
        String now = "NOW()";
        // 部门
        jdbcTemplate.update("INSERT INTO pm_dept (parent_id, dept_name, sort, leader, status, sys001, sys003, sys005, sys006) VALUES " +
                "(0, '研发部', 1, '李经理', 1, " + now + ", 1, 1, 'pm'), " +
                "(0, '产品部', 2, '王产品', 1, " + now + ", 1, 1, 'pm'), " +
                "(0, '测试部', 3, '孙测试', 1, " + now + ", 1, 1, 'pm')");
        Long devDept = queryId("pm_dept", "dept_name", "研发部");
        Long prodDept = queryId("pm_dept", "dept_name", "产品部");
        Long testDept = queryId("pm_dept", "dept_name", "测试部");
        // 用户(BCrypt加密)
        String pwd = passwordEncoder.encode("123456");
        Object[][] users = {
                {"admin", "系统管理员", "admin", prodDept},
                {"owner01", "张总", "owner", prodDept},
                {"pm01", "王产品", "product", prodDept},
                {"mgr01", "李经理", "manager", devDept},
                {"dev01", "赵开发", "dev", devDept},
                {"dev02", "钱开发", "dev", devDept},
                {"qa01", "孙测试", "tester", testDept}};
        for (Object[] u : users) {
            jdbcTemplate.update("INSERT INTO pm_user (username, password, real_name, dept_id, role, email, phone, status, sys001, sys003, sys005, sys006) " +
                            "VALUES (?, ?, ?, ?, ?, '', '', 1, " + now + ", 1, 1, 'pm')", u[0], pwd, u[1], u[3], u[2]);
        }
        Long adminId = queryId("pm_user", "username", "admin");
        Long ownerId = queryId("pm_user", "username", "owner01");
        Long pmId = queryId("pm_user", "username", "pm01");
        Long mgrId = queryId("pm_user", "username", "mgr01");
        Long dev1Id = queryId("pm_user", "username", "dev01");
        Long dev2Id = queryId("pm_user", "username", "dev02");
        Long qaId = queryId("pm_user", "username", "qa01");
        // 项目集
        jdbcTemplate.update("INSERT INTO pm_program (name, description, budget, plan_desc, owner_id, status, sys001, sys003, sys005, sys006) " +
                        "VALUES ('智慧园区项目集', '园区智能化整体解决方案', 5000000, 'Q1完成门禁产品线，Q2上线访客系统', ?, 1, " + now + ", 1, 1, 'pm')", ownerId);
        Long programId = queryId("pm_program", "name", "智慧园区项目集");
        jdbcTemplate.update("INSERT INTO pm_stakeholder (program_id, name, stakeholder_role, contact, remark, sys001, sys003, sys005, sys006) " +
                "VALUES (" + programId + ", '业主委员会', '甲方代表', '13800000001', '验收决策方', " + now + ", 1, 1, 'pm'), " +
                "(" + programId + ", '园区物业', '使用方', '13800000002', '', " + now + ", 1, 1, 'pm')");
        // 产品
        jdbcTemplate.update("INSERT INTO pm_product (name, program_id, owner_id, description, status, sys001, sys003, sys005, sys006) " +
                "VALUES ('门禁系统', " + programId + ", " + pmId + ", '园区门禁一体化产品', 1, " + now + ", 1, 1, 'pm'), " +
                "('访客系统', " + programId + ", " + pmId + ", '访客预约与通行产品', 1, " + now + ", 1, 1, 'pm')");
        Long productId = queryId("pm_product", "name", "门禁系统");
        Long productId2 = queryId("pm_product", "name", "访客系统");
        // 模块
        jdbcTemplate.update("INSERT INTO pm_module (product_id, name, parent_id, sort, sys001, sys003, sys005, sys006) " +
                "VALUES (" + productId + ", '硬件模块', 0, 1, " + now + ", 1, 1, 'pm'), " +
                "(" + productId + ", '软件模块', 0, 2, " + now + ", 1, 1, 'pm'), " +
                "(" + productId + ", '开门算法', 0, 3, " + now + ", 1, 1, 'pm')");
        Long moduleId = queryId("pm_module", "name", "软件模块");
        // 计划
        jdbcTemplate.update("INSERT INTO pm_plan (product_id, title, description, status, sys001, sys003, sys005, sys006) " +
                "VALUES (" + productId + ", 'V1.0 迭代计划', '首版交付：基础门禁功能', 2, " + now + ", 1, 1, 'pm')");
        Long planId = queryId("pm_plan", "title", "V1.0 迭代计划");
        // 需求
        jdbcTemplate.update("INSERT INTO pm_story (product_id, module_id, plan_id, title, description, priority, status, sys001, sys003, sys005, sys006) " +
                "VALUES (" + productId + ", " + moduleId + ", " + planId + ", '刷卡开门', '支持IC卡刷卡开门，响应时间<1s', 4, 'active', " + now + ", 1, 1, 'pm'), " +
                "(" + productId + ", " + moduleId + ", " + planId + ", '密码开门', '支持数字密码开门', 3, 'active', " + now + ", 1, 1, 'pm'), " +
                "(" + productId + ", " + moduleId + ", " + planId + ", '人脸识别开门', '支持人脸识别开门', 2, 'draft', " + now + ", 1, 1, 'pm'), " +
                "(" + productId2 + ", NULL, NULL, '访客预约码', '访客线上预约生成通行码', 3, 'active', " + now + ", 1, 1, 'pm')");
        // 发布
        jdbcTemplate.update("INSERT INTO pm_release (product_id, name, description, status, sys001, sys003, sys005, sys006) " +
                "VALUES (" + productId + ", 'V1.0.0', '首个正式版本', 0, " + now + ", 1, 1, 'pm')");
        // 项目
        jdbcTemplate.update("INSERT INTO pm_project (name, program_id, manager_id, description, budget, phase, status, sys001, sys003, sys005, sys006) " +
                "VALUES ('门禁一期', " + programId + ", " + mgrId + ", '门禁系统一期交付', 800000, 'develop', 1, " + now + ", 1, 1, 'pm')");
        Long projectId = queryId("pm_project", "name", "门禁一期");
        jdbcTemplate.update("INSERT INTO pm_project_product (project_id, product_id) VALUES (" + projectId + ", " + productId + ")");
        jdbcTemplate.update("INSERT INTO pm_project_story (project_id, story_id) " +
                "SELECT " + projectId + ", id FROM pm_story WHERE status = 'active'");
        // 团队
        jdbcTemplate.update("INSERT INTO pm_team (project_id, user_id, role, sys001, sys003, sys005, sys006) " +
                "VALUES (" + projectId + ", " + mgrId + ", '经理', " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", " + dev1Id + ", '研发', " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", " + dev2Id + ", '研发', " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", " + qaId + ", '测试', " + now + ", 1, 1, 'pm')");
        // 任务
        jdbcTemplate.update("INSERT INTO pm_task (project_id, story_id, name, type, assigned_to, status, estimate_hours, description, opened_by, sys001, sys003, sys005, sys006) " +
                "VALUES (" + projectId + ", " + queryId("pm_story", "title", "刷卡开门") + ", '刷卡开门接口开发', 'devel', " + dev1Id + ", 'doing', 16, '实现刷卡开门REST接口', " + mgrId + ", " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", " + queryId("pm_story", "title", "密码开门") + ", '密码开门前端页面', 'devel', " + dev2Id + ", 'wait', 12, '', " + mgrId + ", " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", " + queryId("pm_story", "title", "刷卡开门") + ", '刷卡开门测试', 'test', " + qaId + ", 'wait', 8, '', " + mgrId + ", " + now + ", 1, 1, 'pm')");
        Long task1Id = queryId("pm_task", "name", "刷卡开门接口开发");
        // Bug
        jdbcTemplate.update("INSERT INTO pm_bug (product_id, project_id, title, steps, severity, status, opened_by, assigned_to, sys001, sys003, sys005, sys006) " +
                "VALUES (" + productId + ", " + projectId + ", '刷卡开门偶发无响应', '1.连续刷卡10次 2.观察开门日志', 2, 'active', " + qaId + ", " + dev1Id + ", " + now + ", 1, 1, 'pm'), " +
                "(" + productId + ", " + projectId + ", '密码输入框未校验长度', '输入20位密码提交', 4, 'resolved', " + qaId + ", " + dev2Id + ", " + now + ", 1, 1, 'pm')");
        // 用例
        jdbcTemplate.update("INSERT INTO pm_case (product_id, module_id, title, priority, steps, expected, last_result, executed_by, executed_time, sys001, sys003, sys005, sys006) " +
                "VALUES (" + productId + ", " + moduleId + ", '正常刷卡开门', 3, '1.有效IC卡靠近读卡器', '1秒内开门', 'pass', " + qaId + ", " + now + ", " + now + ", 1, 1, 'pm'), " +
                "(" + productId + ", " + moduleId + ", '无效卡刷卡', 3, '1.无效IC卡靠近读卡器', '不开门并提示', 'fail', " + qaId + ", " + now + ", " + now + ", 1, 1, 'pm')");
        // 代码提交
        jdbcTemplate.update("INSERT INTO pm_commit (task_id, user_id, code_url, remark, commit_time, sys001, sys003, sys005, sys006) " +
                "VALUES (" + task1Id + ", " + dev1Id + ", 'http://git.example.com/pms/door/commit/a1b2c3', '刷卡开门接口初版', " + now + ", " + now + ", 1, 1, 'pm')");
        // 工时记录(演示)
        jdbcTemplate.update("INSERT INTO pm_worklog (project_id, task_id, user_id, work_date, hours, content, sys001, sys003, sys005, sys006) " +
                "VALUES (" + projectId + ", " + task1Id + ", " + dev1Id + ", CURDATE(), 6.5, '刷卡开门接口开发与自测', " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", " + task1Id + ", " + dev1Id + ", DATE_SUB(CURDATE(), INTERVAL 1 DAY), 8, '接口联调', " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", NULL, " + dev2Id + ", CURDATE(), 5, '密码开门前端页面开发', " + now + ", 1, 1, 'pm')");
        // 项目文件(演示)
        jdbcTemplate.update("INSERT INTO pm_file (project_id, file_name, file_url, file_category, remark, uploader_id, sys001, sys003, sys005, sys006) " +
                "VALUES (" + projectId + ", '门禁一期需求说明书.docx', 'http://files.example.com/pm/door-requirement.docx', 'requirement', '一期需求基线', " + pmId + ", " + now + ", 1, 1, 'pm'), " +
                "(" + projectId + ", '接口设计说明.pdf', 'http://files.example.com/pm/door-api-design.pdf', 'design', '', " + dev1Id + ", " + now + ", 1, 1, 'pm')");
        log.info("初始化项目管理系统演示数据完成(admin/owner01/pm01/mgr01/dev01/dev02/qa01, 密码123456)");
    }

    private Long queryId(String table, String column, String value) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM " + table + " WHERE " + column + " = ? ORDER BY id DESC LIMIT 1", Long.class, value);
    }

    /**
     * 项目管理系统相关表初始化DDL(pm_user主键从200000起与sys_user/biz_user隔离)
     */
    private String buildPmInitSql() {
        String audit = "  `sys001` datetime DEFAULT NULL,\n" +
                "  `sys002` datetime DEFAULT NULL,\n" +
                "  `sys003` bigint DEFAULT NULL,\n" +
                "  `sys004` bigint DEFAULT NULL,\n" +
                "  `sys005` int DEFAULT 1,\n" +
                "  `sys006` varchar(64) DEFAULT '',\n" +
                "  `sys007` varchar(64) DEFAULT '',\n";
        String tail = "  PRIMARY KEY (`id`)\n)";
        List<String> ddls = List.of(
                "CREATE TABLE IF NOT EXISTS `pm_dept` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `parent_id` bigint DEFAULT 0,\n  `dept_name` varchar(50) DEFAULT '',\n  `sort` int DEFAULT 0,\n" +
                        "  `leader` varchar(50) DEFAULT '',\n  `status` int DEFAULT 1,\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM部门'",
                "CREATE TABLE IF NOT EXISTS `pm_user` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `username` varchar(50) NOT NULL,\n  `password` varchar(100) NOT NULL,\n  `real_name` varchar(50) DEFAULT '',\n" +
                        "  `dept_id` bigint DEFAULT NULL,\n  `role` varchar(20) DEFAULT 'dev' COMMENT 'admin/owner/product/manager/dev/tester',\n" +
                        "  `email` varchar(50) DEFAULT '',\n  `phone` varchar(20) DEFAULT '',\n  `status` int DEFAULT 1,\n" + audit +
                        "  PRIMARY KEY (`id`),\n  UNIQUE KEY `uk_username` (`username`)\n" +
                        ") ENGINE=InnoDB AUTO_INCREMENT=200000 DEFAULT CHARSET=utf8mb4 COMMENT='PM用户(独立于sys_user)'",
                "CREATE TABLE IF NOT EXISTS `pm_program` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `name` varchar(100) NOT NULL,\n  `description` varchar(500) DEFAULT '',\n  `budget` decimal(14,2) DEFAULT 0,\n" +
                        "  `plan_desc` text,\n  `owner_id` bigint DEFAULT NULL,\n  `start_date` date DEFAULT NULL,\n  `end_date` date DEFAULT NULL,\n" +
                        "  `status` int DEFAULT 1 COMMENT '1进行中 0已关闭',\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM项目集'",
                "CREATE TABLE IF NOT EXISTS `pm_stakeholder` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `program_id` bigint NOT NULL,\n  `name` varchar(50) DEFAULT '',\n  `stakeholder_role` varchar(50) DEFAULT '',\n" +
                        "  `contact` varchar(50) DEFAULT '',\n  `remark` varchar(200) DEFAULT '',\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM项目集干系人'",
                "CREATE TABLE IF NOT EXISTS `pm_product` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `name` varchar(100) NOT NULL,\n  `program_id` bigint DEFAULT NULL,\n  `owner_id` bigint DEFAULT NULL,\n" +
                        "  `description` varchar(500) DEFAULT '',\n  `status` int DEFAULT 1,\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM产品'",
                "CREATE TABLE IF NOT EXISTS `pm_module` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `product_id` bigint NOT NULL,\n  `name` varchar(100) DEFAULT '',\n  `parent_id` bigint DEFAULT 0,\n" +
                        "  `sort` int DEFAULT 0,\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM产品模块'",
                "CREATE TABLE IF NOT EXISTS `pm_plan` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `product_id` bigint NOT NULL,\n  `title` varchar(100) DEFAULT '',\n  `description` varchar(500) DEFAULT '',\n" +
                        "  `start_date` date DEFAULT NULL,\n  `end_date` date DEFAULT NULL,\n  `status` int DEFAULT 1 COMMENT '1未开始 2进行中 3已完成',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM产品计划'",
                "CREATE TABLE IF NOT EXISTS `pm_story` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `product_id` bigint NOT NULL,\n  `module_id` bigint DEFAULT NULL,\n  `plan_id` bigint DEFAULT NULL,\n" +
                        "  `title` varchar(200) NOT NULL,\n  `description` text,\n  `priority` int DEFAULT 3 COMMENT '1低2中3高4最高',\n" +
                        "  `status` varchar(20) DEFAULT 'draft' COMMENT 'draft/active/closed',\n  `closed_reason` varchar(200) DEFAULT '',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM需求'",
                "CREATE TABLE IF NOT EXISTS `pm_release` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `product_id` bigint NOT NULL,\n  `name` varchar(100) DEFAULT '',\n  `description` varchar(500) DEFAULT '',\n" +
                        "  `status` int DEFAULT 0 COMMENT '0未发布 1已发布',\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM产品发布'",
                "CREATE TABLE IF NOT EXISTS `pm_project` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `name` varchar(100) NOT NULL,\n  `program_id` bigint DEFAULT NULL,\n  `manager_id` bigint DEFAULT NULL,\n" +
                        "  `description` varchar(500) DEFAULT '',\n  `budget` decimal(14,2) DEFAULT 0,\n" +
                        "  `start_date` date DEFAULT NULL,\n  `end_date` date DEFAULT NULL,\n  `status` int DEFAULT 1 COMMENT '1进行中 0已结束',\n" +
                        "  `phase` varchar(20) DEFAULT 'research' COMMENT 'research调研/requirement需求整理/develop研发/online上线/ops运维/archive归档',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM项目'",
                "CREATE TABLE IF NOT EXISTS `pm_project_product` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `project_id` bigint NOT NULL,\n  `product_id` bigint NOT NULL,\n" + audit +
                        "  PRIMARY KEY (`id`),\n  KEY `idx_project_id` (`project_id`)\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM项目关联产品'",
                "CREATE TABLE IF NOT EXISTS `pm_project_story` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `project_id` bigint NOT NULL,\n  `story_id` bigint NOT NULL,\n" + audit +
                        "  PRIMARY KEY (`id`),\n  KEY `idx_project_id` (`project_id`)\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM项目关联需求'",
                "CREATE TABLE IF NOT EXISTS `pm_team` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `project_id` bigint NOT NULL,\n  `user_id` bigint NOT NULL,\n  `role` varchar(20) DEFAULT '研发',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM项目团队'",
                "CREATE TABLE IF NOT EXISTS `pm_task` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `project_id` bigint NOT NULL,\n  `story_id` bigint DEFAULT NULL,\n  `name` varchar(200) NOT NULL,\n" +
                        "  `type` varchar(20) DEFAULT 'devel' COMMENT 'devel/test/design/docs',\n  `assigned_to` bigint DEFAULT NULL,\n" +
                        "  `status` varchar(20) DEFAULT 'wait' COMMENT 'wait/doing/done/closed',\n  `estimate_hours` decimal(6,1) DEFAULT 0,\n" +
                        "  `description` varchar(1000) DEFAULT '',\n  `opened_by` bigint DEFAULT NULL,\n" +
                        "  `finished_time` datetime DEFAULT NULL COMMENT '完成时间',\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM任务'",
                "CREATE TABLE IF NOT EXISTS `pm_bug` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `product_id` bigint DEFAULT NULL,\n  `project_id` bigint DEFAULT NULL,\n  `title` varchar(200) NOT NULL,\n" +
                        "  `steps` text,\n  `severity` int DEFAULT 3 COMMENT '1致命2严重3一般4轻微',\n" +
                        "  `status` varchar(20) DEFAULT 'active' COMMENT 'active/resolved/closed',\n  `opened_by` bigint DEFAULT NULL,\n" +
                        "  `assigned_to` bigint DEFAULT NULL,\n  `resolved_by` bigint DEFAULT NULL,\n  `resolution` varchar(20) DEFAULT NULL,\n" +
                        "  `resolve_remark` varchar(500) DEFAULT '' COMMENT '解决说明',\n  `remark` varchar(500) DEFAULT '' COMMENT '转发/领取说明',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM缺陷'",
                "CREATE TABLE IF NOT EXISTS `pm_case` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `product_id` bigint NOT NULL,\n  `module_id` bigint DEFAULT NULL,\n  `title` varchar(200) NOT NULL,\n" +
                        "  `priority` int DEFAULT 3,\n  `steps` text,\n  `expected` text,\n" +
                        "  `last_result` varchar(20) DEFAULT 'none' COMMENT 'none/pass/fail/blocked',\n" +
                        "  `executed_by` bigint DEFAULT NULL,\n  `executed_time` datetime DEFAULT NULL,\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM测试用例'",
                "CREATE TABLE IF NOT EXISTS `pm_commit` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `task_id` bigint DEFAULT NULL,\n  `user_id` bigint DEFAULT NULL,\n  `code_url` varchar(500) DEFAULT '',\n" +
                        "  `remark` varchar(500) DEFAULT '',\n  `commit_time` datetime DEFAULT NULL,\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM代码提交'",
                "CREATE TABLE IF NOT EXISTS `pm_flow_def` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `code` varchar(50) NOT NULL COMMENT '流程编码',\n  `name` varchar(100) DEFAULT '',\n" +
                        "  `biz_type` varchar(20) DEFAULT 'story' COMMENT 'story/bug',\n  `description` varchar(500) DEFAULT '',\n" +
                        "  `status` int DEFAULT 1 COMMENT '1启用 0停用',\n" + audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM流程定义'",
                "CREATE TABLE IF NOT EXISTS `pm_flow_node` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `def_id` bigint NOT NULL COMMENT '流程定义ID',\n  `node_key` varchar(50) NOT NULL COMMENT '节点标识',\n" +
                        "  `node_name` varchar(50) DEFAULT '',\n  `node_type` varchar(20) DEFAULT 'approval' COMMENT 'start/approval/task/end',\n" +
                        "  `sort` int DEFAULT 0,\n  `handler_role` varchar(20) DEFAULT '*' COMMENT 'admin/owner/product/manager/dev/tester/*',\n" +
                        "  `actions` varchar(100) DEFAULT 'approve,reject' COMMENT 'approve/reject/forward/complete',\n" +
                        "  `approve_to` varchar(50) DEFAULT NULL COMMENT '同意后节点key',\n  `reject_to` varchar(50) DEFAULT NULL COMMENT '拒绝后节点key(空=终止)',\n" +
                        "  `biz_status` varchar(20) DEFAULT NULL COMMENT '进入节点回写业务状态',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM流程节点'",
                "CREATE TABLE IF NOT EXISTS `pm_flow_instance` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `def_id` bigint NOT NULL COMMENT '流程定义ID',\n  `biz_type` varchar(20) DEFAULT 'story',\n" +
                        "  `biz_id` bigint NOT NULL COMMENT '业务ID',\n  `current_node` varchar(50) DEFAULT '',\n" +
                        "  `current_assignee` bigint DEFAULT NULL COMMENT '当前处理人(空=按角色)',\n" +
                        "  `status` varchar(20) DEFAULT 'running' COMMENT 'running/finished/terminated',\n" +
                        "  `created_by` bigint DEFAULT NULL,\n" + audit +
                        "  PRIMARY KEY (`id`),\n  KEY `idx_biz` (`biz_type`, `biz_id`)\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM流程实例'",
                "CREATE TABLE IF NOT EXISTS `pm_flow_log` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `instance_id` bigint NOT NULL COMMENT '流程实例ID',\n  `from_node` varchar(50) DEFAULT '',\n" +
                        "  `action` varchar(20) DEFAULT '' COMMENT 'start/approve/reject/forward/complete',\n" +
                        "  `operator_id` bigint DEFAULT NULL,\n  `to_node` varchar(50) DEFAULT NULL,\n  `remark` varchar(200) DEFAULT '',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM流程操作日志'",
                "CREATE TABLE IF NOT EXISTS `pm_worklog` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `project_id` bigint NOT NULL,\n  `task_id` bigint DEFAULT NULL,\n  `story_id` bigint DEFAULT NULL,\n" +
                        "  `user_id` bigint NOT NULL,\n  `work_date` date DEFAULT NULL,\n" +
                        "  `hours` decimal(5,1) DEFAULT 0 COMMENT '工时(小时)',\n  `content` varchar(500) DEFAULT '',\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM工时记录'",
                "CREATE TABLE IF NOT EXISTS `pm_file` (\n  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                        "  `project_id` bigint NOT NULL,\n  `file_name` varchar(200) DEFAULT '',\n  `file_url` varchar(500) DEFAULT '',\n" +
                        "  `file_category` varchar(20) DEFAULT 'other' COMMENT 'survey/requirement/design/develop/test/release/ops/other',\n" +
                        "  `remark` varchar(200) DEFAULT '',\n  `uploader_id` bigint DEFAULT NULL,\n" +
                        audit + tail + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PM项目文件'");
        return String.join(";\n", ddls) + ";";
    }
}
