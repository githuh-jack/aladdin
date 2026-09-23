package com.aladdin.biz.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户登录日志(后台查看)
 *
 * @author cles
 * @date 2026/09/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("biz_login_log")
public class BizLoginLog extends BaseEntity {

    private Long userId;
    private String userName;
    private String loginName;
    private String loginIp;
    private String loginLocation;
    private String loginType;
    private Integer code;
    private String message;
    /** 登录时间 */
    private LocalDateTime loginTime;
}
