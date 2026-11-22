package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 项目管理系统-测试用例
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_case")
public class PmCase extends BaseEntity {

    private Long productId;
    private Long moduleId;
    private String title;
    /** 1低 2中 3高 4最高 */
    private Integer priority;
    /** 前置步骤(JSON或分号分隔) */
    private String steps;
    private String expected;
    /** none未执行 pass通过 fail失败 blocked阻塞 */
    private String lastResult;
    private Long executedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedTime;

    @Column(ignore = true)
    private String productName;
    @Column(ignore = true)
    private String moduleName;
    @Column(ignore = true)
    private String executedByName;
}
