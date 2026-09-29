package com.aladdin.biz.controller;

import com.aladdin.biz.dao.BizUserDao;
import com.aladdin.biz.entity.BizUser;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.core.domain.R;
import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.core.domain.PageQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 传信纸船用户管理(管理端)
 *
 * @author cles
 * @date 2026/09/28
 */
@RestController
@RequestMapping("/biz/user")
public class BizUserController {

    @Autowired
    private BizUserDao userDao;

    /**
     * 管理员: 用户列表(支持 账号/笔名/编号 关键字，不返回密码)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<PageResult<BizUser>> adminList(PageQuery pageQuery,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status) {
        List<BizUser> list = userDao.selectAdminList(keyword, status,
                pageQuery.getOffset(), pageQuery.getLimit());
        long total = userDao.countAdminList(keyword, status);
        list.forEach(u -> u.setPassword(null));
        return R.ok(new PageResult<>(pageQuery.getPage(), pageQuery.getLimit(), total, list));
    }

    /** 管理员: 冻结用户(状态置0，无法登录) */
    @PostMapping("/admin/freeze/{id}")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> freeze(@PathVariable Long id) {
        BizUser user = userDao.selectOneById(id);
        if (user == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        BizUser update = new BizUser();
        update.setId(id);
        update.setStatus(0);
        userDao.update(update);
        return R.ok();
    }

    /** 管理员: 解冻用户(状态置1) */
    @PostMapping("/admin/unfreeze/{id}")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> unfreeze(@PathVariable Long id) {
        BizUser user = userDao.selectOneById(id);
        if (user == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        BizUser update = new BizUser();
        update.setId(id);
        update.setStatus(1);
        userDao.update(update);
        return R.ok();
    }

    /** 管理员: 删除用户(全局逻辑删除) */
    @PostMapping("/admin/remove/{id}")
    @PreAuthorize("hasRole('ROLE_admin')")
    public R<Void> remove(@PathVariable Long id) {
        BizUser user = userDao.selectOneById(id);
        if (user == null) {
            throw new BusinessException(GlobalErrorCode.DATA_NOT_FOUND);
        }
        userDao.deleteById(id);
        return R.ok();
    }
}
