package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysUser;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Row;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.aladdin.system.entity.table.SysDeptTableDef.SYS_DEPT;
import static com.aladdin.system.entity.table.SysResourceTableDef.SYS_RESOURCE;
import static com.aladdin.system.entity.table.SysRoleTableDef.SYS_ROLE;
import static com.aladdin.system.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户DAO
 * 单表查询走 QueryWrapper（主表逻辑删除 sys005=1 由 flex 全局配置自动附加）；
 * 被关联表统一用表名字符串 join（flex 不会对无实体表自动附加逻辑删除），
 * 关联表是否补 sys005 过滤与原 SQL 逐条对齐。
 *
 * @author cles
 * @date 2026/05/06
 */
public interface SysUserDao extends BaseDao<SysUser> {

    default SysUser selectByUsername(String username) {
        return selectOneByQuery(QueryWrapper.create()
                .from(SYS_USER)
                .where(SYS_USER.USERNAME.eq(username)));
    }

    /** 用户角色key(sys_user_role 关联表无实体，字符串join) */
    default Set<String> selectRoleKeysByUserId(Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .select(SYS_ROLE.ROLE_KEY)
                .from(SYS_ROLE)
                .innerJoin("sys_user_role")
                .on(SYS_ROLE.ID.eq(new QueryColumn("sys_user_role", "role_id")))
                .where(new QueryColumn("sys_user_role", "user_id").eq(userId));
        return new HashSet<>(selectListByQueryAs(query, String.class));
    }

    /** 用户权限标识(sys_role_resource/sys_user_role 均为关联表) */
    default Set<String> selectPermsByUserId(Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .select(SYS_RESOURCE.PERMS)
                .from(SYS_RESOURCE)
                .innerJoin("sys_role_resource")
                .on(SYS_RESOURCE.ID.eq(new QueryColumn("sys_role_resource", "resource_id")))
                .innerJoin("sys_user_role")
                .on(new QueryColumn("sys_role_resource", "role_id").eq(new QueryColumn("sys_user_role", "role_id")))
                .where(new QueryColumn("sys_user_role", "user_id").eq(userId));
        return new HashSet<>(selectListByQueryAs(query, String.class));
    }

    default Set<String> selectAllPerms() {
        QueryWrapper query = QueryWrapper.create()
                .select(SYS_RESOURCE.PERMS)
                .from(SYS_RESOURCE)
                .where(SYS_RESOURCE.PERMS.isNotNull(true))
                .and(SYS_RESOURCE.PERMS.ne(""));
        return new HashSet<>(selectListByQueryAs(query, String.class));
    }

    /** 用户详情(带部门名，原SQL不过滤已删部门，故 sys_dept 用字符串join) */
    default SysUser selectUserWithDeptById(Long id) {
        QueryWrapper query = QueryWrapper.create()
                .select(SYS_USER.DEFAULT_COLUMNS, new QueryColumn("sys_dept", "dept_name"))
                .from(SYS_USER)
                .leftJoin("sys_dept")
                .on(SYS_USER.DEPT_ID.eq(new QueryColumn("sys_dept", "id")))
                .where(SYS_USER.ID.eq(id));
        return selectOneByQuery(query);
    }

    /** 分页用户列表(带部门名与用户名/状态/部门过滤，EXISTS 等价改写为 IN 子查询) */
    default List<SysUser> selectUserListWithDept(String username, Integer status, Long deptId, int offset, int limit) {
        QueryWrapper query = QueryWrapper.create()
                .select(SYS_USER.DEFAULT_COLUMNS, new QueryColumn("sys_dept", "dept_name"))
                .from(SYS_USER)
                .leftJoin("sys_dept")
                .on(SYS_USER.DEPT_ID.eq(new QueryColumn("sys_dept", "id")));
        if (username != null && !username.isEmpty()) {
            query.and(SYS_USER.USERNAME.like(username));
        }
        if (status != null) {
            query.and(SYS_USER.STATUS.eq(status));
        }
        if (deptId != null) {
            QueryCondition deptCond = SYS_USER.DEPT_ID.eq(deptId)
                    .or(SYS_USER.ID.in(QueryWrapper.create()
                            .select(new QueryColumn("user_id"))
                            .from("sys_user_dept")
                            .where(new QueryColumn("dept_id").eq(deptId))));
            query.and(deptCond);
        }
        query.orderBy(SYS_USER.ID.asc()).offset(offset).limit(limit);
        return selectListByQuery(query);
    }

    default long countUserListWithDept(String username, Integer status, Long deptId) {
        QueryWrapper query = QueryWrapper.create().from(SYS_USER);
        if (username != null && !username.isEmpty()) {
            query.and(SYS_USER.USERNAME.like(username));
        }
        if (status != null) {
            query.and(SYS_USER.STATUS.eq(status));
        }
        if (deptId != null) {
            QueryCondition deptCond = SYS_USER.DEPT_ID.eq(deptId)
                    .or(SYS_USER.ID.in(QueryWrapper.create()
                            .select(new QueryColumn("user_id"))
                            .from("sys_user_dept")
                            .where(new QueryColumn("dept_id").eq(deptId))));
            query.and(deptCond);
        }
        return selectCountByQuery(query);
    }

    /** 用户-部门关联(含部门名；userIds 为逗号分隔ID串，来源系统内部，改解析后参数化IN防注入) */
    default List<Map<String, Object>> selectUserDeptRels(String userIds) {
        List<Long> ids = new ArrayList<>();
        if (userIds != null && !userIds.isEmpty()) {
            Arrays.stream(userIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> ids.add(Long.valueOf(s)));
        }
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper query = QueryWrapper.create()
                .select(new QueryColumn("sys_user_dept", "user_id"),
                        new QueryColumn("sys_user_dept", "dept_id"),
                        new QueryColumn("sys_dept", "dept_name"))
                .from("sys_user_dept")
                .innerJoin("sys_dept")
                .on(new QueryColumn("sys_dept", "id").eq(new QueryColumn("sys_user_dept", "dept_id"))
                        .and(SYS_DEPT.SYS005.eq(1)))
                .where(new QueryColumn("sys_user_dept", "user_id").in(ids));
        List<Row> rows = selectRowsByQuery(query);
        return rows.stream().map(r -> (Map<String, Object>) r).collect(Collectors.toList());
    }

    default int updatePassword(Long id, String password) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPassword(password);
        return update(u);
    }

    default int updateStatus(Long id, Integer status) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setStatus(status);
        return update(u);
    }

    default int updatePasswordWithTime(Long id, String password, java.time.LocalDateTime pwdChangeTime) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPassword(password);
        u.setPwdChangeTime(pwdChangeTime);
        u.setPwdForceChange(0);
        return update(u);
    }

    default int updatePwdForceChange(Long id, Integer forceChange) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPwdForceChange(forceChange);
        return update(u);
    }

    default SysUser selectByUsernameAndTenantId(String username, Long tenantId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(SYS_USER)
                .where(SYS_USER.USERNAME.eq(username))
                .and(SYS_USER.TENANT_ID.eq(tenantId)));
    }
}
