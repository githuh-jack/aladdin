package com.aladdin.system.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统默认菜单实体
 * 存储前端路由所需的默认菜单数据，不受角色权限影响
 *
 * @author cles
 * @date 2026/06/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_menu")
public class SysMenu extends BaseEntity {

    /** 父菜单ID */
    private Long parentId;
    /** 路由name(唯一) */
    private String menuName;
    /** 菜单标题(i18n key) */
    private String title;
    /** 图标名或URL */
    private String icon;
    /** 路由路径 */
    private String path;
    /** 组件路径或布局名(BasicLayout/IFrameView/具体路径) */
    private String component;
    /** 排序 */
    private Integer sort;
    /** 是否固定tab 0否1是 */
    private Integer affixTab;
    /** 是否缓存 0否1是 */
    private Integer keepAlive;
    /** 徽标类型(dot等) */
    private String badgeType;
    /** 外链URL */
    private String link;
    /** 状态 0停用 1启用 */
    private Integer status;

    @Column(ignore = true)
    private List<SysMenu> children;
}
