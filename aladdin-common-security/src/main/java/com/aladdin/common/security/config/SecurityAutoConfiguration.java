package com.aladdin.common.security.config;

import com.aladdin.common.security.handler.SecurityExceptionHandler;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "aladdin.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class
})
public class SecurityAutoConfiguration {

    /**
     * 注册 BeanPostProcessor，将 MethodSecurityInterceptor 的 AccessDecisionManager
     * 替换为 AdminAwareAccessDecisionManager，使 admin 用户绕过所有 @PreAuthorize 校验。
     * 必须声明为 static，避免 Spring 提前实例化本配置类导致 @ConditionalOnBean 失效。
     */
    @Bean
    public static BeanPostProcessor methodSecurityInterceptorPostProcessor() {
        return new MethodSecurityInterceptorPostProcessor();
    }
}
