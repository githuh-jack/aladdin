package com.aladdin.system.service;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.db.base.BaseService;
import com.aladdin.system.entity.SysUser;

import java.util.Set;

/**
 * 系统用户服务接口
 *
 * @author cles
 * @date 2026/05/06
 */
public interface SysUserService extends BaseService<SysUser> {

    SysUser getByUsername(String username);

    Set<String> getRoleKeysByUserId(Long userId);

    Set<String> getPermsByUserId(Long userId);

    /**
     * 获取系统中所有有效的权限标识（用于超级管理员放行）。
     */
    Set<String> getAllPerms();

    SysUser getUserWithDeptById(Long id);

    boolean resetPassword(Long id, String password);

    boolean updateStatus(Long id, Integer status);

    /** 修改密码，校验不能与最近N次相同 */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /** 修改个人信息 */
    boolean updateProfile(Long userId, String nickname, String email, String phone, String avatar);

    /** 检查密码是否过期 */
    boolean isPasswordExpired(SysUser user);

    /** 检查是否需要强制修改密码 */
    boolean needForceChangePassword(SysUser user);

    /** 分页查询用户列表 */
    PageResult<SysUser> listPage(PageQuery pageQuery, String username, Integer status, Long deptId);

    /**
     * 保存用户多部门关联(第一个为主部门，同步更新sys_user.dept_id)
     */
    void saveUserDepts(Long userId, java.util.List<Long> deptIds);
}
