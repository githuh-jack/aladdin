package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizInviteRecordDao;
import com.aladdin.biz.dao.BizUserProfileDao;
import com.aladdin.biz.entity.BizUserProfile;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.security.service.LoginService;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邀请码控制器（用户端）
 *
 * @author cles
 * @date 2026/09/18
 */
@RestController
@RequestMapping({"/invite", "/biz/invite"})
public class InviteController {

    /** 邀请成功奖励邀请人的信用分 */
    private static final int INVITE_REWARD_CREDIT = 10;

    @Autowired
    private BizUserProfileDao profileDao;

    @Autowired
    private BizInviteRecordDao inviteRecordDao;

    /**
     * 校验邀请码是否有效（注册页游客可调，白名单放行）
     * 返回：valid 是否有效、inviterName 邀请人笔名
     */
    @GetMapping("/check")
    public R<Map<String, Object>> check(@RequestParam("code") String code) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        if (code == null || !code.trim().toUpperCase().matches("[A-Z0-9]{4,16}")) {
            return R.ok(result);
        }
        BizUserProfile owner = profileDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("invite_code").eq(code.trim().toUpperCase())));
        if (owner != null) {
            result.put("valid", true);
            result.put("inviterName", getNickname(owner.getUserId()));
        }
        return R.ok(result);
    }

    /** 我的邀请记录（我邀请了谁、获得多少信用分奖励） */
    @GetMapping("/my")
    public R<Map<String, Object>> my() {
        Long userId = LoginService.getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> records = inviteRecordDao.selectByInviter(userId == null ? -1L : userId);
        int totalReward = records.stream()
                .mapToInt(r -> r.get("rewardCoins") == null ? 0 : Integer.parseInt(String.valueOf(r.get("rewardCoins"))))
                .sum();
        result.put("records", records);
        result.put("totalReward", totalReward);
        return R.ok(result);
    }

    private String getNickname(Long userId) {
        Map<String, Object> row = inviteRecordDao.selectNicknameByUserId(userId);
        return row == null || row.get("nickname") == null ? "" : String.valueOf(row.get("nickname"));
    }
}
