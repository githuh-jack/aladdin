package com.aladdin.biz.config;

import com.aladdin.biz.interceptor.DailyRewardInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置：注册每日签到拦截器
 *
 * @author cles
 * @date 2026/09/20
 */
@Configuration
public class BizWebConfig implements WebMvcConfigurer {

    @Autowired
    private DailyRewardInterceptor dailyRewardInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 仅拦截业务请求，避免合并运行时签到逻辑波及 /auth、/system 接口
        registry.addInterceptor(dailyRewardInterceptor).addPathPatterns("/biz/**");
    }
}
