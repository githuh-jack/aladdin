package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-产品模块
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_module")
public class PmModule extends BaseEntity {

    private Long productId;
    private String name;
    private Long parentId;
    private Integer sort;

    @Column(ignore = true)
    private String productName;
}
