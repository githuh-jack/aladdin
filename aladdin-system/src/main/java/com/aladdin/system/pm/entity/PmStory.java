package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-需求(用户故事)
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_story")
public class PmStory extends BaseEntity {

    private Long productId;
    private Long moduleId;
    private Long planId;
    private String title;
    private String description;
    /** 1低 2中 3高 4最高 */
    private Integer priority;
    /** draft草稿 active激活 closed关闭 */
    private String status;
    private String closedReason;

    @Column(ignore = true)
    private String productName;
    @Column(ignore = true)
    private String moduleName;
    @Column(ignore = true)
    private String planTitle;
}
