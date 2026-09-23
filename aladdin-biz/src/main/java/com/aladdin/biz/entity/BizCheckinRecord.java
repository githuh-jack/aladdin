package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 每日签到/登录记录
 *
 * @author cles
 * @date 2026/09/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_checkin_record")
public class BizCheckinRecord extends BaseEntity {

    private Long userId;
    /** 签到日期 */
    private LocalDate checkinDate;
    /** 本次获得铜钱 */
    private Integer coin;
    /** 连续签到天数 */
    private Integer streakDays;
}
