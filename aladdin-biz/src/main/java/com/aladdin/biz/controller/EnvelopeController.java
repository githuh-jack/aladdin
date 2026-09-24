package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizEnvelopeDao;
import com.aladdin.biz.dao.BizUserEnvelopeItemDao;
import com.aladdin.biz.entity.BizEnvelope;
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
 * 信封控制器
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/envelope", "/biz/envelope"})
public class EnvelopeController {

    @Autowired
    private BizEnvelopeDao envelopeDao;

    @Autowired
    private BizUserEnvelopeItemDao userEnvelopeItemDao;

    @GetMapping("/shop")
    public R<List<BizEnvelope>> shopList() {
        return R.ok(envelopeDao.selectOnShelfList());
    }

    /** 我的信封(按模板聚合可用/已用数量) */
    @GetMapping("/mine")
    public R<List<Map<String, Object>>> mine() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(userEnvelopeItemDao.selectMyEnvelopeAgg(userId));
    }

    @GetMapping("/detail/{id}")
    public R<BizEnvelope> detail(@PathVariable Long id) {
        BizEnvelope env = envelopeDao.selectOneById(id);
        if (env == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        return R.ok(env);
    }

    // ===== 管理员接口 =====

    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:envelope:list') or hasRole('ROLE_admin')")
    public R<List<BizEnvelope>> adminList() {
        return R.ok(envelopeDao.selectAllList());
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasAuthority('biz:envelope:add') or hasRole('ROLE_admin')")
    public R<Void> add(@RequestBody BizEnvelope envelope) {
        envelope.setSys001(LocalDateTime.now());
        envelope.setSys005(1);
        envelope.setSys006("biz");
        envelopeDao.insert(envelope);
        return R.ok();
    }

    @PostMapping("/admin/edit")
    @PreAuthorize("hasAuthority('biz:envelope:edit') or hasRole('ROLE_admin')")
    public R<Void> edit(@RequestBody BizEnvelope envelope) {
        envelope.setSys002(LocalDateTime.now());
        envelopeDao.update(envelope);
        return R.ok();
    }

    @PostMapping("/admin/remove/{id}")
    @PreAuthorize("hasAuthority('biz:envelope:remove') or hasRole('ROLE_admin')")
    public R<Void> remove(@PathVariable Long id) {
        envelopeDao.deleteById(id);
        return R.ok();
    }
}
