package com.aladdin.common.log.aspect;

import com.aladdin.common.core.annotation.OpLog;
import com.aladdin.common.core.context.UserContextHolder;
import com.aladdin.common.core.utils.IpUtil;
import com.aladdin.common.core.utils.ServletUtil;
import com.aladdin.common.log.entity.OperationLog;
import com.aladdin.common.log.OperationLogService;
import com.alibaba.fastjson2.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 操作日志切面
 *
 * @author cles
 * @date 2026/05/06
 */
@Aspect
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private OperationLogService operationLogService;

    public void setOperationLogService(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OpLog opLog) throws Throwable {
        LocalDateTime startTime = LocalDateTime.now();
        OperationLog logEntity = new OperationLog();
        logEntity.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        logEntity.setStartTime(startTime);
        logEntity.setOperationName(opLog.value());
        logEntity.setOperationType(opLog.type());

        Long userId = UserContextHolder.getUserId();
        String username = UserContextHolder.getUsername();
        if (userId != null) {
            logEntity.setUserId(userId.intValue());
            logEntity.setUserName(username);
        }

        HttpServletRequest request = ServletUtil.getRequest();
        if (request != null) {
            logEntity.setRequestUrl(request.getRequestURL().toString());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setRequestIp(IpUtil.getIpAddr(request));
        }

        MethodSignature signature = (MethodSignature) point.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = point.getArgs();
        if (paramNames != null && paramNames.length > 0) {
            try {
                StringBuilder sb = new StringBuilder("{");
                for (int i = 0; i < paramNames.length; i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append("\"").append(paramNames[i]).append("\":");
                    String jsonValue;
                    try {
                        jsonValue = JSON.toJSONString(paramValues[i]);
                    } catch (Exception e) {
                        jsonValue = String.valueOf(paramValues[i]);
                    }
                    if (jsonValue != null && jsonValue.length() > 500) {
                        jsonValue = jsonValue.substring(0, 500) + "...";
                    }
                    sb.append(jsonValue);
                }
                sb.append("}");
                logEntity.setRequestParam(sb.toString());
            } catch (Exception e) {
                logEntity.setRequestParam("parse error");
            }
        }

        Object result = null;
        try {
            result = point.proceed();
            logEntity.setCode(20000);
        } catch (Throwable e) {
            logEntity.setCode(50000);
            logEntity.setMessage(e.getMessage());
            throw e;
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            logEntity.setEndTime(endTime);
            logEntity.setCostTime(Duration.between(startTime, endTime).toMillis());

            if (result != null) {
                try {
                    String responseJson = JSON.toJSONString(result);
                    if (responseJson.length() > 2000) {
                        responseJson = responseJson.substring(0, 2000) + "...";
                    }
                    logEntity.setResponseData(responseJson);
                } catch (Exception e) {
                    logEntity.setResponseData("serialize error");
                }
            }

            saveLog(logEntity);
        }

        return result;
    }

    private void saveLog(OperationLog logEntity) {
        if (operationLogService != null) {
            try {
                operationLogService.save(logEntity);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", e.getMessage());
            }
        }
    }
}
