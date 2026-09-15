package com.aladdin.system.service;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.db.base.BaseService;
import com.aladdin.system.entity.SysRole;

import java.util.List;

/**
 * 系统角色服务接口
 *
 * @author cles
 * @date 2026/05/06
 */
public interface SysRoleService extends BaseService<SysRole> {

    List<SysRole> getRolesByUserId(Long userId);

    /** 按 roleKey 查询角色 */
    SysRole getRoleByKey(String roleKey);

    void assignResources(Long roleId, List<Long> resourceIds);

    void assignRoles(Long userId, List<Long> roleIds);

    void assignDataScope(Long roleId, Integer dataScope, List<Long> deptIds);

    List<Long> getDeptIdsByRoleId(Long roleId);

    /** 分页查询角色列表 */
    PageResult<SysRole> listPage(PageQuery pageQuery, String roleName, Integer status);
}
