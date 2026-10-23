package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户奖章
 *
 * @author cles
 * @date 2026/10/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_user_medal")
public class BizUserMedal extends BaseEntity {

    private Long userId;
    /** 奖章ID */
    private Long medalId;
    /** 来源(report_reward举报奖励) */
    private String source;

    // ===== 展示用冗余字段（不映射DB列） =====
    @com.mybatisflex.annotation.Column(ignore = true)
    private String medalName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String medalIcon;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String medalDescription;
}
