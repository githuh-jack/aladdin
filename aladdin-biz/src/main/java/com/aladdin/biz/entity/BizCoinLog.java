package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 铜钱流水
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_coin_log")
public class BizCoinLog extends BaseEntity {

    private Long userId;
    /** 变动数量(正负) */
    private Integer changeAmount;
    /** 变动后余额 */
    private Integer balanceAfter;
    /** recharge/shop/send_letter/admin_adjust */
    private String logType;
    /** 关联业务ID */
    private Long refId;
    private String remark;

    @com.mybatisflex.annotation.Column(ignore = true)
    private String userName;
}
