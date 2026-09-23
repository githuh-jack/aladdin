package com.aladdin.system.controller;

import com.aladdin.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计控制器
 * 供前端 analytics 分析页展示：概览卡片/近7天注册登录趋势/近12月注册/系统资源雷达/登录结果/操作类型
 *
 * @author cles
 * @date 2026/09/22
 */
@RestController
@RequestMapping({"/dashboard", "/system/dashboard"})
public class DashboardController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分析页统计数据
     */
    @GetMapping("/analytics")
    public R<Map<String, Object>> analytics() {
        Map<String, Object> data = new HashMap<>();
        data.put("overview", overview());
        data.put("trends", trends());
        data.put("months", months());
        data.put("radar", radar());
        data.put("loginPie", loginPie());
        data.put("opRose", opRose());
        return R.ok(data);
    }

    /**
     * 概览4卡片：用户总量(+今日新增)/登录总量(+今日)/操作总量(+今日)/邀请注册数
     */
    private Map<String, Object> overview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("userTotal", count("SELECT COUNT(*) FROM sys_user"));
        overview.put("userToday", count("SELECT COUNT(*) FROM sys_user WHERE DATE(sys001) = CURDATE()"));
        overview.put("loginTotal", count("SELECT COUNT(*) FROM sys_login_log"));
        overview.put("loginToday", count("SELECT COUNT(*) FROM sys_login_log WHERE DATE(login_time) = CURDATE()"));
        overview.put("opTotal", count("SELECT COUNT(*) FROM sys_operation_log"));
        overview.put("opToday", count("SELECT COUNT(*) FROM sys_operation_log WHERE DATE(operation_time) = CURDATE()"));
        overview.put("inviteTotal", count("SELECT COUNT(*) FROM sys_user WHERE invite_code IS NOT NULL AND invite_code != ''"));
        return overview;
    }

    /**
     * 近7天注册+登录双折线
     */
    private Map<String, Object> trends() {
        List<String> dates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dates.add(queryString("SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL " + i + " DAY), '%m-%d')"));
        }
        Map<String, Integer> regMap = groupByDay("SELECT DATE_FORMAT(sys001, '%m-%d') d, COUNT(*) c FROM sys_user " +
                "WHERE sys001 >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) GROUP BY d");
        Map<String, Integer> loginMap = groupByDay("SELECT DATE_FORMAT(login_time, '%m-%d') d, COUNT(*) c FROM sys_login_log " +
                "WHERE login_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) GROUP BY d");
        List<Integer> registers = new ArrayList<>();
        List<Integer> logins = new ArrayList<>();
        for (String d : dates) {
            registers.add(regMap.getOrDefault(d, 0));
            logins.add(loginMap.getOrDefault(d, 0));
        }
        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("dates", dates);
        trends.put("registers", registers);
        trends.put("logins", logins);
        return trends;
    }

    /**
     * 近12月注册柱状图
     */
    private Map<String, Object> months() {
        List<String> labels = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            labels.add(queryString("SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL " + i + " MONTH), '%Y-%m')"));
        }
        Map<String, Integer> regMap = new HashMap<>();
        jdbcTemplate.query("SELECT DATE_FORMAT(sys001, '%Y-%m') m, COUNT(*) c FROM sys_user " +
                        "WHERE sys001 >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH) GROUP BY m",
                rs -> {
                    regMap.put(rs.getString(1), rs.getInt(2));
                });
        List<Integer> registers = new ArrayList<>();
        for (String m : labels) {
            registers.add(regMap.getOrDefault(m, 0));
        }
        Map<String, Object> months = new LinkedHashMap<>();
        months.put("labels", labels);
        months.put("registers", registers);
        return months;
    }

    /**
     * 系统资源雷达：用户/角色/部门/菜单/资源/字典 各表记录数
     */
    private Map<String, Object> radar() {
        List<String> indicators = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        addRadarItem(indicators, values, "用户", "SELECT COUNT(*) FROM sys_user");
        addRadarItem(indicators, values, "角色", "SELECT COUNT(*) FROM sys_role");
        addRadarItem(indicators, values, "部门", "SELECT COUNT(*) FROM sys_dept");
        addRadarItem(indicators, values, "菜单", "SELECT COUNT(*) FROM sys_menu");
        addRadarItem(indicators, values, "资源", "SELECT COUNT(*) FROM sys_resource");
        addRadarItem(indicators, values, "字典", "SELECT COUNT(*) FROM sys_dict_data");
        Map<String, Object> radar = new LinkedHashMap<>();
        radar.put("indicators", indicators);
        radar.put("values", values);
        return radar;
    }

    /**
     * 登录结果分布饼图：成功/失败
     */
    private List<Map<String, Object>> loginPie() {
        List<Map<String, Object>> pie = new ArrayList<>();
        long success = count("SELECT COUNT(*) FROM sys_login_log WHERE code = 20000");
        long fail = count("SELECT COUNT(*) FROM sys_login_log WHERE code IS NOT NULL AND code != 20000");
        Map<String, Object> successItem = new LinkedHashMap<>();
        successItem.put("name", "登录成功");
        successItem.put("value", success);
        pie.add(successItem);
        Map<String, Object> failItem = new LinkedHashMap<>();
        failItem.put("name", "登录失败");
        failItem.put("value", fail);
        pie.add(failItem);
        return pie;
    }

    /**
     * 操作类型分布玫瑰图：按操作名称分组
     */
    private List<Map<String, Object>> opRose() {
        List<Map<String, Object>> rose = new ArrayList<>();
        jdbcTemplate.query("SELECT operation, COUNT(*) c FROM sys_operation_log WHERE operation IS NOT NULL AND operation != '' " +
                        "GROUP BY operation ORDER BY c DESC LIMIT 6",
                rs -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", rs.getString(1));
                    item.put("value", rs.getInt(2));
                    rose.add(item);
                });
        return rose;
    }

    private void addRadarItem(List<String> indicators, List<Integer> values, String name, String sql) {
        try {
            indicators.add(name);
            values.add((int) count(sql));
        } catch (Exception e) {
            indicators.add(name);
            values.add(0);
        }
    }

    private Map<String, Integer> groupByDay(String sql) {
        Map<String, Integer> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getString(1), rs.getInt(2));
        });
        return map;
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }

    private String queryString(String sql) {
        String result = jdbcTemplate.queryForObject(sql, String.class);
        return result == null ? "" : result;
    }
}
