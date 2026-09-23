package com.aladdin.common.log.impl;

import com.aladdin.common.core.context.UserContextHolder;
import com.aladdin.common.log.entity.OperationLog;
import com.aladdin.common.log.OperationLogService;
import com.alibaba.fastjson2.JSON;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis操作日志服务实现
 *
 * @author cles
 * @date 2026/05/06
 */
public class RedisOperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(RedisOperationLogServiceImpl.class);

    private static final String OPERATION_LOG_KEY = "ZM:log:operation";

    private final StringRedisTemplate redisTemplate;

    public RedisOperationLogServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(OperationLog operationLog) {
        try {
            String value = JSON.toJSONString(operationLog);
            redisTemplate.opsForList().rightPush(OPERATION_LOG_KEY, value);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
