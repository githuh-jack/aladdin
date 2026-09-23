package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信封
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_envelope")
public class BizEnvelope extends BaseEntity {

    private String name;
    private String description;
    private String imageUrl;
    private Integer price;
    private Integer stock;
    private String envelopeType;
    private Integer status;
}
