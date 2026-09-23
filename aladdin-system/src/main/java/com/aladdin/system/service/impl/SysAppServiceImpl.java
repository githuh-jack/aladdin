package com.aladdin.system.service.impl;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.db.base.BaseServiceImpl;
import com.aladdin.system.dao.SysAppDao;
import com.aladdin.system.entity.SysApp;
import com.aladdin.system.entity.SysDept;
import com.aladdin.system.service.SysAppService;
import com.aladdin.system.service.SysDeptService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用服务实现
 * 每新增一个应用，自动在部门表创建同名根节点
 *
 * @author cles
 * @date 2026/09/22
 */
@Service
public class SysAppServiceImpl extends BaseServiceImpl<SysAppDao, SysApp> implements SysAppService {

    @Autowired
    @Lazy
    private SysDeptService sysDeptService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<SysApp> listPage(PageQuery pageQuery, String appName, Integer status) {
        QueryWrapper cond = buildQueryWrapper(appName, status);
        long total = getMapper().selectCountByQuery(cond);
        QueryWrapper pageWrapper = buildQueryWrapper(appName, status);
        pageWrapper.orderBy("id DESC").offset((pageQuery.getPage() - 1) * pageQuery.getLimit()).limit(pageQuery.getLimit());
        List<SysApp> list = getMapper().selectListByQuery(pageWrapper);
        return new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list);
    }

    private QueryWrapper buildQueryWrapper(String appName, Integer status) {
        QueryWrapper qw = QueryWrapper.create();
        if (appName != null && !appName.isEmpty()) {
            qw.where("app_name LIKE ?", "%" + appName + "%");
        }
        if (status != null) {
            qw.where("status = ?", status);
        }
        return qw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createApp(SysApp app) {
        if (getByAppCode(app.getAppCode()) != null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "应用编码已存在: " + app.getAppCode());
        }
        // 创建应用对应的根部门
        SysDept dept = new SysDept();
        dept.setParentId(0L);
        dept.setDeptName(app.getAppName());
        dept.setSort(0);
        dept.setStatus(1);
        dept.setSys001(LocalDateTime.now());
        dept.setSys003(1L);
        dept.setSys005(1);
        dept.setSys006("system");
        sysDeptService.save(dept);

        app.setDeptId(dept.getId());
        // 创建应用菜单根节点(sys_resource M类型，应用专属菜单挂在其下，按path判重避免重复创建)
        String menuPath = "/app/" + app.getAppCode();
        Integer menuCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_resource WHERE parent_id = 0 AND path = ?", Integer.class, menuPath);
        if (menuCount == null || menuCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_resource (resource_name, parent_id, sort, path, component, resource_type, perms, icon, status, sys001, sys003, sys005, sys006) " +
                            "VALUES (?, 0, 9, ?, NULL, 'M', '', 'ant-design:appstore-outlined', 1, NOW(), 1, 1, 'system')",
                    app.getAppName(), menuPath);
        }
        Long menuId = jdbcTemplate.queryForObject("SELECT id FROM sys_resource WHERE path = ? AND parent_id = 0 ORDER BY id ASC LIMIT 1",
                Long.class, menuPath);
        // admin角色授权该菜单根节点(NOT EXISTS判重，表无(role_id,resource_id)唯一键，INSERT IGNORE无法防重)
        if (menuId != null) {
            jdbcTemplate.update("INSERT INTO sys_role_resource (role_id, resource_id) " +
                    "SELECT 1, ? FROM DUAL WHERE NOT EXISTS " +
                    "(SELECT 1 FROM sys_role_resource WHERE role_id = 1 AND resource_id = ?)", menuId, menuId);
        }

        app.setSys001(LocalDateTime.now());
        app.setSys003(1L);
        app.setSys005(1);
        app.setSys006("system");
        if (app.getAllowRegister() == null) {
            app.setAllowRegister(1);
        }
        if (app.getStatus() == null) {
            app.setStatus(1);
        }
        return save(app);
    }

    @Override
    public boolean updateApp(SysApp app) {
        SysApp exist = getByAppCode(app.getAppCode());
        if (exist != null && !exist.getId().equals(app.getId())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "应用编码已存在: " + app.getAppCode());
        }
        app.setSys002(LocalDateTime.now());
        app.setSys004(1L);
        return updateById(app);
    }

    @Override
    public SysApp getByAppCode(String appCode) {
        return getMapper().selectOneByQuery(QueryWrapper.create().where("app_code = ?", appCode));
    }

    @Override
    public int initTables(Long appId) {
        SysApp app = getById(appId);
        if (app == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "应用不存在");
        }
        String initSql = app.getInitSql();
        if (initSql == null || initSql.trim().isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "该应用未配置相关表初始化语句");
        }
        // 按分号拆分逐条执行(跳过纯注释段)
        int executed = 0;
        for (String stmt : initSql.split(";")) {
            String sql = stmt.trim();
            sql = sql.replaceAll("(?m)^\\s*--.*$", "").trim();
            if (sql.isEmpty()) {
                continue;
            }
            jdbcTemplate.execute(sql);
            executed++;
        }
        SysApp update = new SysApp();
        update.setId(appId);
        update.setTablesInitialized(1);
        update.setSys002(LocalDateTime.now());
        updateById(update);
        return executed;
    }
}
