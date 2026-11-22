package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-产品发布
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_release")
public class PmRelease extends BaseEntity {

    private Long productId;
    private String name;
    private String description;
    /** 0未发布 1已发布 */
    private Integer status;

    @Column(ignore = true)
    private String productName;
}
