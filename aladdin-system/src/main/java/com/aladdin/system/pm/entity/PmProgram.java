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
 * 项目管理系统-项目集
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_program")
public class PmProgram extends BaseEntity {

    private String name;
    private String description;
    /** 预算(元) */
    private BigDecimal budget;
    /** 规划描述 */
    private String planDesc;
    private Long ownerId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    /** 1进行中 0已关闭 */
    private Integer status;

    @Column(ignore = true)
    private String ownerName;
}
