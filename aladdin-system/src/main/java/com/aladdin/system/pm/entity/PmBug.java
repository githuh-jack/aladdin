package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-缺陷
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_bug")
public class PmBug extends BaseEntity {

    private Long productId;
    private Long projectId;
    private String title;
    /** 重现步骤 */
    private String steps;
    /** 严重程度 1致命 2严重 3一般 4轻微 */
    private Integer severity;
    /** active激活 resolved已解决 closed已关闭 */
    private String status;
    private Long openedBy;
    private Long assignedTo;
    private Long resolvedBy;
    /** 解决方案: fixed已修复 bydesign设计如此 notrepro无法重现 duplicate重复 */
    private String resolution;
    /** 解决说明(修复时填写) */
    private String resolveRemark;
    /** 转发/领取说明 */
    private String remark;

    @Column(ignore = true)
    private String productName;
    @Column(ignore = true)
    private String openedByName;
    @Column(ignore = true)
    private String assignedToName;
    @Column(ignore = true)
    private String resolvedByName;
}
