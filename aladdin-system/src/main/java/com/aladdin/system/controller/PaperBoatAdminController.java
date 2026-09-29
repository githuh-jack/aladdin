package com.aladdin.system.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 传信纸船应用管理接口
 * 查询应用相关表(biz_前缀，需先在应用管理页执行"初始化相关表"建表)
 *
 * @author cles
 * @date 2026/09/22
 */
@RestController
@RequestMapping("/system/paperBoat")
public class PaperBoatAdminController {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页查询公共方法
     */
    private PageResult<Map<String, Object>> pageQuery(String table, PageQuery pq,
                                                      StringBuilder where, List<Object> args, String orderBy) {
        return pageQuery("*", table, pq, where, args, orderBy);
    }

    /**
     * 分页查询公共方法(支持自定义SELECT列与JOIN，from子句直接传入)
     */
    private PageResult<Map<String, Object>> pageQuery(String selectColumns, String from, PageQuery pq,
                                                      StringBuilder where, List<Object> args, String orderBy) {
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + from + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(pq.getLimit());
        listArgs.add((pq.getPage() - 1) * pq.getLimit());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + selectColumns + " FROM " + from + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?", listArgs.toArray());
        // 日期字段格式化为字符串，便于前端展示
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_PATTERN);
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getValue() instanceof Date) {
                    entry.setValue(sdf.format((Date) entry.getValue()));
                }
            }
        }
        return new PageResult<>(pq.getPage(), pq.getLimit(), total == null ? 0 : total, rows);
    }

    private void like(StringBuilder where, List<Object> args, String column, String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND ").append(column).append(" LIKE ?");
            args.add("%" + keyword + "%");
        }
    }

    /** 按用户笔名/编号过滤(编号精确匹配，笔名模糊匹配) */
    private void userKeyword(StringBuilder where, List<Object> args, String alias, String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (").append(alias).append(".nickname LIKE ? OR ").append(alias).append(".user_no = ?)");
            args.add("%" + keyword + "%");
            args.add(keyword);
        }
    }

    /**
     * 应用用户列表
     */
    @GetMapping("/user/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> userList(PageQuery pq, @RequestParam(required = false) String username) {
        StringBuilder where = new StringBuilder(" WHERE sys005 = 1");
        List<Object> args = new ArrayList<>();
        like(where, args, "username", username);
        like(where, args, "nickname", username);
        return R.ok(pageQuery("biz_user", pq, where, args, "id ASC"));
    }

    /**
     * 信件列表(JOIN用户表返回笔名/编号，不暴露用户ID)
     */
    @GetMapping("/letter/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> letterList(PageQuery pq, @RequestParam(required = false) String keyword) {
        String from = "biz_letter l " +
                "LEFT JOIN biz_user su ON l.sender_id = su.id " +
                "LEFT JOIN biz_user ru ON l.receiver_id = ru.id";
        String columns = "l.*, su.nickname AS sender_name, su.user_no AS sender_no, " +
                "ru.nickname AS receiver_name, ru.user_no AS receiver_no";
        StringBuilder where = new StringBuilder(" WHERE l.sys005 = 1");
        List<Object> args = new ArrayList<>();
        userKeyword(where, args, "su", keyword);
        return R.ok(pageQuery(columns, from, pq, where, args, "l.id DESC"));
    }

    /**
     * 铜钱流水列表(JOIN用户表返回编号/笔名，不暴露用户ID)
     */
    @GetMapping("/coin/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> coinList(PageQuery pq, @RequestParam(required = false) String keyword) {
        String from = "biz_coin_log c LEFT JOIN biz_user u ON c.user_id = u.id";
        String columns = "c.*, u.nickname AS user_name, u.user_no AS user_no";
        StringBuilder where = new StringBuilder(" WHERE c.sys005 = 1");
        List<Object> args = new ArrayList<>();
        userKeyword(where, args, "u", keyword);
        return R.ok(pageQuery(columns, from, pq, where, args, "c.id DESC"));
    }

    /**
     * 奖励记录列表(奖励发放流水：注册/每日奖励等，来自 biz_coin_log)
     */
    @GetMapping("/reward/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> rewardList(PageQuery pq, @RequestParam(required = false) String keyword) {
        String from = "biz_coin_log c LEFT JOIN biz_user u ON c.user_id = u.id";
        String columns = "c.id, c.user_id, u.user_no AS user_no, u.nickname AS user_name, " +
                "c.log_type AS reward_type, c.change_amount AS reward_coins, c.remark, c.sys001 AS reward_date";
        StringBuilder where = new StringBuilder(" WHERE c.sys005 = 1 AND c.log_type IN ('reward', 'register')");
        List<Object> args = new ArrayList<>();
        userKeyword(where, args, "u", keyword);
        return R.ok(pageQuery(columns, from, pq, where, args, "c.id DESC"));
    }

    /**
     * 邀请记录列表
     */
    @GetMapping("/invite/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> inviteList(PageQuery pq, @RequestParam(required = false) Long userId) {
        StringBuilder where = new StringBuilder(" WHERE sys005 = 1");
        List<Object> args = new ArrayList<>();
        if (userId != null) {
            where.append(" AND user_id = ?");
            args.add(userId);
        }
        return R.ok(pageQuery("biz_invite_log", pq, where, args, "id DESC"));
    }

    /**
     * 应用配置列表
     */
    @GetMapping("/config/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> configList(PageQuery pq, @RequestParam(required = false) String configKey) {
        StringBuilder where = new StringBuilder(" WHERE sys005 = 1");
        List<Object> args = new ArrayList<>();
        like(where, args, "config_key", configKey);
        return R.ok(pageQuery("biz_config", pq, where, args, "id ASC"));
    }

    /**
     * 保存应用配置(新增或按config_key更新)
     */
    @PostMapping("/config/save")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<Boolean> saveConfig(@RequestBody Map<String, String> body) {
        String configKey = body.get("config_key");
        String configValue = body.getOrDefault("config_value", "");
        String remark = body.getOrDefault("remark", "");
        if (configKey == null || configKey.isEmpty()) {
            return R.fail("配置键不能为空");
        }
        jdbcTemplate.update("INSERT INTO biz_config (config_key, config_value, remark, sys001, sys003, sys005, sys006) " +
                        "VALUES (?, ?, ?, NOW(), 1, 1, 'system') " +
                        "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), remark = VALUES(remark), sys002 = NOW()",
                configKey, configValue, remark);
        return R.ok(true);
    }

    /**
     * 删除应用配置
     */
    @PostMapping("/config/remove/{id}")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<Boolean> removeConfig(@org.springframework.web.bind.annotation.PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM biz_config WHERE id = ?", id);
        return R.ok(true);
    }
}
