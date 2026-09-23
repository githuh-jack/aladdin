package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysUserGroup;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.aladdin.system.entity.table.SysResourceTableDef.SYS_RESOURCE;
import static com.aladdin.system.entity.table.SysRoleTableDef.SYS_ROLE;

/**
 * 用户组DAO
 *
 * @author cles
 * @date 2026/06/12
 */
public interface SysUserGroupDao extends BaseDao<SysUserGroup> {

    /**
     * 查询用户经用户组授予的角色 key(主表 sys005=1 由 flex 自动追加；关联表用字符串表名；Set 天然去重)
     */
    default Set<String> selectRoleKeysByUserId(Long userId) {
        List<String> keys = selectListByQueryAs(QueryWrapper.create()
                .select(SYS_ROLE.ROLE_KEY)
                .from(SYS_ROLE)
                .innerJoin("sys_user_group_role")
                .on(SYS_ROLE.ID.eq(new QueryColumn("sys_user_group_role", "role_id")))
                .innerJoin("sys_user_group_user")
                .on(new QueryColumn("sys_user_group_role", "group_id").eq(new QueryColumn("sys_user_group_user", "group_id")))
                .where(new QueryColumn("sys_user_group_user", "user_id").eq(userId)), String.class);
        return keys == null ? new HashSet<>() : new HashSet<>(keys);
    }

    /**
     * 查询用户经用户组授予的权限标识(主表 sys005=1 由 flex 自动追加；关联表用字符串表名；Set 天然去重)
     */
    default Set<String> selectPermsByUserId(Long userId) {
        List<String> perms = selectListByQueryAs(QueryWrapper.create()
                .select(SYS_RESOURCE.PERMS)
                .from(SYS_RESOURCE)
                .innerJoin("sys_role_resource")
                .on(SYS_RESOURCE.ID.eq(new QueryColumn("sys_role_resource", "resource_id")))
                .innerJoin("sys_user_group_role")
                .on(new QueryColumn("sys_role_resource", "role_id").eq(new QueryColumn("sys_user_group_role", "role_id")))
                .innerJoin("sys_user_group_user")
                .on(new QueryColumn("sys_user_group_role", "group_id").eq(new QueryColumn("sys_user_group_user", "group_id")))
                .where(new QueryColumn("sys_user_group_user", "user_id").eq(userId)), String.class);
        return perms == null ? new HashSet<>() : new HashSet<>(perms);
    }
}
