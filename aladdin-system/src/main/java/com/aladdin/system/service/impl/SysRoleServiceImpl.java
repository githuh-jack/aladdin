package com.aladdin.system.service.impl;

import com.aladdin.common.core.domain.PageQuery;
import com.aladdin.common.core.domain.PageResult;
import com.aladdin.common.db.base.BaseServiceImpl;
import com.aladdin.system.dao.SysRoleDao;
import com.aladdin.system.entity.SysRole;
import com.aladdin.system.service.SysRoleService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static com.aladdin.system.entity.table.SysRoleTableDef.SYS_ROLE;

/**
 * 系统角色服务实现
 *
 * @author cles
 * @date 2026/05/06
 */
@Service
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleDao, SysRole> implements SysRoleService {

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return getMapper().selectRolesByUserId(userId);
    }

    @Override
    public SysRole getRoleByKey(String roleKey) {
        if (roleKey == null || roleKey.trim().isEmpty()) {
            return null;
        }
        return getMapper().selectOneByQuery(QueryWrapper.create()
                .where(SYS_ROLE.ROLE_KEY.eq(roleKey))
                .and(SYS_ROLE.SYS005.eq(1)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignResources(Long roleId, List<Long> resourceIds) {
        getMapper().deleteRoleResourceByRoleId(roleId);
        if (resourceIds != null && !resourceIds.isEmpty()) {
            getMapper().batchInsertRoleResource(roleId, resourceIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        getMapper().deleteUserRoleByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            getMapper().batchInsertUserRole(userId, roleIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDataScope(Long roleId, Integer dataScope, List<Long> deptIds) {
        SysRole role = getById(roleId);
        if (role == null) {
            return;
        }
        role.setDataScope(dataScope);
        updateById(role);
        getMapper().deleteRoleDeptByRoleId(roleId);
        if (deptIds != null && !deptIds.isEmpty() && dataScope == 2) {
            getMapper().batchInsertRoleDept(roleId, deptIds);
        }
    }

    @Override
    public List<Long> getDeptIdsByRoleId(Long roleId) {
        return getMapper().selectDeptIdsByRoleId(roleId);
    }

    @Override
    public PageResult<SysRole> listPage(PageQuery pageQuery, String roleName, Integer status) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(SYS_ROLE.SYS005.eq(1));
        if (roleName != null && !roleName.isEmpty()) {
            queryWrapper.and(SYS_ROLE.ROLE_NAME.like(roleName));
        }
        if (status != null) {
            queryWrapper.and(SYS_ROLE.STATUS.eq(status));
        }
        queryWrapper.orderBy(SYS_ROLE.SORT.asc());
        com.mybatisflex.core.paginate.Page<SysRole> page = getMapper()
                .paginate(pageQuery.getPage(), pageQuery.getLimit(), queryWrapper);
        return new PageResult<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), page.getRecords());
    }
}
