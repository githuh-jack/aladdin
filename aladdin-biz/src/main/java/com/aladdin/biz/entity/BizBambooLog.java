package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文竹生长记录(仅后台)
 *
 * @author cles
 * @date 2026/09/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_bamboo_log")
public class BizBambooLog extends BaseEntity {

    private Long userId;
    /** letter/diary */
    private String bizType;
    /** 本次生长(cm) */
    private Integer addCm;
    /** 内容字数 */
    private Integer contentLen;
    /** 关联信件/日记ID */
    private Long refId;
}
