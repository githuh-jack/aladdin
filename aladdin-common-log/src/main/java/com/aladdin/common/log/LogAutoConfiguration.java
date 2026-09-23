package com.aladdin.common.log;

import com.aladdin.common.log.aspect.OperationLogAspect;
import com.aladdin.common.log.impl.RedisLoginLogServiceImpl;
import com.aladdin.common.log.impl.RedisOperationLogServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 日志模块自动配置：登录日志/操作日志默认使用 Redis 实现，
 * 业务侧存在自定义 LoginLogService/OperationLogService Bean 时不注册默认实现
 *
 * @author cles
 * @date 2026/09/23
 */
@Configuration
@ConditionalOnProperty(prefix = "aladdin.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LoginLogService.class)
    @ConditionalOnProperty(prefix = "aladdin.security", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public LoginLogService loginLogService(StringRedisTemplate redisTemplate) {
        return new RedisLoginLogServiceImpl(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(OperationLogService.class)
    @ConditionalOnProperty(prefix = "aladdin.security", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogService operationLogService(StringRedisTemplate redisTemplate) {
        return new RedisOperationLogServiceImpl(redisTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "aladdin.security", name = "oper-log-enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogAspect operationLogAspect(OperationLogService operationLogService) {
        OperationLogAspect aspect = new OperationLogAspect();
        aspect.setOperationLogService(operationLogService);
        return aspect;
    }
}
