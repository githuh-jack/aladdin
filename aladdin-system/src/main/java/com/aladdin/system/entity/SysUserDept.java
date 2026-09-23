package com.aladdin.system.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户部门关联实体
 *
 * @author cles
 * @date 2026/09/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_dept")
public class SysUserDept extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 是否主部门 0否 1是 */
    private Integer isPrimary;
}
