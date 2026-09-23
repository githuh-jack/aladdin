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
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(pq.getLimit());
        listArgs.add((pq.getPage() - 1) * pq.getLimit());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + table + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?", listArgs.toArray());
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
     * 信件列表
     */
    @GetMapping("/letter/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> letterList(PageQuery pq, @RequestParam(required = false) Long senderId) {
        StringBuilder where = new StringBuilder(" WHERE sys005 = 1");
        List<Object> args = new ArrayList<>();
        if (senderId != null) {
            where.append(" AND sender_id = ?");
            args.add(senderId);
        }
        return R.ok(pageQuery("biz_letter", pq, where, args, "id DESC"));
    }

    /**
     * 铜钱流水列表
     */
    @GetMapping("/coin/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> coinList(PageQuery pq, @RequestParam(required = false) Long userId) {
        StringBuilder where = new StringBuilder(" WHERE sys005 = 1");
        List<Object> args = new ArrayList<>();
        if (userId != null) {
            where.append(" AND user_id = ?");
            args.add(userId);
        }
        return R.ok(pageQuery("biz_coin_log", pq, where, args, "id DESC"));
    }

    /**
     * 奖励记录列表
     */
    @GetMapping("/reward/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> rewardList(PageQuery pq,
                                                         @RequestParam(required = false) Long userId,
                                                         @RequestParam(required = false) String rewardType) {
        StringBuilder where = new StringBuilder(" WHERE sys005 = 1");
        List<Object> args = new ArrayList<>();
        if (userId != null) {
            where.append(" AND user_id = ?");
            args.add(userId);
        }
        like(where, args, "reward_type", rewardType);
        return R.ok(pageQuery("biz_reward_log", pq, where, args, "id DESC"));
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
     * 文竹生长记录列表
     */
    @GetMapping("/bambooLog/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list')")
    public R<PageResult<Map<String, Object>>> bambooLogList(PageQuery pq, @RequestParam(required = false) Long userId) {
        StringBuilder where = new StringBuilder(" WHERE sys005 = 1");
        List<Object> args = new ArrayList<>();
        if (userId != null) {
            where.append(" AND user_id = ?");
            args.add(userId);
        }
        return R.ok(pageQuery("biz_bamboo_log", pq, where, args, "id DESC"));
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
