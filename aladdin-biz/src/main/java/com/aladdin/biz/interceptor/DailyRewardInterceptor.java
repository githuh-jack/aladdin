package com.aladdin.biz.interceptor;

import com.aladdin.biz.service.RewardService;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 每日签到拦截器：登录用户当日首个业务请求时触发签到与每日登录奖励
 * (每日限1次，连续签到里程碑奖励见 RewardService)
 *
 * @author cles
 * @date 2026/09/20
 */
@Component
public class DailyRewardInterceptor implements HandlerInterceptor {

    @Autowired
    private RewardService rewardService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            Long userId = LoginService.getCurrentUserId();
            if (userId != null) {
                rewardService.doDailyCheckin(userId);
            }
        } catch (Exception ignored) {
            // 签到失败不影响正常请求
        }
        return true;
    }
}
