package com.aladdin.system.pm.controller;

import com.aladdin.common.core.exception.BusinessException;
import com.aladdin.common.core.exception.GlobalErrorCode;
import com.aladdin.common.security.service.LoginService;
import com.aladdin.system.pm.dao.PmUserDao;
import com.aladdin.system.pm.entity.PmUser;
import org.springframework.stereotype.Component;

/**
 * 项目管理系统-身份辅助
 * pm_user 与 sys_user 相互独立：pm_user 主键从 200000 起，
 * 登录态 token 的 subject = 200000 + pm_user.id，与 sys_user(小id)、biz_user(10000起) 隔离。
 * 平台(sys_user)登录用户视为系统管理员，拥有项目管理系统全部权限。
 *
 * @author cles
 * @date 2026/11/02
 */
@Component
public class PmAuthHelper {

    /** pm_user id 在登录态中的偏移量(与 sys_user/biz_user 的 id 空间隔离) */
    public static final long PM_USER_ID_OFFSET = 200_000L;

    private final PmUserDao pmUserDao;

    public PmAuthHelper(PmUserDao pmUserDao) {
        this.pmUserDao = pmUserDao;
    }

    /** 当前登录是否为平台系统用户(sys_user)，平台用户拥有pm系统管理员权限 */
    public boolean isSysUser() {
        Long userId = LoginService.getCurrentUserId();
        return userId != null && userId < PM_USER_ID_OFFSET;
    }

    /** 获取当前登录的pm用户；平台用户返回null；未登录抛401 */
    public PmUser currentPmUser() {
        Long userId = LoginService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        if (userId < PM_USER_ID_OFFSET) {
            return null;
        }
        PmUser user = pmUserDao.selectOneById(userId - PM_USER_ID_OFFSET);
        if (user == null) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 校验当前用户具备指定角色之一(平台用户直接放行，pm admin 始终放行)
     *
     * @param roles 允许的角色: owner/product/manager/dev/tester
     */
    public void requireRole(String... roles) {
        PmUser user = currentPmUser();
        if (user == null) {
            return;
        }
        if ("admin".equals(user.getRole())) {
            return;
        }
        for (String role : roles) {
            if (role.equals(user.getRole())) {
                return;
            }
        }
        throw new BusinessException(GlobalErrorCode.FORBIDDEN.getCode(), "当前角色无权执行该操作");
    }

    /** 当前pm用户id(平台用户返回null) */
    public Long currentPmUserId() {
        PmUser user = currentPmUser();
        return user == null ? null : user.getId();
    }
}
