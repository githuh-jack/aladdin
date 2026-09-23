package com.aladdin.system.controller;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.annotation.OpLog;
import com.aladdin.system.entity.SysApp;
import com.aladdin.system.service.SysAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应用管理控制器
 *
 * @author cles
 * @date 2026/09/22
 */
@RestController
@RequestMapping({"/app", "/system/app"})
public class SysAppController {

    @Autowired
    private SysAppService sysAppService;

    /**
     * 分页查询应用列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:app:list')")
    public R<PageResult<SysApp>> list(PageQuery pageQuery,
                                      @RequestParam(required = false) String appName,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(sysAppService.listPage(pageQuery, appName, status));
    }

    /**
     * 新增应用(自动创建同名根部门)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:app:add')")
    @OpLog(value = "新增应用", type = "app")
    public R<Boolean> save(@RequestBody SysApp app) {
        return R.ok(sysAppService.createApp(app));
    }

    /**
     * 修改应用
     */
    @PostMapping("/edit")
    @PreAuthorize("hasAuthority('system:app:edit')")
    @OpLog(value = "修改应用", type = "app")
    public R<Boolean> edit(@RequestBody SysApp app) {
        return R.ok(sysAppService.updateApp(app));
    }

    /**
     * 删除应用
     */
    @PostMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('system:app:remove')")
    @OpLog(value = "删除应用", type = "app")
    public R<Boolean> remove(@PathVariable Long id) {
        return R.ok(sysAppService.removeById(id));
    }

    /**
     * 初始化应用相关表(执行应用存储的DDL语句)
     */
    @PostMapping("/initTables/{id}")
    @PreAuthorize("hasAuthority('system:app:edit')")
    @OpLog(value = "初始化应用相关表", type = "app")
    public R<Integer> initTables(@PathVariable Long id) {
        return R.ok(sysAppService.initTables(id));
    }
}
