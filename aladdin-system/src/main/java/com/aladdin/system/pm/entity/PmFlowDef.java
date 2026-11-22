package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-流程定义(需求/缺陷流程可配置)
 *
 * @author cles
 * @date 2026/11/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_flow_def")
public class PmFlowDef extends BaseEntity {

    /** 流程编码: story_flow/bug_flow */
    private String code;
    private String name;
    /** 业务类型: story/bug */
    private String bizType;
    private String description;
    /** 1启用 0停用 */
    private Integer status;

    @com.mybatisflex.annotation.Column(ignore = true)
    private Integer nodeCount;
}
