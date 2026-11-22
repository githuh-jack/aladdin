package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 信件举报
 *
 * @author cles
 * @date 2026/10/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_report")
public class BizReport extends BaseEntity {

    /** 被举报目标ID(信件ID/日记ID，兼容历史列名 letter_id) */
    private Long letterId;
    /** 举报目标类型 letter=信件 diary=日记(历史数据默认letter) */
    private String targetType;
    /** 举报人ID(收件人) */
    private Long reporterId;
    /** 被举报人ID(信件发件人) */
    private Long reportedId;
    /** 举报选项(垃圾广告/辱骂攻击/色情低俗/违法违规/诈骗钓鱼/泄露隐私/其他) */
    private String category;
    /** 其他说明 */
    private String description;
    /** 0待处理 1已处理 */
    private Integer status;
    /** 奖励举报人铜钱数 */
    private Integer rewardCoins;
    /** 奖励举报人邮票ID */
    private Long rewardStampId;
    /** 奖励举报人信用分 */
    private Integer rewardCredit;
    /** 奖励举报人奖章ID */
    private Long rewardMedalId;
    /** 被举报人禁闭天数(0=不禁闭) */
    private Integer confineDays;
    /** 禁闭是否已起算(0待领取处罚邮件 1已起算) */
    private Integer confineStarted;
    /** 被举报人扣减信用分 */
    private Integer deductCredit;
    /** 发给举报人的回复邮件ID */
    private Long reporterMailId;
    /** 发给被举报人的处罚通知邮件ID */
    private Long reportedMailId;
    /** 处理管理员ID */
    private Long adminId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime handleTime;

    /** 处理备注 */
    private String handleRemark;

    // ===== 展示用冗余字段（不映射DB列） =====
    @com.mybatisflex.annotation.Column(ignore = true)
    private String letterTitle;
    /** 举报目标标题(按target_type取信件或日记标题) */
    @com.mybatisflex.annotation.Column(ignore = true)
    private String targetTitle;
    /** 举报目标内容(按target_type取信件或日记内容) */
    @com.mybatisflex.annotation.Column(ignore = true)
    private String targetContent;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String reporterName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private Long reporterNo;
    @com.mybatisflex.annotation.Column(ignore = true)
    private String reportedName;
    @com.mybatisflex.annotation.Column(ignore = true)
    private Long reportedNo;
}
