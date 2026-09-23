package com.aladdin.system.entity;

import com.aladdin.common.core.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录日志实体（sys_login_log）
 *
 * @author cles
 * @date 2026/09/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_login_log")
public class SysLoginLog extends BaseEntity {

    /** 链路追踪ID */
    private String traceId;

    /** 用户ID */
    private Long userId;

    /** 用户姓名 */
    private String userName;

    /** 登录账号 */
    private String loginName;

    /** 登录IP */
    private String loginIp;

    /** 登录地点 */
    private String loginLocation;

    /** 登录类型 */
    private String loginType;

    /** Token */
    private String token;

    /** 会话ID */
    private String sessionId;

    /** 登录结果码 */
    private Integer code;

    /** 登录结果消息 */
    private String message;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
