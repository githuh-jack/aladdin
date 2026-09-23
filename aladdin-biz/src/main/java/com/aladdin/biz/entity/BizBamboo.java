package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户文竹(每个用户一株)
 *
 * @author cles
 * @date 2026/09/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_bamboo")
public class BizBamboo extends BaseEntity {

    private Long userId;
    /** 当前高度(cm) */
    private Integer heightCm;
}
