package com.aladdin.system.service;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.db.base.BaseService;
import com.aladdin.system.entity.SysMenu;

import java.util.List;
import java.util.Map;

/**
 * 系统默认菜单服务接口
 * 提供前端路由所需的默认菜单数据，不受角色权限影响
 * 同时提供菜单管理CRUD功能
 *
 * @author cles
 * @date 2026/06/24
 */
public interface SysMenuService extends BaseService<SysMenu> {

    /**
     * 获取默认菜单树(扁平列表，仅启用)
     */
    List<SysMenu> getDefaultMenuList();

    /**
     * 获取默认菜单树结构(仅启用)
     */
    List<SysMenu> getDefaultMenuTree();

    /**
     * 获取默认菜单的前端路由格式(直接可用)
     */
    List<Map<String, Object>> getDefaultMenuRoutes();

    /**
     * 获取全部菜单(扁平列表，含停用，按sort排序)
     */
    List<SysMenu> getMenuList();

    /**
     * 获取全部菜单树(含停用，按sort排序)
     */
    List<SysMenu> getMenuTree();

    /**
     * 分页查询菜单
     *
     * @param pageQuery 分页参数
     * @param menuName  菜单名称(模糊匹配，可为空)
     * @param status    状态(可为空)
     */
    PageResult<SysMenu> listPage(PageQuery pageQuery, String menuName, Integer status);
}
