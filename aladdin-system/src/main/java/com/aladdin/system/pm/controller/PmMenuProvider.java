package com.aladdin.system.pm.controller;

import com.aladdin.system.dao.SysResourceDao;
import com.aladdin.system.entity.SysResource;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.entity.PmUser;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目管理系统-菜单提供者
 * /menu/all 识别pm登录用户(pm_user.id>=200000)时，按角色返回/app/pm下的菜单子集
 *
 * @author cles
 * @date 2026/11/02
 */
@Component
public class PmMenuProvider {

    public static final String PM_MENU_ROOT_PATH = "/app/pm";

    /** 角色 -> 可见子菜单path集合(admin见全部) */
    private static final Map<String, Set<String>> ROLE_MENUS = Map.of(
            "owner", Set.of("/app/pm/program", "/app/pm/project", "/app/pm/workbench", "/app/pm/stat", "/app/pm/worklog", "/app/pm/flowtodo"),
            "product", Set.of("/app/pm/product", "/app/pm/module", "/app/pm/plan", "/app/pm/story", "/app/pm/release", "/app/pm/workbench", "/app/pm/worklog", "/app/pm/file", "/app/pm/flowtodo"),
            "manager", Set.of("/app/pm/project", "/app/pm/story", "/app/pm/task", "/app/pm/workbench", "/app/pm/stat", "/app/pm/worklog", "/app/pm/file", "/app/pm/flowtodo"),
            "dev", Set.of("/app/pm/task", "/app/pm/bug", "/app/pm/commit", "/app/pm/workbench", "/app/pm/worklog", "/app/pm/file", "/app/pm/flowtodo"),
            "tester", Set.of("/app/pm/case", "/app/pm/bug", "/app/pm/workbench", "/app/pm/worklog", "/app/pm/file", "/app/pm/flowtodo"));

    private final SysResourceDao sysResourceDao;
    private final PmUserDao pmUserDao;

    public PmMenuProvider(SysResourceDao sysResourceDao, PmUserDao pmUserDao) {
        this.sysResourceDao = sysResourceDao;
        this.pmUserDao = pmUserDao;
    }

    /**
     * 判断该登录态id是否为pm用户
     */
    public static boolean isPmUser(Long userId) {
        return userId != null && userId >= PmAuthHelper.PM_USER_ID_OFFSET;
    }

    /**
     * 按pm用户角色构建菜单路由(格式与SysMenuController.convertToRoutes一致)
     */
    public List<Map<String, Object>> getRoutes(Long pmLoginId) {
        PmUser user = pmUserDao.selectOneById(pmLoginId - PmAuthHelper.PM_USER_ID_OFFSET);
        if (user == null) {
            return List.of();
        }
        String role = user.getRole() == null ? "admin" : user.getRole();
        List<SysResource> all = sysResourceDao.selectListByQuery(QueryWrapper.create()
                .where(new QueryColumn("path").likeLeft(PM_MENU_ROOT_PATH + "/"))
                .and(new QueryColumn("resource_type").eq("M"))
                .and(new QueryColumn("status").eq(1)));
        if (all.isEmpty()) {
            return List.of();
        }
        // 根节点
        SysResource root = all.stream()
                .filter(r -> PM_MENU_ROOT_PATH.equals(r.getPath()))
                .findFirst().orElse(null);
        // 子菜单
        List<SysResource> children = all.stream()
                .filter(r -> !PM_MENU_ROOT_PATH.equals(r.getPath()))
                .sorted(Comparator.comparing(r -> r.getSort() == null ? 0 : r.getSort()))
                .collect(java.util.stream.Collectors.toList());
        if (!"admin".equals(role)) {
            Set<String> allowed = ROLE_MENUS.getOrDefault(role, Collections.emptySet());
            children.removeIf(r -> !allowed.contains(r.getPath()));
        }
        if (children.isEmpty()) {
            return List.of();
        }
        // 输出: 根(BasicLayout) + children
        List<Map<String, Object>> childRoutes = new ArrayList<>();
        for (SysResource child : children) {
            Map<String, Object> route = new LinkedHashMap<>();
            route.put("name", toPascalCase(child.getPath(), child.getId()));
            route.put("path", child.getPath());
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", child.getResourceName());
            if (child.getIcon() != null && !child.getIcon().isEmpty()) {
                meta.put("icon", child.getIcon());
            }
            if (child.getSort() != null) {
                meta.put("order", child.getSort());
            }
            route.put("meta", meta);
            route.put("component", child.getComponent() == null ? "" : child.getComponent());
            childRoutes.add(route);
        }
        if (root == null) {
            return childRoutes;
        }
        Map<String, Object> rootRoute = new LinkedHashMap<>();
        rootRoute.put("name", toPascalCase(root.getPath(), root.getId()));
        rootRoute.put("path", root.getPath());
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", root.getResourceName());
        if (root.getIcon() != null && !root.getIcon().isEmpty()) {
            meta.put("icon", root.getIcon());
        }
        meta.put("order", root.getSort());
        rootRoute.put("meta", meta);
        rootRoute.put("component", "BasicLayout");
        rootRoute.put("children", childRoutes);
        return List.of(rootRoute);
    }

    private String toPascalCase(String path, Long id) {
        if (path == null || path.isEmpty()) {
            return "PmMenu" + id;
        }
        String name = path.replace("/", " ").trim();
        StringBuilder result = new StringBuilder();
        for (String part : name.split("\\s+")) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
        }
        return result.length() > 0 ? result.toString() : "PmMenu" + id;
    }
}
