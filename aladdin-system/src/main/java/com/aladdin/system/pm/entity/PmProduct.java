package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-产品
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_product")
public class PmProduct extends BaseEntity {

    private String name;
    private Long programId;
    private Long ownerId;
    private String description;
    /** 1正常 0停用 */
    private Integer status;

    @Column(ignore = true)
    private String programName;
    @Column(ignore = true)
    private String ownerName;
}
