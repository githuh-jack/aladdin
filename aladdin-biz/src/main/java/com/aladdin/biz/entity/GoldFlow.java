package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 历史投资金额（可编辑）
 *
 * @author cles
 * @date 2026/11/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_gold_flow")
public class GoldFlow extends BaseEntity {

    private Long userId;
    /** 投入日期 */
    private LocalDate flowDate;
    /** 投入金额(元)，0=当日不投入 */
    private BigDecimal amount;
    private String memo;
}
