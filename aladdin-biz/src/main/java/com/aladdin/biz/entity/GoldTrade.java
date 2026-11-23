package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 黄金买入流水
 *
 * @author cles
 * @date 2026/11/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_gold_trade")
public class GoldTrade extends BaseEntity {

    private Long userId;
    private LocalDate tradeDate;
    /** GRAM/UNIT */
    private String buyMode;
    /** 成交价(元/克) */
    private BigDecimal price;
    /** 买入克数 */
    private BigDecimal grams;
    /** 买入金额 */
    private BigDecimal amount;
    /** 买入份数 */
    private BigDecimal units;
    /** 手续费 */
    private BigDecimal fee;
    /** AUTO/MANUAL */
    private String source;
    private String note;
}
