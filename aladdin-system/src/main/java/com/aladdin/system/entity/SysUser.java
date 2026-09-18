package com.aladdin.system.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("realName")
    private String nickname;
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
}
