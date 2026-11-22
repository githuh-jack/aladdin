package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-流程节点
 *
 * @author cles
 * @date 2026/11/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_flow_node")
public class PmFlowNode extends BaseEntity {

    private Long defId;
    /** 节点标识(流程内唯一) */
    private String nodeKey;
    private String nodeName;
    /** 节点类型: start开始/approval审批/task执行/end结束 */
    private String nodeType;
    private Integer sort;
    /** 处理角色: admin/owner/product/manager/dev/tester/*任意 */
    private String handlerRole;
    /** 可用操作(逗号分隔): approve,reject,forward,complete */
    private String actions;
    /** 同意后进入的节点key */
    private String approveTo;
    /** 拒绝后进入的节点key(空=终止流程) */
    private String rejectTo;
    /** 进入该节点时回写业务状态(story: draft/active/closed; bug: active/resolved/closed) */
    private String bizStatus;
}
