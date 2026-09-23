package com.aladdin.system.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.log.entity.LoginLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录日志控制器
 * 从sys_login_log表分页查询登录日志(SysLoginLogDbService落库，倒序最新在前)
 *
 * @author cles
 * @date 2026/07/03
 */
@RestController
@RequestMapping({"/loginLog", "/system/loginLog"})
public class SysLoginLogController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页查询登录日志
     *
     * @param pageQuery 分页参数
     * @param userName  用户名(模糊匹配登录名/用户名，可为空)
     * @param code      状态码(可为空，20000成功/50000失败)
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:loginlog:list')")
    public R<PageResult<LoginLog>> list(PageQuery pageQuery,
                                        @RequestParam(required = false) String userName,
                                        @RequestParam(required = false) Integer code) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (userName != null && !userName.isEmpty()) {
            where.append(" AND (login_name LIKE ? OR user_name LIKE ?)");
            params.add("%" + userName + "%");
            params.add("%" + userName + "%");
        }
        if (code != null) {
            where.append(" AND code = ?");
            params.add(code);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_login_log" + where, Long.class, params.toArray());
        if (total == null || total == 0) {
            return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), 0L, java.util.Collections.emptyList()));
        }

        String sql = "SELECT trace_id, user_id, user_name, login_name, login_ip, login_location, login_type, token, session_id, code, message, login_time" +
                " FROM sys_login_log" + where + " ORDER BY id DESC LIMIT ? OFFSET ?";
        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(pageQuery.getLimit());
        queryParams.add((pageQuery.getPage() - 1) * pageQuery.getLimit());

        List<LoginLog> page = jdbcTemplate.query(sql, (rs, rowNum) -> {
            LoginLog logRow = new LoginLog();
            logRow.setTraceId(rs.getString("trace_id"));
            long userId = rs.getLong("user_id");
            logRow.setUserId(rs.wasNull() ? null : (int) userId);
            logRow.setUserName(rs.getString("user_name"));
            logRow.setLoginName(rs.getString("login_name"));
            logRow.setLoginIp(rs.getString("login_ip"));
            logRow.setLoginLocation(rs.getString("login_location"));
            logRow.setLoginType(rs.getString("login_type"));
            logRow.setToken(rs.getString("token"));
            logRow.setSessionId(rs.getString("session_id"));
            int codeVal = rs.getInt("code");
            logRow.setCode(rs.wasNull() ? null : codeVal);
            logRow.setMessage(rs.getString("message"));
            java.sql.Timestamp t = rs.getTimestamp("login_time");
            logRow.setLoginTime(t == null ? null : t.toLocalDateTime());
            return logRow;
        }, queryParams.toArray());

        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, page));
    }
}
