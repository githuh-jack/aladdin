package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysResource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aladdin.system.entity.table.SysResourceTableDef.SYS_RESOURCE;

/**
 * 系统资源DAO
 *
 * @author cles
 * @date 2026/06/11
 */
public interface SysResourceDao extends BaseDao<SysResource> {

    /**
     * 查询用户可见资源(主表 sys005=1 由 flex 自动追加；关联表用字符串表名；JOIN 可能放大行数，Java 侧按 id 去重替代 SQL DISTINCT)
     */
    default List<SysResource> selectResourcesByUserId(Long userId) {
        List<SysResource> list = selectListByQuery(QueryWrapper.create()
                .from(SYS_RESOURCE)
                .innerJoin("sys_role_resource")
                .on(SYS_RESOURCE.ID.eq(new QueryColumn("sys_role_resource", "resource_id")))
                .innerJoin("sys_user_role")
                .on(new QueryColumn("sys_role_resource", "role_id").eq(new QueryColumn("sys_user_role", "role_id")))
                .where(new QueryColumn("sys_user_role", "user_id").eq(userId))
                .orderBy(SYS_RESOURCE.SORT.asc()));
        return distinctById(list);
    }

    default List<String> selectPermsByUserId(Long userId) {
        List<String> perms = selectListByQueryAs(QueryWrapper.create()
                .select(SYS_RESOURCE.PERMS)
                .from(SYS_RESOURCE)
                .innerJoin("sys_role_resource")
                .on(SYS_RESOURCE.ID.eq(new QueryColumn("sys_role_resource", "resource_id")))
                .innerJoin("sys_user_role")
                .on(new QueryColumn("sys_role_resource", "role_id").eq(new QueryColumn("sys_user_role", "role_id")))
                .where(new QueryColumn("sys_user_role", "user_id").eq(userId))
                .and(SYS_RESOURCE.PERMS.isNotNull(true))
                .and(SYS_RESOURCE.PERMS.ne("")), String.class);
        return perms == null ? new ArrayList<>() : perms.stream().distinct().collect(Collectors.toList());
    }

    default List<SysResource> selectResourcesByRoleId(Long roleId) {
        List<SysResource> list = selectListByQuery(QueryWrapper.create()
                .from(SYS_RESOURCE)
                .innerJoin("sys_role_resource")
                .on(SYS_RESOURCE.ID.eq(new QueryColumn("sys_role_resource", "resource_id")))
                .where(new QueryColumn("sys_role_resource", "role_id").eq(roleId))
                .orderBy(SYS_RESOURCE.SORT.asc()));
        return distinctById(list);
    }

    /**
     * JOIN 放大去重：按主键保序去重(LinkedHashMap.putIfAbsent)
     */
    static List<SysResource> distinctById(List<SysResource> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        Map<Long, SysResource> map = new LinkedHashMap<>(list.size());
        for (SysResource r : list) {
            map.putIfAbsent(r.getId(), r);
        }
        return new ArrayList<>(map.values());
    }
}
