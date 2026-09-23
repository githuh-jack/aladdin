package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizBambooDao;
import com.aladdin.biz.dao.BizBambooLogDao;
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
 * 文竹控制器(仅后台查看：每个用户一株，写信/写日记生长)
 *
 * @author cles
 * @date 2026/09/20
 */
@RestController
@RequestMapping({"/bamboo", "/biz/bamboo"})
public class BambooController {

    @Autowired
    private BizBambooDao bambooDao;

    @Autowired
    private BizBambooLogDao bambooLogDao;

    /** 全部用户文竹列表(管理端) */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<PageResult<Map<String, Object>>> adminList(PageQuery pageQuery,
                                                        @RequestParam(required = false) Long userId) {
        List<Map<String, Object>> rows = bambooDao.selectListWithUser(userId, pageQuery.getOffset(), pageQuery.getLimit());
        long total = bambooDao.countList(userId);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, rows));
    }

    /** 生长记录(管理端) */
    @GetMapping("/admin/logs")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<PageResult<Map<String, Object>>> adminLogs(PageQuery pageQuery,
                                                        @RequestParam(required = false) Long userId) {
        List<Map<String, Object>> rows = bambooLogDao.selectLogsWithUser(userId, pageQuery.getOffset(), pageQuery.getLimit());
        long total = bambooLogDao.countLogs(userId);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, rows));
    }
}
