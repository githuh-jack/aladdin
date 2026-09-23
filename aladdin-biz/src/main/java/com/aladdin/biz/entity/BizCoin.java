package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户铜钱账户
 *
 * @author cles
 * @date 2026/09/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_coin")
public class BizCoin extends BaseEntity {

    private Long userId;
    /** 当前铜钱余额 */
    private Integer balance;
}
