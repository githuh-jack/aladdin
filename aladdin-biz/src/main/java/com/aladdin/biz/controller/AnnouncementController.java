package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizAnnouncementDao;
import com.aladdin.biz.entity.BizAnnouncement;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告控制器
 * 用户：查看已发布公告
 * 管理员：CRUD 公告
 *
 * @author cles
 * @date 2026/09/24
 */
@RestController
@RequestMapping({"/announcement", "/biz/announcement"})
public class AnnouncementController {

    @Autowired
    private BizAnnouncementDao announcementDao;

    /** 已发布公告(用户登录后首页弹窗) */
    @GetMapping("/published")
    public R<List<BizAnnouncement>> published() {
        return R.ok(announcementDao.selectPublished());
    }

    // ===== 管理员接口 =====

    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('biz:announcement:list') or hasRole('ROLE_admin')")
    public R<List<BizAnnouncement>> adminList() {
        return R.ok(announcementDao.selectAllList());
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasAuthority('biz:announcement:add') or hasRole('ROLE_admin')")
    public R<Void> add(@RequestBody BizAnnouncement announcement) {
        if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
            return R.fail(GlobalErrorCode.BAD_REQUEST.getCode(), "公告标题不能为空");
        }
        announcement.setSys001(LocalDateTime.now());
        announcement.setSys005(1);
        announcement.setSys006("biz");
        announcementDao.insert(announcement);
        return R.ok();
    }

    @PostMapping("/admin/edit")
    @PreAuthorize("hasAuthority('biz:announcement:edit') or hasRole('ROLE_admin')")
    public R<Void> edit(@RequestBody BizAnnouncement announcement) {
        announcement.setSys002(LocalDateTime.now());
        announcementDao.update(announcement);
        return R.ok();
    }

    @PostMapping("/admin/remove/{id}")
    @PreAuthorize("hasAuthority('biz:announcement:remove') or hasRole('ROLE_admin')")
    public R<Void> remove(@PathVariable Long id) {
        announcementDao.deleteById(id);
        return R.ok();
    }
}
