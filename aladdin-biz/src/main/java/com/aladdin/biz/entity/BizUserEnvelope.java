package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户持有信封
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_user_envelope")
public class BizUserEnvelope extends BaseEntity {

    private Long userId;
    private Long envelopeId;
    private Integer count;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String envelopeName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String envelopeImageUrl;
}
