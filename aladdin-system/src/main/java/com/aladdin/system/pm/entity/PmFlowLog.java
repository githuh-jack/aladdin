package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-流程操作日志
 *
 * @author cles
 * @date 2026/11/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_flow_log")
public class PmFlowLog extends BaseEntity {

    private Long instanceId;
    /** 来源节点key */
    private String fromNode;
    /** 操作: start/approve/reject/forward/complete */
    private String action;
    private Long operatorId;
    /** 目标节点key(终止/完成为空) */
    private String toNode;
    private String remark;

    @Column(ignore = true)
    private String operatorName;
    @Column(ignore = true)
    private String fromNodeName;
    @Column(ignore = true)
    private String toNodeName;
}
