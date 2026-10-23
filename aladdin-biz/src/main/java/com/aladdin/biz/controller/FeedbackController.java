package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizFeedbackDao;
import com.aladdin.biz.entity.BizFeedback;
import com.aladdin.biz.service.FeedbackService;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 问题反馈控制器
 * 用户：提交反馈、查看我的反馈及处理结果
 * 管理员：反馈列表、处理反馈(回复+可选铜钱赠送，系统邮件通知)
 *
 * @author cles
 * @date 2026/10/08
 */
@RestController
@RequestMapping({"/feedback", "/biz/feedback"})
public class FeedbackController {

    @Autowired
    private BizFeedbackDao feedbackDao;

    @Autowired
    private FeedbackService feedbackService;

    /** 用户提交反馈 */
    @PostMapping("/submit")
    public R<Void> submit(@RequestBody Map<String, String> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        String content = body.get("content") == null ? "" : body.get("content").trim();
        if (content.isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请填写反馈内容");
        }
        if (content.length() > 1000) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "反馈内容不能超过1000字");
        }
        String contact = body.get("contact") == null ? "" : body.get("contact").trim();
        if (contact.length() > 50) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "联系方式不能超过50字");
        }
        BizFeedback fb = new BizFeedback();
        fb.setUserId(userId);
        fb.setContent(content);
        fb.setContact(contact);
        fb.setStatus(0);
        fb.setCoinAmount(0);
        fb.setSys001(LocalDateTime.now());
        fb.setSys005(1);
        fb.setSys006("biz");
        feedbackDao.insert(fb);
        return R.ok("反馈已提交，感谢您的意见", null);
    }

    /** 我的反馈列表(含处理结果) */
    @GetMapping("/mine")
    public R<List<BizFeedback>> mine() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(feedbackDao.selectMyList(userId));
    }

    // ===== 管理员接口 =====

    /** 管理员: 反馈列表(待处理在前) */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<PageResult<BizFeedback>> adminList(PageQuery pageQuery,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status) {
        List<BizFeedback> list = feedbackDao.selectAdminList(keyword, status,
                pageQuery.getOffset(), pageQuery.getLimit());
        long total = feedbackDao.countAdminList(keyword, status);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list));
    }

    /**
     * 管理员: 处理反馈
     * body: {reply?, coinAmount?} 铜钱大于0时随系统邮件赠送给反馈人
     */
    @PostMapping("/admin/handle/{id}")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long adminId = LoginService.getCurrentUserId();
        if (adminId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        String reply = body.get("reply") == null ? "" : body.get("reply").toString().trim();
        if (reply.length() > 1000) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "回复内容不能超过1000字");
        }
        Integer coinAmount = 0;
        Object coin = body.get("coinAmount");
        if (coin instanceof Number n) {
            coinAmount = n.intValue();
        } else if (coin != null) {
            try {
                coinAmount = Integer.parseInt(coin.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        String msg = feedbackService.handle(adminId, id, reply, coinAmount);
        return R.ok(msg, null);
    }
}
