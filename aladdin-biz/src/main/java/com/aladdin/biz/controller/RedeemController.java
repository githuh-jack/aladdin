package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizRedeemCodeDao;
import com.aladdin.biz.dao.BizRedeemLogDao;
import com.aladdin.biz.dao.BizStampDao;
import com.aladdin.biz.entity.BizRedeemCode;
import com.aladdin.biz.entity.BizRedeemLog;
import com.aladdin.biz.service.RedeemService;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 兑换码控制器
 * 用户：输入兑换码兑换铜钱(每人每码限一次)
 * 管理员：兑换码 CRUD、查看兑换记录
 *
 * @author cles
 * @date 2026/09/29
 */
@RestController
@RequestMapping({"/redeem", "/biz/redeem"})
public class RedeemController {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Autowired
    private RedeemService redeemService;

    @Autowired
    private BizRedeemCodeDao redeemCodeDao;

    @Autowired
    private BizRedeemLogDao redeemLogDao;

    @Autowired
    private BizStampDao stampDao;

    /** 用户兑换 */
    @PostMapping("/exchange")
    public R<Map<String, Object>> exchange(@RequestBody Map<String, String> body) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        String code = body.get("code") == null ? "" : body.get("code").trim();
        if (code.isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "请输入兑换码");
        }
        return R.ok(redeemService.redeem(userId, code));
    }

    // ===== 管理员接口 =====

    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:redeem:list') or hasRole('ROLE_admin')")
    public R<List<BizRedeemCode>> adminList() {
        return R.ok(redeemCodeDao.selectAllList());
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasAuthority('biz:redeem:add') or hasRole('ROLE_admin')")
    public R<Void> adminAdd(@RequestBody BizRedeemCode req) {
        if (req.getCoinAmount() == null || req.getCoinAmount() <= 0) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "兑换铜钱数必须大于0");
        }
        if (req.getMaxCount() == null || req.getMaxCount() < 1) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "可兑换次数至少为1");
        }
        if (req.getStampId() != null && req.getStampId() > 0 && stampDao.selectOneById(req.getStampId()) == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "赠送的邮票不存在");
        }
        String code = req.getCode() == null ? "" : req.getCode().trim().toUpperCase();
        if (code.isEmpty()) {
            code = generateCode();
        } else if (code.length() > 32) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "兑换码长度不能超过32位");
        }
        if (redeemCodeDao.selectByCode(code) != null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "兑换码已存在");
        }
        BizRedeemCode entity = new BizRedeemCode();
        entity.setCode(code);
        entity.setCoinAmount(req.getCoinAmount());
        entity.setMaxCount(req.getMaxCount());
        entity.setUsedCount(0);
        entity.setStampId(req.getStampId() != null && req.getStampId() > 0 ? req.getStampId() : null);
        entity.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        entity.setSys001(LocalDateTime.now());
        entity.setSys005(1);
        entity.setSys006("biz");
        redeemCodeDao.insert(entity);
        return R.ok();
    }

    @PostMapping("/admin/edit")
    @PreAuthorize("hasAuthority('biz:redeem:edit') or hasRole('ROLE_admin')")
    public R<Void> adminEdit(@RequestBody BizRedeemCode req) {
        if (req.getId() == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "缺少ID");
        }
        if (req.getCoinAmount() != null && req.getCoinAmount() <= 0) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "兑换铜钱数必须大于0");
        }
        if (req.getMaxCount() != null && req.getMaxCount() < 1) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "可兑换次数至少为1");
        }
        if (req.getStampId() != null && req.getStampId() > 0 && stampDao.selectOneById(req.getStampId()) == null) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "赠送的邮票不存在");
        }
        // 只更新可编辑字段，避免覆盖 used_count
        BizRedeemCode entity = new BizRedeemCode();
        entity.setId(req.getId());
        entity.setCoinAmount(req.getCoinAmount());
        entity.setMaxCount(req.getMaxCount());
        entity.setStampId(req.getStampId() != null && req.getStampId() > 0 ? req.getStampId() : null);
        entity.setStatus(req.getStatus());
        entity.setSys002(LocalDateTime.now());
        redeemCodeDao.update(entity);
        // stampId=0 表示取消赠送邮票(flex update 忽略 null，需显式置空)
        if (req.getStampId() != null && req.getStampId() == 0) {
            redeemCodeDao.clearStamp(req.getId());
        }
        return R.ok();
    }

    @PostMapping("/admin/remove/{id}")
    @PreAuthorize("hasAuthority('biz:redeem:remove') or hasRole('ROLE_admin')")
    public R<Void> adminRemove(@PathVariable Long id) {
        redeemCodeDao.deleteById(id);
        return R.ok();
    }

    @GetMapping("/admin/logs")
    @PreAuthorize("hasAuthority('biz:redeem:list') or hasRole('ROLE_admin')")
    public R<PageResult<BizRedeemLog>> adminLogs(PageQuery pageQuery,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String code) {
        List<BizRedeemLog> list = redeemLogDao.selectAllWithUser(userId, code,
                pageQuery.getOffset(), pageQuery.getLimit());
        long total = redeemLogDao.countAll(userId, code);
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list));
    }

    /** 生成8位随机兑换码(去掉易混淆字符) */
    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
