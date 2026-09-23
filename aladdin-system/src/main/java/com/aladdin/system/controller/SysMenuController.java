package com.aladdin.system.controller;

import com.aladdin.common.core.annotation.OpLog;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import com.aladdin.system.entity.SysMenu;
import com.aladdin.system.entity.SysResource;
import com.aladdin.system.service.SysMenuService;
import com.aladdin.system.service.SysResourceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单控制器
 * 提供前端路由所需的菜单数据
 * 默认菜单从sys_menu表查询，不受角色权限影响
 * 同时提供菜单管理CRUD功能
 *
 * @author cles
 * @date 2026/06/24
 */
@RestController
@RequestMapping({"/menu", "/system/menu"})
public class SysMenuController {

    private final SysResourceService sysResourceService;
    private final SysMenuService sysMenuService;

    public SysMenuController(SysResourceService sysResourceService, SysMenuService sysMenuService) {
        this.sysResourceService = sysResourceService;
        this.sysMenuService = sysMenuService;
    }

    /**
     * 获取当前用户的菜单树（前端路由格式）
     * 资源类型为M（菜单）的才作为路由返回
     * 若数据库无菜单数据，则返回sys_menu表中的默认菜单
     */
    @GetMapping("/all")
    public R<List<Map<String, Object>>> getAllMenus() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            return R.fail(GlobalErrorCode.UNAUTHORIZED);
        }
        List<SysResource> resources = sysResourceService.getResourcesByUserId(userId);
        // 服务层返回的是树(仅根节点+children)，先拍平成扁平列表再按M类型过滤，
        // 否则二次buildTree会对根节点setChildren(空)导致hasChildren=false而误入兜底逻辑
        List<SysResource> flat = new ArrayList<>();
        collectAll(resources, flat);
        // 只保留菜单类型(M)的资源
        List<SysResource> menuResources = flat.stream()
                .filter(r -> "M".equals(r.getResourceType()))
                .collect(Collectors.toList());
        // 数据库无菜单数据时，返回默认菜单(从sys_menu表查询，不受角色权限影响)
        if (menuResources.isEmpty()) {
            return R.ok(sysMenuService.getDefaultMenuRoutes());
        }
        // 构建树结构
        List<SysResource> tree = buildTree(menuResources);
        // 检查树是否有有效子节点（资源类型未正确配置时可能只有根节点无子节点，此时回退到默认菜单）
        boolean hasChildren = tree.stream()
                .anyMatch(r -> r.getChildren() != null && !r.getChildren().isEmpty());
        if (!hasChildren) {
            return R.ok(sysMenuService.getDefaultMenuRoutes());
        }
        // 转换为前端路由格式
        List<Map<String, Object>> routes = convertToRoutes(tree);
        return R.ok(routes);
    }

    /**
     * 获取默认菜单（前端路由格式）
     * 直接从sys_menu表查询，不受角色权限影响
     */
    @GetMapping("/default")
    public R<List<Map<String, Object>>> getDefaultMenus() {
        return R.ok(sysMenuService.getDefaultMenuRoutes());
    }

    /**
     * 菜单管理 - 分页查询
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<PageResult<SysMenu>> list(PageQuery pageQuery,
                                       @RequestParam(required = false) String menuName,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(sysMenuService.listPage(pageQuery, menuName, status));
    }

    /**
     * 菜单管理 - 获取菜单树(含停用)
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<List<SysMenu>> tree() {
        return R.ok(sysMenuService.getMenuTree());
    }

    /**
     * 菜单管理 - 获取菜单详情
     */
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<SysMenu> getById(@PathVariable Long id) {
        return R.ok(sysMenuService.getById(id));
    }

    /**
     * 菜单管理 - 新增菜单
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    @OpLog(value = "新增菜单", type = "menu")
    public R<Void> save(@RequestBody SysMenu menu) {
        return sysMenuService.save(menu) ? R.ok() : R.fail();
    }

    /**
     * 菜单管理 - 修改菜单
     */
    @PostMapping("/edit")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OpLog(value = "修改菜单", type = "menu")
    public R<Void> update(@RequestBody SysMenu menu) {
        return sysMenuService.updateById(menu) ? R.ok() : R.fail();
    }

    /**
     * 菜单管理 - 删除菜单
     */
    @PostMapping("/remove/{id}")
    @PreAuthorize("hasAuthority('system:menu:remove')")
    @OpLog(value = "删除菜单", type = "menu")
    public R<Void> remove(@PathVariable Long id) {
        return sysMenuService.removeById(id) ? R.ok() : R.fail();
    }

    /**
     * 递归拍平资源树(根节点+全部后代)
     */
    private void collectAll(List<SysResource> nodes, List<SysResource> out) {
        for (SysResource node : nodes) {
            out.add(node);
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                collectAll(node.getChildren(), out);
            }
        }
    }

    /**
     * 构建资源树
     */
    private List<SysResource> buildTree(List<SysResource> resources) {
        Map<Long, SysResource> resourceMap = new LinkedHashMap<>();
        for (SysResource resource : resources) {
            resourceMap.put(resource.getId(), resource);
            resource.setChildren(new ArrayList<>());
        }
        List<SysResource> roots = new ArrayList<>();
        for (SysResource resource : resources) {
            if (resource.getParentId() == null || resource.getParentId() == 0) {
                roots.add(resource);
            } else {
                SysResource parent = resourceMap.get(resource.getParentId());
                if (parent != null) {
                    parent.getChildren().add(resource);
                } else {
                    roots.add(resource);
                }
            }
        }
        return roots;
    }

    /**
     * 将资源树转换为前端路由格式
     */
    private List<Map<String, Object>> convertToRoutes(List<SysResource> tree) {
        List<Map<String, Object>> routes = new ArrayList<>();
        for (SysResource resource : tree) {
            if (resource.getStatus() != null && resource.getStatus() == 0) {
                continue;
            }
            Map<String, Object> route = new LinkedHashMap<>();
            route.put("name", toPascalCase(resource.getPath(), resource.getId()));
            route.put("path", resource.getPath() != null ? resource.getPath() : "");

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", resource.getResourceName());
            if (resource.getIcon() != null && !resource.getIcon().isEmpty()) {
                meta.put("icon", resource.getIcon());
            }
            if (resource.getSort() != null) {
                meta.put("order", resource.getSort());
            }
            if (resource.getPerms() != null && !resource.getPerms().isEmpty()) {
                meta.put("authority", Collections.singletonList(resource.getPerms()));
            }
            route.put("meta", meta);

            if (resource.getChildren() != null && !resource.getChildren().isEmpty()) {
                // 有子菜单，使用BasicLayout
                route.put("component", "BasicLayout");
                List<Map<String, Object>> children = convertToRoutes(resource.getChildren());
                if (!children.isEmpty()) {
                    route.put("children", children);
                }
            } else {
                // 叶子菜单，使用具体组件路径
                route.put("component", resource.getComponent() != null ? resource.getComponent() : "");
            }

            routes.add(route);
        }
        return routes;
    }

    /**
     * 将路径转换为PascalCase名称
     */
    private String toPascalCase(String path, Long id) {
        if (path == null || path.isEmpty()) {
            return "Menu" + id;
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
        return result.length() > 0 ? result.toString() : "Menu" + id;
    }
}
