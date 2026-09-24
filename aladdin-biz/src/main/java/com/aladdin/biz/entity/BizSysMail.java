package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统邮件(管理员发送，可附赠铜钱/邮票/信封)
 *
 * @author cles
 * @date 2026/09/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_sys_mail")
public class BizSysMail extends BaseEntity {

    /** 邮件标题 */
    private String title;
    /** 邮件内容 */
    private String content;
    /** 附赠铜钱数(0为无) */
    private Integer coinAmount;
    /** 附赠邮票ID */
    private Long stampId;
    /** 附赠信封ID */
    private Long envelopeId;
    /** 发送管理员ID */
    private Long senderId;
}
