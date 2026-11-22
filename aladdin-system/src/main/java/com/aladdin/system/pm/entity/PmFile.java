package com.aladdin.system.pm.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目管理系统-项目文件(资料归档)
 *
 * @author cles
 * @date 2026/11/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("pm_file")
public class PmFile extends BaseEntity {

    private Long projectId;
    /** 文件名称 */
    private String fileName;
    /** 文件链接(或路径) */
    private String fileUrl;
    /** 分类: survey调研 requirement需求 design设计 develop研发 test测试 release上线 ops运维 other其他 */
    private String fileCategory;
    private String remark;
    private Long uploaderId;

    @Column(ignore = true)
    private String uploaderName;
    @Column(ignore = true)
    private String projectName;
}
