package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-用户(独立于sys_user，由管理员在系统内维护)
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_user")
public class PmUser extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private Long deptId;
    /** 角色: admin管理员/owner项目集负责人/product产品经理/manager项目经理/dev研发/tester测试 */
    private String role;
    private String email;
    private String phone;
    /** 1正常 0禁用 */
    private Integer status;

    @Column(ignore = true)
    private String deptName;
}
