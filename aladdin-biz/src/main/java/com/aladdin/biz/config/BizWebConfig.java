package com.aladdin.biz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 * 原每日签到拦截器(DailyRewardInterceptor)已移除，签到改为前端显式调用 /checkin/submit(#11)
 *
 * @author cles
 * @date 2026/09/20
 */
@Configuration
public class BizWebConfig implements WebMvcConfigurer {
}
