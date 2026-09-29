package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 兑换码配置(后台生成，用户兑换铜钱，每人每码限一次)
 *
 * @author cles
 * @date 2026/09/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_redeem_code")
public class BizRedeemCode extends BaseEntity {

    /** 兑换码 */
    private String code;
    /** 兑换铜钱数 */
    private Integer coinAmount;
    /** 可兑换总次数 */
    private Integer maxCount;
    /** 已兑换次数 */
    private Integer usedCount;
    /** 赠送邮票ID(空=仅铜钱) */
    private Long stampId;
    /** 0停用 1启用 */
    private Integer status;
}
