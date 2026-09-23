package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户持有邮票
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_user_stamp")
public class BizUserStamp extends BaseEntity {

    private Long userId;
    private Long stampId;
    /** 可用数量 */
    private Integer count;
    /** 已使用数量 */
    private Integer usedCount;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String stampName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String stampImageUrl;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String stampType;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String stampTheme;
}
