package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户系统邮件(每用户一份，读取时领取附件，幂等)
 *
 * @author cles
 * @date 2026/09/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_user_mail")
public class BizUserMail extends BaseEntity {

    /** 系统邮件ID */
    private Long mailId;
    /** 接收用户ID */
    private Long userId;
    /** 0未领取 1已领取 */
    private Integer claimed;
    /** 领取时间 */
    private LocalDateTime claimTime;

    @Column(ignore = true)
    private String title;
    @Column(ignore = true)
    private String content;
    @Column(ignore = true)
    private Integer coinAmount;
    @Column(ignore = true)
    private Long stampId;
    @Column(ignore = true)
    private Long envelopeId;
    @Column(ignore = true)
    private String stampName;
    @Column(ignore = true)
    private String envelopeName;
    @Column(ignore = true)
    private LocalDateTime sendTime;
}
