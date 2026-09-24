package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizAvatarReviewDao;
import com.aladdin.biz.dao.BizCreditLogDao;
import com.aladdin.biz.dao.BizInviteRecordDao;
import com.aladdin.biz.dao.BizUserProfileDao;
import com.aladdin.biz.entity.BizAvatarReview;
import com.aladdin.biz.entity.BizCreditLog;
import com.aladdin.biz.entity.BizInviteRecord;
import com.aladdin.biz.entity.BizUserProfile;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户资料控制器（邀请码/性别/地区/年龄/信用分/头像/个性签名）
 *
 * @author cles
 * @date 2026/09/17
 */
@RestController
@RequestMapping({"/profile", "/biz/profile"})
public class ProfileController {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /** 头像允许的扩展名 */
    private static final java.util.Set<String> AVATAR_EXTS = java.util.Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    /** 头像文件名白名单（防路径穿越） */
    private static final java.util.regex.Pattern AVATAR_FILE_PATTERN = java.util.regex.Pattern.compile("^[a-f0-9]{32}\\.(jpg|jpeg|png|gif|webp|bmp)$");
    private static final long AVATAR_MAX_SIZE = 5 * 1024 * 1024;

    @Autowired
    private BizUserProfileDao profileDao;

    @Autowired
    private BizAvatarReviewDao avatarReviewDao;

    @Autowired
    private BizCreditLogDao creditLogDao;

    @Autowired
    private BizInviteRecordDao inviteRecordDao;

    @Autowired
    private com.aladdin.biz.service.ConfigService configService;

    @Autowired
    private com.aladdin.biz.service.RewardService rewardService;

    /** 邀请成功奖励邀请人的铜钱 */
    private static final int INVITE_REWARD_COINS = 30;

    /** 头像存储目录 */
    @Value("${biz.avatar-dir:./uploads/avatar}")
    private String avatarDir;

    /** 我的资料（首次访问自动初始化并生成邀请码） */
    @GetMapping("/me")
    public R<BizUserProfile> me() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizUserProfile profile = getByUserId(userId);
        if (profile == null) {
            profile = initProfile(userId);
        }
        return R.ok(profile);
    }

    /** 保存我的资料（邀请码由系统自动生成，头像只能通过上传并审核通过后生效） */
    @PostMapping("/save")
    public R<Void> save(@RequestBody BizUserProfile body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizUserProfile profile = getByUserId(userId);
        if (profile == null) {
            profile = initProfile(userId);
        }
        if (body.getGender() != null) {
            profile.setGender(body.getGender());
        }
        if (body.getRegion() != null) {
            profile.setRegion(body.getRegion().trim());
        }
        if (body.getRegionSecret() != null) {
            profile.setRegionSecret(body.getRegionSecret());
        }
        if (body.getAge() != null) {
            if (body.getAge() < 0 || body.getAge() > 120) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "年龄需在0-120之间");
            }
            profile.setAge(body.getAge());
        }
        if (body.getAgeSecret() != null) {
            profile.setAgeSecret(body.getAgeSecret());
        }
        if (body.getSignature() != null) {
            String sig = body.getSignature().trim();
            if (sig.length() > 100) {
                throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "个性签名最多100字");
            }
            profile.setSignature(sig);
        }
        // 邀请码/头像URL/头像状态/信用分不允许由此接口修改
        profile.setUserId(userId);
        profile.setSys002(LocalDateTime.now());
        profileDao.update(profile);
        // 完善资料一次性奖励
        checkProfileCompleteReward(profile);
        return R.ok();
    }

    /** 上传本地头像（点击上传，提交后进入后台审核，审核通过才生效） */
    @PostMapping("/avatar/upload")
    public R<BizAvatarReview> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "请选择图片文件");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "头像图片不能超过5MB");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !AVATAR_EXTS.contains(ext.toLowerCase())) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST.getCode(), "仅支持 jpg/jpeg/png/gif/webp/bmp 图片");
        }
        BizUserProfile profile = getByUserId(userId);
        if (profile == null) {
            profile = initProfile(userId);
        }
        // 保存文件
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
        try {
            Path dir = Paths.get(avatarDir);
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "头像保存失败");
        }
        // 生成审核记录并置为待审核
        BizAvatarReview review = new BizAvatarReview();
        review.setUserId(userId);
        review.setFileName(fileName);
        review.setImageUrl("/biz/avatar/" + fileName);
        review.setStatus(0);
        review.setRemark("");
        review.setSys001(LocalDateTime.now());
        review.setSys005(1);
        review.setSys006("biz");
        avatarReviewDao.insert(review);

        profile.setAvatarStatus(1);
        profile.setSys002(LocalDateTime.now());
        profileDao.update(profile);
        return R.ok(review);
    }

    /** 查看头像图片（游客可看，审核页面也会用到） */
    @GetMapping("/avatar/{fileName}")
    public ResponseEntity<FileSystemResource> avatar(@PathVariable("fileName") String fileName) {
        if (fileName == null || !AVATAR_FILE_PATTERN.matcher(fileName).matches()) {
            return ResponseEntity.badRequest().build();
        }
        Path path = Paths.get(avatarDir, fileName);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        MediaType type = switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "bmp" -> MediaType.parseMediaType("image/bmp");
            default -> MediaType.IMAGE_JPEG;
        };
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(path));
    }

    // ==================== 管理端 ====================

    /** 头像审核列表 */
    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<List<Map<String, Object>>> adminReviews(@RequestParam(required = false) Integer status) {
        return R.ok(avatarReviewDao.selectReviewsWithUser(status));
    }

    /** 审核头像（通过后头像生效，拒绝则保留原头像） */
    @PostMapping("/admin/review")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> adminReview(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Boolean pass = body.get("pass") == null ? null : Boolean.valueOf(String.valueOf(body.get("pass")));
        String remark = body.get("remark") == null ? "" : String.valueOf(body.get("remark"));
        if (id == null || pass == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST);
        }
        BizAvatarReview review = avatarReviewDao.selectOneById(id);
        if (review == null) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND);
        }
        review.setStatus(pass ? 1 : 2);
        review.setRemark(remark);
        review.setSys002(LocalDateTime.now());
        avatarReviewDao.update(review);

        BizUserProfile profile = getByUserId(review.getUserId());
        if (profile != null) {
            if (pass) {
                profile.setAvatarUrl(review.getImageUrl());
                profile.setAvatarStatus(2);
            } else {
                profile.setAvatarStatus(3);
            }
            profile.setSys002(LocalDateTime.now());
            profileDao.update(profile);
        }
        return R.ok();
    }

    /** 调整信用分（仅管理端，记录日志） */
    @PostMapping("/admin/credit")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> adminCredit(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId")));
        Integer changeVal = body.get("changeVal") == null ? null : Integer.valueOf(String.valueOf(body.get("changeVal")));
        String reason = body.get("reason") == null ? "" : String.valueOf(body.get("reason"));
        if (userId == null || changeVal == null || changeVal == 0) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST);
        }
        BizUserProfile profile = getByUserId(userId);
        if (profile == null) {
            profile = initProfile(userId);
        }
        int balance = Math.max(0, (profile.getCreditScore() == null ? 100 : profile.getCreditScore()) + changeVal);
        profile.setCreditScore(balance);
        profile.setSys002(LocalDateTime.now());
        profileDao.update(profile);
        addCreditLog(userId, changeVal, balance, reason.isEmpty() ? "管理员调整" : reason);
        return R.ok();
    }

    // ==================== 私有方法 ====================

    private BizUserProfile getByUserId(Long userId) {
        return profileDao.selectOneByQuery(QueryWrapper.create()
                .where(new QueryColumn("user_id").eq(userId)));
    }

    private BizUserProfile initProfile(Long userId) {
        BizUserProfile profile = new BizUserProfile();
        profile.setUserId(userId);
        profile.setInviteCode(generateUniqueCode());
        profile.setGender(0);
        profile.setRegion("");
        profile.setRegionSecret(1);
        profile.setAge(null);
        profile.setAgeSecret(1);
        profile.setSignature("");
        profile.setAvatarUrl("");
        profile.setAvatarStatus(0);
        profile.setCreditScore(100);
        profile.setRewardFlags(1);
        profile.setSys001(LocalDateTime.now());
        profile.setSys005(1);
        profile.setSys006("biz");
        profileDao.insert(profile);
        // 信用分初始记录
        addCreditLog(userId, 100, 100, "注册赠送100信用分");
        // 每个用户一株文竹
        rewardService.ensureBamboo(userId);
        // 注册时填写了邀请码，绑定邀请关系并奖励邀请人
        bindInviteIfNeeded(userId);
        return profile;
    }

    /**
     * 完善资料奖励(一次性)：性别已选、地区、年龄、签名已填且头像审核通过
     */
    private void checkProfileCompleteReward(BizUserProfile profile) {
        if (profile.getRewardFlags() == null) {
            profile.setRewardFlags(0);
        }
        if ((profile.getRewardFlags() & 2) != 0) {
            return;
        }
        boolean complete = profile.getGender() != null && profile.getGender() != 0
                && profile.getRegion() != null && !profile.getRegion().isEmpty()
                && profile.getAge() != null
                && profile.getSignature() != null && !profile.getSignature().isEmpty()
                && profile.getAvatarStatus() != null && profile.getAvatarStatus() == 2;
        if (!complete) {
            return;
        }
        int reward = configService.getInt("reward.profile_complete", 50);
        if (reward > 0) {
            rewardService.addCoins(profile.getUserId(), reward, "reward", "完善资料奖励");
        }
        profile.setRewardFlags(profile.getRewardFlags() | 2);
        profile.setSys002(LocalDateTime.now());
        profileDao.update(profile);
    }

    /**
     * 绑定邀请关系：注册时邀请码写入 biz_user.invite_code，
     * 被邀请人首次初始化资料时在此绑定并给邀请人发放信用分奖励
     */
    private void bindInviteIfNeeded(Long inviteeId) {
        try {
            String code = inviteRecordDao.selectInviteCodeByUser(inviteeId);
            if (code == null || code.trim().isEmpty()) {
                return;
            }
            BizUserProfile inviter = profileDao.selectOneByQuery(QueryWrapper.create()
                    .where(new QueryColumn("invite_code").eq(code.trim().toUpperCase())));
            if (inviter == null || inviter.getUserId().equals(inviteeId)) {
                return;
            }
            Long count = inviteRecordDao.selectCountByQuery(QueryWrapper.create()
                    .where(new QueryColumn("invitee_id").eq(inviteeId)));
            if (count != null && count > 0) {
                return;
            }
            BizInviteRecord record = new BizInviteRecord();
            record.setInviterId(inviter.getUserId());
            record.setInviteeId(inviteeId);
            record.setInviteCode(code.trim().toUpperCase());
            int rewardCoins = configService.getInt("reward.invite_register", INVITE_REWARD_COINS);
            record.setRewardCoins(rewardCoins);
            record.setSys001(LocalDateTime.now());
            record.setSys005(1);
            record.setSys006("biz");
            inviteRecordDao.insert(record);
            // 邀请人铜钱奖励 + 流水
            if (rewardCoins > 0) {
                rewardService.addCoins(inviter.getUserId(), rewardCoins, "reward", "邀请新用户注册奖励");
            }
        } catch (Exception ignored) {
            // 邀请绑定失败不影响资料初始化
        }
    }

    /** 记录信用分变动 */
    private void addCreditLog(Long userId, int changeVal, int balance, String reason) {
        BizCreditLog log = new BizCreditLog();
        log.setUserId(userId);
        log.setChangeVal(changeVal);
        log.setBalance(balance);
        log.setReason(reason);
        log.setSys001(LocalDateTime.now());
        log.setSys005(1);
        log.setSys006("biz");
        creditLogDao.insert(log);
    }

    /** 生成不重复的6位邀请码 */
    private String generateUniqueCode() {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (profileDao.selectOneByQuery(QueryWrapper.create()
                    .where(new QueryColumn("invite_code").eq(code))) == null) {
                return code;
            }
        }
        throw new BusinessException(GlobalErrorCode.INTERNAL_ERROR);
    }
}
