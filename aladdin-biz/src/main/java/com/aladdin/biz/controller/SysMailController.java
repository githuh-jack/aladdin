package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizEnvelopeDao;
import com.aladdin.biz.dao.BizStampDao;
import com.aladdin.biz.dao.BizSysMailDao;
import com.aladdin.biz.dao.BizUserDao;
import com.aladdin.biz.dao.BizUserEnvelopeItemDao;
import com.aladdin.biz.dao.BizUserMailDao;
import com.aladdin.biz.dao.BizUserStampItemDao;
import com.aladdin.biz.entity.BizSysMail;
import com.aladdin.biz.entity.BizUserEnvelopeItem;
import com.aladdin.biz.entity.BizUserMail;
import com.aladdin.biz.entity.BizUserStampItem;
import com.aladdin.biz.service.RewardService;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统邮件控制器
 * 用户：查看我的系统邮件、读取时领取附赠(铜钱/邮票/信封，幂等)
 * 管理员：给全体用户发送系统邮件、查看发送记录
 *
 * @author cles
 * @date 2026/09/24
 */
@RestController
@RequestMapping({"/mail", "/biz/mail"})
public class SysMailController {

    @Autowired
    private BizSysMailDao sysMailDao;

    @Autowired
    private BizUserMailDao userMailDao;

    @Autowired
    private BizUserDao userDao;

    @Autowired
    private BizStampDao stampDao;

    @Autowired
    private BizEnvelopeDao envelopeDao;

    @Autowired
    private BizUserStampItemDao userStampItemDao;

    @Autowired
    private BizUserEnvelopeItemDao userEnvelopeItemDao;

    @Autowired
    private RewardService rewardService;

    /** 我的系统邮件 */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(userMailDao.selectMyMails(userId));
    }

    /** 读取邮件时领取附赠(幂等，每人只领一次) */
    @PostMapping("/claim/{id}")
    public R<Void> claim(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizUserMail um = userMailDao.selectOneById(id);
        if (um == null || !userId.equals(um.getUserId())) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (um.getClaimed() != null && um.getClaimed() == 1) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该邮件奖励已领取");
        }
        // 先占领取标记，防并发重复发放
        if (userMailDao.markClaimed(id, userId) == 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该邮件奖励已领取");
        }
        BizSysMail mail = sysMailDao.selectOneById(um.getMailId());
        List<String> granted = new ArrayList<>();
        if (mail != null) {
            if (mail.getCoinAmount() != null && mail.getCoinAmount() > 0) {
                rewardService.addCoins(userId, mail.getCoinAmount(), "mail", "系统邮件附赠");
                granted.add("铜钱×" + mail.getCoinAmount());
            }
            if (mail.getStampId() != null) {
                grantStamp(userId, mail.getStampId());
                granted.add("邮票");
            }
            if (mail.getEnvelopeId() != null) {
                grantEnvelope(userId, mail.getEnvelopeId());
                granted.add("信封");
            }
        }
        return R.ok(granted.isEmpty() ? "已领取" : "已领取：" + String.join("、", granted), null);
    }

    // ===== 管理员接口 =====

    /** 发送记录(含送达/已领取人数) */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:mail:list') or hasRole('ROLE_admin')")
    public R<List<Map<String, Object>>> adminList() {
        return R.ok(sysMailDao.selectAdminList());
    }

    /**
     * 给全体用户发送系统邮件
     * body: {title, content, coinAmount?, stampId?, envelopeId?} 至少附赠一种
     */
    @PostMapping("/admin/send")
    @PreAuthorize("hasAuthority('biz:mail:send') or hasRole('ROLE_admin')")
    public R<Map<String, Object>> send(@RequestBody BizSysMail mail) {
        if (mail.getTitle() == null || mail.getTitle().trim().isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "邮件标题不能为空");
        }
        boolean hasCoin = mail.getCoinAmount() != null && mail.getCoinAmount() > 0;
        if (!hasCoin && mail.getStampId() == null && mail.getEnvelopeId() == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "请至少附赠铜钱、邮票或信封其中一种");
        }
        if (mail.getStampId() != null && stampDao.selectOneById(mail.getStampId()) == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "附赠的邮票不存在");
        }
        if (mail.getEnvelopeId() != null && envelopeDao.selectOneById(mail.getEnvelopeId()) == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "附赠的信封不存在");
        }
        mail.setSenderId(LoginService.getCurrentUserId());
        mail.setSys001(LocalDateTime.now());
        mail.setSys005(1);
        mail.setSys006("biz");
        sysMailDao.insert(mail);
        // 全量活跃用户各发一份
        List<Long> userIds = userDao.selectActiveUserIds();
        List<BizUserMail> rows = new ArrayList<>(userIds.size());
        for (Long uid : userIds) {
            BizUserMail um = new BizUserMail();
            um.setMailId(mail.getId());
            um.setUserId(uid);
            um.setClaimed(0);
            um.setSys001(LocalDateTime.now());
            um.setSys005(1);
            um.setSys006("biz");
            rows.add(um);
        }
        for (int i = 0; i < rows.size(); i += 500) {
            userMailDao.insertBatch(rows.subList(i, Math.min(i + 500, rows.size())));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("mailId", mail.getId());
        data.put("count", rows.size());
        return R.ok("发送成功", data);
    }

    /** 附赠邮票入库(一条实例，编码回填，不对外展示) */
    private void grantStamp(Long userId, Long stampId) {
        BizUserStampItem item = new BizUserStampItem();
        item.setUserId(userId);
        item.setStampId(stampId);
        item.setStatus(1);
        item.setSys001(LocalDateTime.now());
        item.setSys005(1);
        item.setSys006("biz");
        userStampItemDao.insert(item);
        userStampItemDao.fillStampCodes();
    }

    /** 附赠信封入库(一条实例，编码回填，不对外展示) */
    private void grantEnvelope(Long userId, Long envelopeId) {
        BizUserEnvelopeItem item = new BizUserEnvelopeItem();
        item.setUserId(userId);
        item.setEnvelopeId(envelopeId);
        item.setStatus(1);
        item.setSys001(LocalDateTime.now());
        item.setSys005(1);
        item.setSys006("biz");
        userEnvelopeItemDao.insert(item);
        userEnvelopeItemDao.fillEnvelopeCodes();
    }
}
