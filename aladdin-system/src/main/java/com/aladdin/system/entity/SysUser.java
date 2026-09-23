package com.aladdin.system.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    /** 昵称(笔名) */
    private String nickname;
    /** 姓名 */
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    /** 注册时填写的邀请码(绑定后被邀请标记) */
    private String inviteCode;
    private Long deptId;
    private Integer status;
    /** 密码最后修改时间 */
    private LocalDateTime pwdChangeTime;
    /** 是否首次登录需修改密码 */
    private Integer pwdForceChange;
    /** 租户ID */
    private Long tenantId;

    @Column(ignore = true)
    private String deptName;

    /** 用户所属部门ID集合(多部门) */
    @Column(ignore = true)
    private java.util.List<Long> deptIds;

    /** 用户所属部门名称集合展示(逗号分隔) */
    @Column(ignore = true)
    private String deptNames;
}
