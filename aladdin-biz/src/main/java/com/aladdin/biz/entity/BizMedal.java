package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 奖章定义
 *
 * @author cles
 * @date 2026/10/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_medal")
public class BizMedal extends BaseEntity {

    /** 奖章名称 */
    private String name;
    /** 奖章图标URL */
    private String icon;
    /** 奖章说明 */
    private String description;
    /** 0停用 1启用 */
    private Integer status;
}
