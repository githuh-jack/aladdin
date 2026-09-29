package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统邮件(管理员发送，到达时间到点后进入用户信箱；历史版本支持附赠，新邮件不再附赠)
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
    /** 到达时间(到点后用户信箱可见) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime arrivalTime;
    /** 附赠铜钱数(历史数据) */
    private Integer coinAmount;
    /** 附赠邮票ID */
    private Long stampId;
    /** 附赠信封ID */
    private Long envelopeId;
    /** 发送管理员ID */
    private Long senderId;
}
