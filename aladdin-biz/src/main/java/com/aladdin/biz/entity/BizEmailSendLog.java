package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮箱验证码发送记录(每次SMTP发送一条，成功失败均记录)
 *
 * @author cles
 * @date 2026/09/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_email_send_log")
public class BizEmailSendLog extends BaseEntity {

    /** 收件邮箱 */
    private String email;
    /** 用途场景(register注册/bind绑定/reset重置密码等) */
    private String scene;
    /** 验证码 */
    private String code;
    /** 0失败 1发送成功 */
    private Integer status;
    /** 失败原因(发送失败时) */
    private String failReason;
    /** 发送时间 */
    private LocalDateTime sendTime;
    /** 验证码过期时间 */
    private LocalDateTime expireTime;
    /** 请求IP */
    private String clientIp;
}
