package com.aladdin.common.security.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor;

/**
 * BeanPostProcessor：
 * 在 {@link MethodSecurityInterceptor} 初始化后，将其 AccessDecisionManager
 * 替换为 {@link AdminAwareAccessDecisionManager}，使 admin 用户绕过所有 @PreAuthorize 校验。
 *
 * @author cles
 */
public class MethodSecurityInterceptorPostProcessor implements BeanPostProcessor {

    public MethodSecurityInterceptorPostProcessor() {
        System.out.println("[AdminBypass] MethodSecurityInterceptorPostProcessor 已实例化");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MethodSecurityInterceptor) {
            MethodSecurityInterceptor interceptor = (MethodSecurityInterceptor) bean;
            System.out.println("[AdminBypass] 替换 MethodSecurityInterceptor 的 AccessDecisionManager, beanName=" + beanName);
            interceptor.setAccessDecisionManager(
                    new AdminAwareAccessDecisionManager(interceptor.getAccessDecisionManager()));
        }
        return bean;
    }
}
