package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-项目团队
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_team")
public class PmTeam extends BaseEntity {

    private Long projectId;
    private Long userId;
    /** 团队角色: 经理/研发/测试/其他 */
    private String role;

    @Column(ignore = true)
    private String userName;
    @Column(ignore = true)
    private String realName;
}
