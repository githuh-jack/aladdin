package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 项目管理系统-项目
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_project")
public class PmProject extends BaseEntity {

    private String name;
    private Long programId;
    private Long managerId;
    private String description;
    /** 预算(元) */
    private BigDecimal budget;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    /** 1进行中 0已结束 */
    private Integer status;
    /** 项目阶段: research调研 requirement需求整理 develop研发 online上线 ops运维 archive归档 */
    private String phase;

    @Column(ignore = true)
    private String programName;
    @Column(ignore = true)
    private String managerName;
    /** 关联产品ID列表(仅创建/编辑时提交) */
    @Column(ignore = true)
    private List<Long> productIds;
}
