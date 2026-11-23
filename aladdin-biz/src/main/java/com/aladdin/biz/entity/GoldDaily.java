package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 黄金投资每日快照
 *
 * @author cles
 * @date 2026/11/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_gold_daily")
public class GoldDaily extends BaseEntity {

    private Long userId;
    private LocalDate tradeDate;
    private BigDecimal price;
    private BigDecimal deposit;
    private BigDecimal buyAmount;
    private BigDecimal grams;
    private BigDecimal cumulativeGrams;
    private BigDecimal cumulativeInvested;
    private BigDecimal marketValue;
    private BigDecimal equity;
    private BigDecimal profit;
    private BigDecimal profitRate;
}
