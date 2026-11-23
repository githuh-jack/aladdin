package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 黄金投资设置（每用户一条）
 *
 * @author cles
 * @date 2026/11/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_gold_setting")
public class GoldSetting extends BaseEntity {

    private Long userId;
    /** GRAM=按克数买, UNIT=按份数买 */
    private String buyMode;
    /** 每份金额(元/份) */
    private BigDecimal unitAmount;
    /** 每次买入克数 */
    private BigDecimal gramAmount;
    /** 每交易日投入金额 */
    private BigDecimal dailyAmount;
    /** 开始投资日期 */
    private LocalDate startDate;
    /** 初始本金 */
    private BigDecimal initialCapital;
    /** 手续费率 */
    private BigDecimal feeRate;
    /** 预期年化收益率 */
    private BigDecimal expectedAnnualRate;
    /** 预测持有天数 */
    private Integer horizonDays;
    /** 是否按日自动投入 */
    private Boolean autoDeposit;
}
