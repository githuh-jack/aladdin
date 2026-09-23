package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizDiaryDao;
import com.aladdin.biz.entity.BizDiary;
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

/**
 * 日记控制器
 *
 * @author cles
 * @date 2026/09/15
 */
@RestController
@RequestMapping({"/diary", "/biz/diary"})
public class DiaryController {

    @Autowired
    private BizDiaryDao diaryDao;

    @Autowired
    private com.aladdin.biz.service.RewardService rewardService;

    /** 我的日记列表 */
    @GetMapping("/list")
    public R<List<BizDiary>> list(PageQuery pageQuery,
                                  @RequestParam(required = false) String keyWord) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return R.ok(diaryDao.selectListWithUser(userId, null, keyWord, false,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }

    /** 公开广场(仅当天) */
    @GetMapping("/public")
    public R<List<BizDiary>> publicList(PageQuery pageQuery,
                                        @RequestParam(required = false) String keyWord) {
        return R.ok(diaryDao.selectListWithUser(null, 1, keyWord, true,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }

    /** 详情 */
    @GetMapping("/detail/{id}")
    public R<BizDiary> detail(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        BizDiary diary = diaryDao.selectDetailById(id);
        if (diary == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        // 私密日记只有作者可见，admin 由安全框架放行
        if (Integer.valueOf(0).equals(diary.getIsPublic())
                && (userId == null || !diary.getUserId().equals(userId))) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        return R.ok(diary);
    }

    /** 新增 */
    @PostMapping
    public R<Void> save(@RequestBody BizDiary diary) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        diary.setUserId(userId);
        diary.setSys001(LocalDateTime.now());
        diary.setSys005(1);
        diary.setSys006("biz");
        diaryDao.insert(diary);
        // 当日写日记奖励 + 文竹生长(每天最多1cm)
        rewardService.onDiaryCreated(userId, diary.getId(), diary.getContent());
        return R.ok();
    }

    /** 修改 */
    @PostMapping("/edit")
    public R<Void> update(@RequestBody BizDiary diary) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizDiary existing = diaryDao.selectOneById(diary.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        diary.setUserId(userId);
        diary.setSys002(LocalDateTime.now());
        diaryDao.update(diary);
        return R.ok();
    }

    /** 删除 */
    @PostMapping("/remove/{id}")
    public R<Void> remove(@PathVariable Long id) {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        BizDiary existing = diaryDao.selectOneById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
        diaryDao.deleteById(id);
        return R.ok();
    }

    /** 管理员: 查询所有日记 */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:diary:list') or hasRole('ROLE_admin')")
    public R<List<BizDiary>> adminList(PageQuery pageQuery,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) String keyWord) {
        return R.ok(diaryDao.selectListWithUser(userId, null, keyWord, false,
                pageQuery.getOffset(), pageQuery.getLimit()));
    }
}
