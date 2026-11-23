package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Au99.99 日行情
 *
 * @author cles
 * @date 2026/11/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_gold_price")
public class GoldPrice extends BaseEntity {

    /** 交易日 */
    private LocalDate tradeDate;
    /** 开盘价 */
    private BigDecimal openPrice;
    /** 收盘价/成交价 */
    private BigDecimal closePrice;
    /** 最低价 */
    private BigDecimal lowPrice;
    /** 最高价 */
    private BigDecimal highPrice;
    /** 数据来源 */
    private String source;
}
