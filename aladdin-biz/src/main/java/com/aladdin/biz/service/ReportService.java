package com.aladdin.biz.service;

import com.aladdin.biz.dao.BizDiaryDao;
import com.aladdin.biz.dao.BizLetterDao;
import com.aladdin.biz.dao.BizMedalDao;
import com.aladdin.biz.dao.BizReportDao;
import com.aladdin.biz.dao.BizStampDao;
import com.aladdin.biz.dao.BizSysMailDao;
import com.aladdin.biz.dao.BizUserDao;
import com.aladdin.biz.dao.BizUserMailDao;
import com.aladdin.biz.dao.BizUserMedalDao;
import com.aladdin.biz.entity.BizDiary;
import com.aladdin.biz.entity.BizLetter;
import com.aladdin.biz.entity.BizMedal;
import com.aladdin.biz.entity.BizReport;
import com.aladdin.biz.entity.BizStamp;
import com.aladdin.biz.entity.BizSysMail;
import com.aladdin.biz.entity.BizUser;
import com.aladdin.biz.entity.BizUserMail;
import com.aladdin.biz.entity.BizUserMedal;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信件举报服务
 * 用户：收信后举报(多选项+其他说明)
 * 管理员：处理举报——分别向举报者/被举报者发系统邮件回复；
 *        奖励举报者(铜钱/邮票随邮件领取，信用分/奖章即时发放)；
 *        处罚被举报者(扣信用分即时生效，禁闭天数待其领取处罚邮件时起算)
 *
 * @author cles
 * @date 2026/10/14
 */
@Service
public class ReportService {

    /** 举报选项白名单 */
    public static final List<String> CATEGORIES =
            List.of("垃圾广告", "辱骂攻击", "色情低俗", "违法违规", "诈骗钓鱼", "泄露隐私", "其他");

    @Autowired
    private BizReportDao reportDao;

    @Autowired
    private BizLetterDao letterDao;

    @Autowired
    private BizDiaryDao diaryDao;

    @Autowired
    private BizUserDao userDao;

    @Autowired
    private BizStampDao stampDao;

    @Autowired
    private BizMedalDao medalDao;

    @Autowired
    private BizUserMedalDao userMedalDao;

    @Autowired
    private BizSysMailDao sysMailDao;

    @Autowired
    private BizUserMailDao userMailDao;

    @Autowired
    private RewardService rewardService;

    /**
     * 用户举报信件(仅收件人，信件送达后)
     *
     * @return 举报单
     */
    @Transactional(rollbackFor = Exception.class)
    public BizReport submit(Long userId, Long letterId, String category, String description) {
        // 校验举报类型与说明(信件/日记通用规则)
        String desc = checkCategoryAndDescription(category, description);
        if (letterId == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "缺少被举报信件");
        }
        BizLetter letter = letterDao.selectOneById(letterId);
        if (letter == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (!userId.equals(letter.getReceiverId())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "只有收件人可以举报该信件");
        }
        if (letter.getSenderId() == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该信件没有寄件人，无需举报");
        }
        if (letter.getArrivalTime() == null || letter.getArrivalTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "信件还在路上，送达后才能举报");
        }
        if (Integer.valueOf(4).equals(letter.getStatus())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该信件已退回寄件人，无需举报");
        }
        if (reportDao.countByTargetAndReporter(TARGET_LETTER, letterId, userId) > 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该信件您已举报过，请等待处理结果");
        }
        return insertReport(userId, letterId, TARGET_LETTER, letter.getSenderId(), category, desc);
    }

    /**
     * 用户举报日记(仅公开日记，且不能举报自己的日记)
     *
     * @param userId      举报人ID
     * @param diaryId     被举报日记ID
     * @param category    举报类型(白名单)
     * @param description 补充说明(选"其他"时必填)
     * @return 举报单
     */
    @Transactional(rollbackFor = Exception.class)
    public BizReport submitDiary(Long userId, Long diaryId, String category, String description) {
        // 校验举报类型与说明(信件/日记通用规则)
        String desc = checkCategoryAndDescription(category, description);
        if (diaryId == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "缺少被举报日记");
        }
        BizDiary diary = diaryDao.selectOneById(diaryId);
        if (diary == null || !Integer.valueOf(1).equals(diary.getSys005())) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        // 仅公开日记可被举报
        if (!Integer.valueOf(1).equals(diary.getIsPublic())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "仅公开日记可以举报");
        }
        // 举报者不能是作者本人
        if (diary.getUserId() == null || userId.equals(diary.getUserId())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "不能举报自己的日记");
        }
        if (reportDao.countByTargetAndReporter(TARGET_DIARY, diaryId, userId) > 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该日记您已举报过，请等待处理结果");
        }
        return insertReport(userId, diaryId, TARGET_DIARY, diary.getUserId(), category, desc);
    }

    /** 举报目标类型：信件 */
    public static final String TARGET_LETTER = "letter";
    /** 举报目标类型：日记 */
    public static final String TARGET_DIARY = "diary";

    /**
     * 校验举报类型白名单与说明长度(信件/日记通用)
     *
     * @return trim后的说明文本
     */
    private String checkCategoryAndDescription(String category, String description) {
        if (category == null || !CATEGORIES.contains(category.trim())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "请选择正确的举报类型");
        }
        String desc = description == null ? "" : description.trim();
        if (desc.length() > 500) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "说明内容不能超过500字");
        }
        if ("其他".equals(category.trim()) && desc.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "选择\"其他\"时请填写补充说明");
        }
        return desc;
    }

    /** 落库通用字段组装(targetType=letter/diary，targetId复用letter_id列) */
    private BizReport insertReport(Long reporterId, Long targetId, String targetType,
                                   Long reportedId, String category, String desc) {
        LocalDateTime now = LocalDateTime.now();
        BizReport report = new BizReport();
        report.setLetterId(targetId);
        report.setTargetType(targetType);
        report.setReporterId(reporterId);
        report.setReportedId(reportedId);
        report.setCategory(category.trim());
        report.setDescription(desc);
        report.setStatus(0);
        report.setRewardCoins(0);
        report.setRewardCredit(0);
        report.setConfineDays(0);
        report.setConfineStarted(0);
        report.setDeductCredit(0);
        report.setSys001(now);
        report.setSys005(1);
        report.setSys006("biz");
        reportDao.insert(report);
        return report;
    }

    /**
     * 管理员处理举报(幂等：已处理不可重复处理)
     *
     * @param adminId         管理员ID
     * @param reportId        举报ID
     * @param replyToReporter 发给举报者的回复内容(可空)
     * @param replyToReported 发给被举报者的回复内容(可空)
     * @param rewardCoins     奖励举报者铜钱(随回复邮件领取)
     * @param rewardStampId   奖励举报者邮票ID(随回复邮件领取，可空)
     * @param rewardCredit    奖励举报者信用分(即时)
     * @param rewardMedalId   奖励举报者奖章ID(即时，可空)
     * @param confineDays     被举报者禁闭天数(0=不禁闭；从其领取处罚邮件起算)
     * @param deductCredit    被举报者扣减信用分(即时)
     * @param handleRemark    处理备注(可空)
     * @return 结果提示
     */
    @Transactional(rollbackFor = Exception.class)
    public String handle(Long adminId, Long reportId, String replyToReporter, String replyToReported,
                         int rewardCoins, Long rewardStampId, int rewardCredit, Long rewardMedalId,
                         int confineDays, int deductCredit, String handleRemark) {
        BizReport report = reportDao.selectOneById(reportId);
        if (report == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (report.getStatus() != null && report.getStatus() == 1) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "该举报已处理");
        }
        if (rewardCoins < 0) rewardCoins = 0;
        if (rewardCredit < 0) rewardCredit = 0;
        if (confineDays < 0) confineDays = 0;
        if (deductCredit < 0) deductCredit = 0;
        BizStamp rewardStamp = null;
        if (rewardStampId != null) {
            rewardStamp = stampDao.selectOneById(rewardStampId);
            if (rewardStamp == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "奖励邮票不存在");
            }
        }
        BizMedal rewardMedal = null;
        if (rewardMedalId != null) {
            rewardMedal = medalDao.selectOneById(rewardMedalId);
            if (rewardMedal == null) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "奖励的奖章不存在");
            }
        }
        String reply1 = replyToReporter == null ? "" : replyToReporter.trim();
        String reply2 = replyToReported == null ? "" : replyToReported.trim();
        if (reply1.length() > 1000 || reply2.length() > 1000) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "回复内容不能超过1000字");
        }
        LocalDateTime now = LocalDateTime.now();

        // ===== 1. 举报者回复邮件(铜钱/邮票随信附赠，领取时自动发放) =====
        StringBuilder c1 = new StringBuilder("您举报的内容已核实处理，感谢您对纸船信箱的守护！");
        if (!reply1.isEmpty()) {
            c1.append("\n管理员回复：").append(reply1);
        }
        if (rewardCoins > 0) {
            c1.append("\n奖励铜钱×").append(rewardCoins);
        }
        if (rewardStamp != null) {
            c1.append("\n奖励邮票「").append(rewardStamp.getName()).append("」一枚");
        }
        if (rewardCredit > 0) {
            c1.append("\n奖励信用分+").append(rewardCredit);
        }
        if (rewardMedal != null) {
            c1.append("\n授予奖章「").append(rewardMedal.getName()).append("」，可在用户中心查看");
        }
        c1.append("\n附赠奖励请打开邮件后领取。");
        Long reporterMailId = sendMail(adminId, report.getReporterId(), "您的举报已处理", c1.toString(),
                rewardCoins > 0 ? rewardCoins : null, rewardStampId);

        // ===== 2. 举报者即时奖励：信用分、奖章 =====
        if (rewardCredit > 0) {
            rewardService.adjustCredit(report.getReporterId(), rewardCredit, "举报奖励");
        }
        if (rewardMedal != null) {
            grantMedal(report.getReporterId(), rewardMedalId);
        }

        // ===== 3. 被举报者处罚通知邮件 =====
        boolean punish = confineDays > 0 || deductCredit > 0;
        Long reportedMailId = null;
        if (punish) {
            StringBuilder c2 = new StringBuilder("经核实，您寄出的信件违反了《传信纸船信箱公约》，现对您作出如下处罚：");
            if (confineDays > 0) {
                c2.append("\n· 禁闭 ").append(confineDays).append(" 天(自您阅读本邮件起算，期间不能寄出信件)");
            }
            if (deductCredit > 0) {
                c2.append("\n· 扣减信用分 ").append(deductCredit).append(" 分(已生效)");
            }
            if (!reply2.isEmpty()) {
                c2.append("\n管理员回复：").append(reply2);
            }
            c2.append("\n请遵守信箱公约，共同维护纸船信箱的美好。");
            reportedMailId = sendMail(adminId, report.getReportedId(), "处罚通知", c2.toString(), null, null);
        }

        // ===== 4. 被举报者即时处罚：扣信用分；禁闭天数登记(起算时间待领取邮件时写入) =====
        if (deductCredit > 0) {
            rewardService.adjustCredit(report.getReportedId(), -deductCredit, "举报处罚扣减");
        }
        if (confineDays > 0) {
            userDao.resetConfine(report.getReportedId(), confineDays);
        }

        // ===== 5. 被举报目标标记异常(日记置abnormal，信件置abnormal，均不再上广场) =====
        if (TARGET_DIARY.equals(report.getTargetType())) {
            // 日记举报：标记日记异常
            BizDiary diaryUpdate = new BizDiary();
            diaryUpdate.setId(report.getLetterId());
            diaryUpdate.setAbnormal(1);
            diaryDao.update(diaryUpdate);
        } else {
            // 信件举报(含历史数据)：标记信件异常
            BizLetter letterUpdate = new BizLetter();
            letterUpdate.setId(report.getLetterId());
            letterUpdate.setAbnormal(1);
            letterDao.update(letterUpdate);
        }

        // ===== 6. 回填举报单 =====
        BizReport update = new BizReport();
        update.setId(reportId);
        update.setStatus(1);
        update.setRewardCoins(rewardCoins);
        update.setRewardStampId(rewardStampId);
        update.setRewardCredit(rewardCredit);
        update.setRewardMedalId(rewardMedalId);
        update.setConfineDays(confineDays);
        update.setDeductCredit(deductCredit);
        update.setReporterMailId(reporterMailId);
        update.setReportedMailId(reportedMailId);
        update.setAdminId(adminId);
        update.setHandleTime(now);
        update.setHandleRemark(handleRemark == null ? "" : handleRemark.trim());
        update.setSys002(now);
        reportDao.update(update);
        return "已处理" + (punish ? "，处罚通知已发送" : "") + "，回复邮件已分别发送给双方";
    }

    /**
     * 用户查看/领取邮件时触发：若为处罚通知且禁闭尚未起算 → 写入禁闭起算时间(幂等)
     */
    public void onPunishMailViewed(Long userId, Long mailId) {
        if (userId == null || mailId == null) {
            return;
        }
        BizReport report = reportDao.selectByReportedMailId(mailId);
        if (report == null || !userId.equals(report.getReportedId())) {
            return;
        }
        if (report.getConfineDays() == null || report.getConfineDays() <= 0) {
            return;
        }
        if (report.getConfineStarted() != null && report.getConfineStarted() == 1) {
            return;
        }
        // 标记已起算(防并发重复)再写用户禁闭起点
        BizReport mark = new BizReport();
        mark.setId(report.getId());
        mark.setConfineStarted(1);
        if (reportDao.update(mark) > 0) {
            BizUser userUpdate = new BizUser();
            userUpdate.setId(userId);
            userUpdate.setConfineStart(LocalDateTime.now());
            userDao.update(userUpdate);
        }
    }

    /** 发送定向系统邮件(1封sys_mail+1条user_mail) */
    private Long sendMail(Long adminId, Long toUserId, String title, String content,
                          Integer coinAmount, Long stampId) {
        LocalDateTime now = LocalDateTime.now();
        BizSysMail mail = new BizSysMail();
        mail.setTitle(title);
        mail.setContent(content);
        mail.setArrivalTime(now);
        mail.setCoinAmount(coinAmount);
        mail.setStampId(stampId);
        mail.setSenderId(adminId);
        mail.setSys001(now);
        mail.setSys005(1);
        mail.setSys006("biz");
        sysMailDao.insert(mail);
        BizUserMail um = new BizUserMail();
        um.setMailId(mail.getId());
        um.setUserId(toUserId);
        um.setClaimed(0);
        um.setSys001(now);
        um.setSys005(1);
        um.setSys006("biz");
        userMailDao.insert(um);
        return mail.getId();
    }

    /** 发放奖章(已拥有则跳过，唯一键兜底) */
    private void grantMedal(Long userId, Long medalId) {
        if (userMedalDao.countByUserAndMedal(userId, medalId) > 0) {
            return;
        }
        BizUserMedal um = new BizUserMedal();
        um.setUserId(userId);
        um.setMedalId(medalId);
        um.setSource("report_reward");
        um.setSys001(LocalDateTime.now());
        um.setSys005(1);
        um.setSys006("biz");
        try {
            userMedalDao.insert(um);
        } catch (Exception ignored) {
            // 并发下唯一键冲突视为已拥有
        }
    }
}
