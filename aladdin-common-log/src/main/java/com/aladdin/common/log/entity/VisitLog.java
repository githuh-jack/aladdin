package com.aladdin.common.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志
 *
 * @author cles
 * @date 2026/04/30
 */
@Data
public class VisitLog {

    private String sessionId;

    private String requestId;

    private String requestUrl;

    private String methodName;

    private String requestUserName;

    private String requestIp;

    private String requestType;

    private String requestParam;

    private String responseData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private Long cost;

    private int code;

    private String message;
}
