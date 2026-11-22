package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目管理系统-任务
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_task")
public class PmTask extends BaseEntity {

    private Long projectId;
    private Long storyId;
    private String name;
    /** 类型: devel开发 test测试 design设计 docs文档 */
    private String type;
    private Long assignedTo;
    /** wait未开始 doing进行中 done已完成 closed已关闭 */
    private String status;
    /** 预计工时(小时) */
    private BigDecimal estimateHours;
    private String description;
    private Long openedBy;
    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    @Column(ignore = true)
    private String assignedToName;
    @Column(ignore = true)
    private String storyTitle;
    @Column(ignore = true)
    private String projectName;
    @Column(ignore = true)
    private String openedByName;
}
