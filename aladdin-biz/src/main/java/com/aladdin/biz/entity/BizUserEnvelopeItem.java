package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户信封实例(一封一行，唯一编码不对外展示)
 *
 * @author cles
 * @date 2026/09/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_user_envelope_item")
public class BizUserEnvelopeItem extends BaseEntity {

    private Long userId;
    /** 信封模板ID */
    private Long envelopeId;
    /** 唯一编码(EN+12位序号，不对外展示) */
    private String code;
    /** 1未使用 2已使用 */
    private Integer status;
    /** 使用时间 */
    private LocalDateTime usedTime;
    /** 消耗该信封的信件ID */
    private Long letterId;
}
