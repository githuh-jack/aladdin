package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 项目管理系统-产品计划
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_plan")
public class PmPlan extends BaseEntity {

    private Long productId;
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    /** 1未开始 2进行中 3已完成 */
    private Integer status;

    @Column(ignore = true)
    private String productName;
}
