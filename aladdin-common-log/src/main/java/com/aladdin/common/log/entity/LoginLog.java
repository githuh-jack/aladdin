package com.aladdin.common.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;

    private Integer userId;

    private String userName;

    private String loginName;

    private String loginIp;

    private String loginLocation;

    private String loginType;

    private String token;

    private String sessionId;

    private Integer code;

    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime loginTime;
}
