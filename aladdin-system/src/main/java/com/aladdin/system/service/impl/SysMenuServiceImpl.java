package com.aladdin.system.service.impl;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.db.base.BaseServiceImpl;
import com.aladdin.system.dao.SysMenuDao;
import com.aladdin.system.entity.SysMenu;
import com.aladdin.system.service.SysMenuService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aladdin.system.entity.table.SysMenuTableDef.SYS_MENU;

/**
 * 系统默认菜单服务实现
 * 查询sys_menu表中的默认菜单数据，不关联角色权限
 * 同时提供菜单管理CRUD功能
 *
 * @author cles
 * @date 2026/06/24
 */
@Service
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuDao, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> getDefaultMenuList() {
        return list(QueryWrapper.create()
                .where(SYS_MENU.STATUS.eq(1))
                .and(SYS_MENU.SYS005.eq(1))
                .orderBy(SYS_MENU.SORT, true));
    }

    @Override
    public List<SysMenu> getDefaultMenuTree() {
        return buildTree(getDefaultMenuList());
    }

    @Override
    public List<Map<String, Object>> getDefaultMenuRoutes() {
        List<SysMenu> tree = getDefaultMenuTree();
        return convertToRoutes(tree);
    }

    @Override
    public List<SysMenu> getMenuList() {
        return list(QueryWrapper.create()
                .where(SYS_MENU.SYS005.eq(1))
                .orderBy(SYS_MENU.SORT, true));
    }

    @Override
    public List<SysMenu> getMenuTree() {
        return buildTree(getMenuList());
    }

    @Override
    public PageResult<SysMenu> listPage(PageQuery pageQuery, String menuName, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_MENU.SYS005.eq(1))
                .and(SYS_MENU.MENU_NAME.like(menuName).when(menuName != null && !menuName.isEmpty()))
                .and(SYS_MENU.STATUS.eq(status).when(status != null))
                .orderBy(SYS_MENU.SORT, true);
        com.mybatisflex.core.paginate.Page<SysMenu> page = getMapper()
                .paginate(pageQuery.getPage(), pageQuery.getLimit(), wrapper);
        return new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords());
    }

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> grouped = menus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        menus.forEach(m -> m.setChildren(grouped.getOrDefault(m.getId(), new ArrayList<>())));
        return menus.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0L)
                .collect(Collectors.toList());
    }

    /**
     * 将菜单树转换为前端路由格式
     * 输出结构与原buildDefaultMenus()完全一致，前端可直接使用
     */
    private List<Map<String, Object>> convertToRoutes(List<SysMenu> tree) {
        List<Map<String, Object>> routes = new ArrayList<>();
        for (SysMenu menu : tree) {
            if (menu.getStatus() != null && menu.getStatus() == 0) {
                continue;
            }
            Map<String, Object> route = new LinkedHashMap<>();
            route.put("name", menu.getMenuName());
            route.put("path", menu.getPath() != null ? menu.getPath() : "");

            Map<String, Object> meta = new LinkedHashMap<>();
            if (menu.getTitle() != null && !menu.getTitle().isEmpty()) {
                meta.put("title", menu.getTitle());
            }
            if (menu.getIcon() != null && !menu.getIcon().isEmpty()) {
                meta.put("icon", menu.getIcon());
            }
            if (menu.getSort() != null) {
                meta.put("order", menu.getSort());
            }
            if (menu.getAffixTab() != null && menu.getAffixTab() == 1) {
                meta.put("affixTab", true);
            }
            if (menu.getKeepAlive() != null && menu.getKeepAlive() == 1) {
                meta.put("keepAlive", true);
            }
            if (menu.getBadgeType() != null && !menu.getBadgeType().isEmpty()) {
                meta.put("badgeType", menu.getBadgeType());
            }
            if (menu.getLink() != null && !menu.getLink().isEmpty()) {
                meta.put("link", menu.getLink());
            }
            if (!meta.isEmpty()) {
                route.put("meta", meta);
            }

            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                route.put("component", "BasicLayout");
                List<Map<String, Object>> children = convertToRoutes(menu.getChildren());
                if (!children.isEmpty()) {
                    route.put("children", children);
                }
            } else {
                route.put("component", menu.getComponent() != null ? menu.getComponent() : "");
            }

            routes.add(route);
        }
        return routes;
    }
}
