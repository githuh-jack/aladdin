package com.aladdin.system.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用实体
 *
 * @author cles
 * @date 2026/09/22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_app")
public class SysApp extends BaseEntity {

    /** 应用名称 */
    private String appName;
    /** 应用编码 */
    private String appCode;
    /** 应用描述 */
    private String description;
    /** 是否准许注册 0否 1是 */
    private Integer allowRegister;
    /** 关联根部门ID */
    private Long deptId;
    /** 关联菜单根节点ID */
    private Long menuId;
    /** 应用相关表初始化DDL语句(分号分隔) */
    private String initSql;
    /** 相关表是否已初始化 0否 1是 */
    private Integer tablesInitialized;
    private Integer status;
    private Long tenantId;

    /** 部门名称(关联查询展示) */
    @com.mybatisflex.annotation.Column(ignore = true)
    private String deptName;
}
