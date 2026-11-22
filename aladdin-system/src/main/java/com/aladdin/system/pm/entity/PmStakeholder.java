package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-项目集干系人
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_stakeholder")
public class PmStakeholder extends BaseEntity {

    private Long programId;
    private String name;
    /** 单位/角色 */
    private String stakeholderRole;
    private String contact;
    private String remark;
}
