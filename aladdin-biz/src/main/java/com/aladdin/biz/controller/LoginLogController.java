package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizLoginLogDao;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 登录日志控制器(后台查看)
 *
 * @author cles
 * @date 2026/09/20
 */
@RestController
@RequestMapping({"/log", "/biz/log"})
public class LoginLogController {

    @Autowired
    private BizLoginLogDao loginLogDao;

    /** 登录日志列表(管理端) */
    @GetMapping("/admin/login")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<PageResult<Map<String, Object>>> adminList(PageQuery pageQuery,
                                                        @RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) String loginName) {
        List<Map<String, Object>> rows = loginLogDao.selectLogsWithUser(userId,
                loginName == null ? null : loginName.trim(),
                pageQuery.getOffset(), pageQuery.getLimit());
        long total = loginLogDao.countLogs(userId, loginName == null ? null : loginName.trim());
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, rows));
    }
}
