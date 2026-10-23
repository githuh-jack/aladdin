package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 问题反馈(用户提交，后台处理，处理时可附赠铜钱并邮件通知)
 *
 * @author cles
 * @date 2026/10/08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_feedback")
public class BizFeedback extends BaseEntity {

    /** 反馈用户ID */
    private Long userId;
    /** 反馈内容 */
    private String content;
    /** 联系方式(选填) */
    private String contact;
    /** 处理回复 */
    private String reply;
    /** 0待处理 1已处理 */
    private Integer status;
    /** 赠送铜钱数(0=未赠送) */
    private Integer coinAmount;
    /** 处理管理员ID */
    private Long adminId;
    /** 处理时间 */
    private LocalDateTime handleTime;

    @Column(ignore = true)
    private Long userNo;
    @Column(ignore = true)
    private String userName;
    @Column(ignore = true)
    private String adminName;
}
