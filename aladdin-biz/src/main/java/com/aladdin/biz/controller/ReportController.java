package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizReportDao;
import com.aladdin.biz.entity.BizReport;
import com.aladdin.biz.service.ReportService;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 信件举报控制器
 * 用户：收到信件后举报(多选项+其他说明)、查看我的举报及处理结果
 * 管理员：举报列表、处理举报(分别回复双方+奖励举报者+处罚被举报者)
 *
 * @author cles
 * @date 2026/10/14
 */
@RestController
@RequestMapping({"/report", "/biz/report"})
public class ReportController {

    @Autowired
    private BizReportDao reportDao;

    @Autowired
    private ReportService reportService;

    /** 用户举报信件 body: {letterId, category, description?} */
    @PostMapping("/letter")
    @Transactional(rollbackFor = Exception.class)
    public R<BizReport> reportLetter(@RequestBody Map<String, Object> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        Long letterId = parseLong(body.get("letterId"));
        String category = body.get("category") == null ? null : body.get("category").toString();
        String description = body.get("description") == null ? "" : body.get("description").toString();
        BizReport report = reportService.submit(userId, letterId, category, description);
        return R.ok("举报已提交，我们会尽快核实处理", report);
    }

    /** 我的举报列表(含处理结果) */
    @GetMapping("/mine")
    public R<List<BizReport>> mine() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(reportDao.selectMyList(userId));
    }

    // ===== 管理员接口 =====

    /** 管理员: 举报列表(待处理在前，含信件标题/双方信息) */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('app:paperBoat:list') or hasRole('ROLE_admin')")
    public R<PageResult<BizReport>> adminList(PageQuery pageQuery,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status) {
        List<BizReport> list = reportDao.selectAdminList(keyword, status,
                pageQuery.getOffset(), pageQuery.getLimit());
        long total = reportDao.countAdminList(keyword, status);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list));
    }

    /**
     * 管理员: 处理举报
     * body: {replyToReporter?, replyToReported?, rewardCoins?, rewardStampId?, rewardCredit?,
     *        rewardMedalId?, confineDays?, deductCredit?, handleRemark?}
     * 奖励：铜钱/邮票随回复邮件由举报者领取，信用分/奖章即时发放
     * 处罚：扣信用分即时生效；禁闭天数从被举报者领取处罚通知邮件起算
     */
    @PostMapping("/admin/handle/{id}")
    @PreAuthorize("hasRole('ROLE_admin')")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long adminId = LoginService.getCurrentUserId();
        if (adminId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        String msg = reportService.handle(adminId, id,
                str(body.get("replyToReporter")),
                str(body.get("replyToReported")),
                (int) longVal(body.get("rewardCoins"), 0),
                parseLong(body.get("rewardStampId")),
                (int) longVal(body.get("rewardCredit"), 0),
                parseLong(body.get("rewardMedalId")),
                (int) longVal(body.get("confineDays"), 0),
                (int) longVal(body.get("deductCredit"), 0),
                str(body.get("handleRemark")));
        return R.ok(msg, null);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long parseLong(Object o) {
        if (o == null || "".equals(o.toString())) {
            return null;
        }
        try {
            return Long.valueOf(o.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "参数格式不正确");
        }
    }

    private static long longVal(Object o, long def) {
        Long v = parseLong(o);
        return v == null ? def : v;
    }
}
