package com.aladdin.common.log.impl;

import com.aladdin.common.log.entity.LoginLog;
import com.aladdin.common.log.LoginLogService;
import com.alibaba.fastjson2.JSON;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis登录日志服务实现
 *
 * @author cles
 * @date 2026/05/06
 */
public class RedisLoginLogServiceImpl implements LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(RedisLoginLogServiceImpl.class);

    private static final String LOGIN_LOG_KEY = "ZM:log:login";

    private final StringRedisTemplate redisTemplate;

    public RedisLoginLogServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(LoginLog loginLog) {
        try {
            String value = JSON.toJSONString(loginLog);
            redisTemplate.opsForList().rightPush(LOGIN_LOG_KEY, value);
        } catch (Exception e) {
            log.warn("保存登录日志失败: {}", e.getMessage());
        }
    }
}
