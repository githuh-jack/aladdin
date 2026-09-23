package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizNoteDao;
import com.aladdin.biz.entity.BizNote;
import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 其他笔记控制器(其他东西)
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/note", "/biz/note"})
public class NoteController {

    @Autowired
    private BizNoteDao noteDao;

    @GetMapping("/list")
    public R<List<BizNote>> list(PageQuery pageQuery,
                                 @RequestParam(required = false) String keyWord) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(noteDao.selectListWithUser(userId, null, keyWord, userId,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }

    @GetMapping("/public")
    public R<List<BizNote>> publicList(PageQuery pageQuery,
                                       @RequestParam(required = false) String keyWord) {
        return R.ok(noteDao.selectListWithUser(null, 1, keyWord, LoginService.getCurrentUserId(),
                pageQuery.getOffset(), pageQuery.getLimit()));
    }

    @GetMapping("/detail/{id}")
    public R<BizNote> detail(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        BizNote note = noteDao.selectDetailById(id);
        if (note == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        if (Integer.valueOf(0).equals(note.getIsPublic())
                && (userId == null || !note.getUserId().equals(userId))) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        return R.ok(note);
    }

    @PostMapping
    public R<Void> save(@RequestBody BizNote note) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        note.setUserId(userId);
        note.setSys001(LocalDateTime.now());
        note.setSys005(1);
        note.setSys006("biz");
        noteDao.insert(note);
        return R.ok();
    }

    @PostMapping("/edit")
    public R<Void> update(@RequestBody BizNote note) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizNote existing = noteDao.selectOneById(note.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        note.setUserId(userId);
        note.setSys002(LocalDateTime.now());
        noteDao.update(note);
        return R.ok();
    }

    @PostMapping("/remove/{id}")
    public R<Void> remove(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizNote existing = noteDao.selectOneById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        noteDao.deleteById(id);
        return R.ok();
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:note:list') or hasRole('ROLE_admin')")
    public R<List<BizNote>> adminList(PageQuery pageQuery,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) String keyWord) {
        return R.ok(noteDao.selectListWithUser(userId, null, keyWord, null,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }
}
