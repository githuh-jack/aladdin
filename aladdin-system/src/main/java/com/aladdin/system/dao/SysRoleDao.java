package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysRole;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import static com.aladdin.system.entity.table.SysRoleTableDef.SYS_ROLE;

/**
 * 系统角色DAO
 *
 * @author cles
 * @date 2026/05/06
 */
public interface SysRoleDao extends BaseDao<SysRole> {

    /**
     * 查询用户角色(主表 sys005=1 由 flex 自动追加；关联表用字符串表名，避免逻辑删除条件破坏 JOIN)
     */
    default List<SysRole> selectRolesByUserId(Long userId) {
        return selectListByQuery(QueryWrapper.create()
                .from(SYS_ROLE)
                .innerJoin("sys_user_role")
                .on(SYS_ROLE.ID.eq(new QueryColumn("sys_user_role", "role_id")))
                .where(new QueryColumn("sys_user_role", "user_id").eq(userId)));
    }

    @Insert("<script>" +
            "INSERT INTO sys_role_resource (role_id, resource_id) VALUES " +
            "<foreach collection='resourceIds' item='resourceId' separator=','>" +
            "(#{roleId}, #{resourceId})" +
            "</foreach>" +
            "</script>")
    int batchInsertRoleResource(@Param("roleId") Long roleId, @Param("resourceIds") List<Long> resourceIds);

    @Delete("DELETE FROM sys_role_resource WHERE role_id = #{roleId}")
    int deleteRoleResourceByRoleId(@Param("roleId") Long roleId);

    @Insert("<script>" +
            "INSERT INTO sys_user_role (user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','>" +
            "(#{userId}, #{roleId})" +
            "</foreach>" +
            "</script>")
    int batchInsertUserRole(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoleByUserId(@Param("userId") Long userId);

    @Insert("<script>" +
            "INSERT INTO sys_role_dept (role_id, dept_id) VALUES " +
            "<foreach collection='deptIds' item='deptId' separator=','>" +
            "(#{roleId}, #{deptId})" +
            "</foreach>" +
            "</script>")
    int batchInsertRoleDept(@Param("roleId") Long roleId, @Param("deptIds") List<Long> deptIds);

    @Delete("DELETE FROM sys_role_dept WHERE role_id = #{roleId}")
    int deleteRoleDeptByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);
}
