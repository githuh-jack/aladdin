package com.aladdin.common.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;

    private Integer userId;

    private String userName;

    private String operationName;

    private String operationType;

    private String requestMethod;

    private String requestUrl;

    private String requestIp;

    private String requestParam;

    private String responseData;

    private Long costTime;

    private Integer code;

    private String message;

    private String sessionId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}
