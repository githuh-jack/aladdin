package com.aladdin.system.pm.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;

/**
 * 项目管理系统-项目关联产品
 *
 * @author cles
 * @date 2026/11/02
 */
@Data
@Table("pm_project_product")
public class PmProjectProduct implements Serializable {

    @com.mybatisflex.annotation.Id(keyType = com.mybatisflex.annotation.KeyType.Auto)
    private Long id;
    private Long projectId;
    private Long productId;
}
