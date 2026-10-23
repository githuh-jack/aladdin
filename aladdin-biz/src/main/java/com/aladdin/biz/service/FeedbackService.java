package com.aladdin.biz.service;

import com.aladdin.biz.dao.BizFeedbackDao;
import com.aladdin.biz.dao.BizSysMailDao;
import com.aladdin.biz.dao.BizUserMailDao;
import com.aladdin.biz.entity.BizFeedback;
import com.aladdin.biz.entity.BizSysMail;
import com.aladdin.biz.entity.BizUserMail;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 问题反馈服务：后台处理反馈，回复并可选附赠铜钱(以系统邮件发送给反馈人，信箱领取)
 *
 * @author cles
 * @date 2026/10/08
 */
@Service
public class FeedbackService {

    @Autowired
    private BizFeedbackDao feedbackDao;

    @Autowired
    private BizSysMailDao sysMailDao;

    @Autowired
    private BizUserMailDao userMailDao;

    /**
     * 处理反馈：写回复+发系统邮件(附赠铜钱随邮件领取)，幂等(已处理不可重复处理)
     *
     * @return 结果提示
     */
    @Transactional
    public String handle(Long adminId, Long id, String reply, Integer coinAmount) {
        BizFeedback fb = feedbackDao.selectOneById(id);
        if (fb == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (fb.getStatus() != null && fb.getStatus() == 1) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该反馈已处理");
        }
        int gift = coinAmount == null || coinAmount < 0 ? 0 : coinAmount;
        LocalDateTime now = LocalDateTime.now();
        // 系统邮件通知反馈人(铜钱随邮件领取)
        BizSysMail mail = new BizSysMail();
        mail.setTitle("您的反馈已处理");
        StringBuilder content = new StringBuilder("感谢您的反馈！");
        if (reply != null && !reply.trim().isEmpty()) {
            content.append("回复：").append(reply.trim());
        }
        if (gift > 0) {
            content.append("\n随信附上铜钱×").append(gift).append("，请查收。");
        }
        mail.setContent(content.toString());
        mail.setArrivalTime(now);
        mail.setCoinAmount(gift > 0 ? gift : null);
        mail.setSenderId(adminId);
        mail.setSys001(now);
        mail.setSys005(1);
        mail.setSys006("biz");
        sysMailDao.insert(mail);
        BizUserMail um = new BizUserMail();
        um.setMailId(mail.getId());
        um.setUserId(fb.getUserId());
        um.setClaimed(0);
        um.setSys001(now);
        um.setSys005(1);
        um.setSys006("biz");
        userMailDao.insert(um);
        // 更新反馈状态
        BizFeedback update = new BizFeedback();
        update.setId(id);
        update.setReply(reply);
        update.setStatus(1);
        update.setCoinAmount(gift);
        update.setAdminId(adminId);
        update.setHandleTime(now);
        update.setSys002(now);
        feedbackDao.update(update);
        return gift > 0 ? "已处理，系统邮件已发送并附赠铜钱×" + gift : "已处理，系统邮件已发送";
    }
}
