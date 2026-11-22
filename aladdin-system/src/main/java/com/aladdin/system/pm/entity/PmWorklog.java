package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目管理系统-工时记录
 *
 * @author cles
 * @date 2026/11/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_worklog")
public class PmWorklog extends BaseEntity {

    private Long projectId;
    private Long taskId;
    private Long storyId;
    private Long userId;
    /** 工作日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;
    /** 工时(小时) */
    private BigDecimal hours;
    /** 工作内容 */
    private String content;

    @Column(ignore = true)
    private String userName;
    @Column(ignore = true)
    private String realName;
    @Column(ignore = true)
    private String projectName;
    @Column(ignore = true)
    private String taskName;
    @Column(ignore = true)
    private String storyTitle;
}
