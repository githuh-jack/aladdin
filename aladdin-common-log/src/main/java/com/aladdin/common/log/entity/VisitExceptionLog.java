package com.aladdin.common.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常日志
 *
 * @author cles
 * @date 2026/04/30
 */
@Data
public class VisitExceptionLog {

    private String sessionId;

    private String title;

    private String exception;

    private String exceptionDetail;

    private int code;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
