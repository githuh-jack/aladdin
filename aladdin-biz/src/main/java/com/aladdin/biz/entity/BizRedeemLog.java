package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 兑换记录(仅后台查看)
 *
 * @author cles
 * @date 2026/09/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_redeem_log")
public class BizRedeemLog extends BaseEntity {

    /** 兑换码ID */
    private Long codeId;
    /** 兑换码(快照) */
    private String code;
    /** 兑换用户ID */
    private Long userId;
    /** 兑换铜钱数 */
    private Integer coinAmount;
    /** 获赠邮票ID(快照,空=无) */
    private Long stampId;

    @Column(ignore = true)
    private String userName;

    @Column(ignore = true)
    private Long userNo;

    @Column(ignore = true)
    private String stampName;
}
