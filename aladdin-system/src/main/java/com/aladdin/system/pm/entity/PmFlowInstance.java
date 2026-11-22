package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-流程实例
 *
 * @author cles
 * @date 2026/11/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_flow_instance")
public class PmFlowInstance extends BaseEntity {

    private Long defId;
    /** 业务类型: story/bug */
    private String bizType;
    /** 业务ID(story/bug主键) */
    private Long bizId;
    /** 当前节点key */
    private String currentNode;
    /** 当前处理人pm_user id(空=按节点角色) */
    private Long currentAssignee;
    /** running运行中/finished已完成/terminated已终止 */
    private String status;
    private Long createdBy;

    @Column(ignore = true)
    private String defName;
    @Column(ignore = true)
    private String bizTitle;
    @Column(ignore = true)
    private String currentNodeName;
    @Column(ignore = true)
    private String currentNodeActions;
    @Column(ignore = true)
    private String currentNodeHandlerRole;
    @Column(ignore = true)
    private String assigneeName;
    @Column(ignore = true)
    private String createdByName;
}
