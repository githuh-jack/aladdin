package com.aladdin.biz.controller;

import com.aladdin.biz.service.RewardService;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 每日签到控制器(点击签到，#11)
 * 原"用户首个请求自动签到"拦截器已移除，改为前端显式调用：
 * 进入签到页查状态(GET /checkin/status)，用户点击签到提交(POST /checkin/submit)
 *
 * @author cles
 * @date 2026/10/29
 */
@RestController
@RequestMapping({"/checkin", "/biz/checkin"})
public class CheckinController {

    @Autowired
    private RewardService rewardService;

    /** 签到状态 {todayChecked: 当日是否已签, streakDays: 连续签到天数} */
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(rewardService.getCheckinStatus(userId));
    }

    /**
     * 提交签到：当日已签返回400"今日已签到"；
     * 成功返回最新签到状态与本次奖励铜钱数(coinsGranted，含连续签到里程碑奖励)
     */
    @PostMapping("/submit")
    public R<Map<String, Object>> submit() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        int coinsGranted = rewardService.doDailyCheckin(userId);
        if (coinsGranted == 0 && Boolean.TRUE.equals(rewardService.getCheckinStatus(userId).get("todayChecked"))) {
            // 已签到(或并发下被抢先签到)：明确提示而非静默成功
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "今日已签到");
        }
        Map<String, Object> data = new HashMap<>();
        data.putAll(rewardService.getCheckinStatus(userId));
        data.put("coinsGranted", coinsGranted);
        return R.ok("签到成功", data);
    }
}
