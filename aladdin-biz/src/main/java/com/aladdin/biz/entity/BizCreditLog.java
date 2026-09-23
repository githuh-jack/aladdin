package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 信用分变动记录
 *
 * @author cles
 * @date 2026/09/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_credit_log")
public class BizCreditLog extends BaseEntity {

    private Long userId;
    /** 变动值(正加负减) */
    private Integer changeVal;
    /** 变动后余额 */
    private Integer balance;
    /** 变动原因 */
    private String reason;
}
