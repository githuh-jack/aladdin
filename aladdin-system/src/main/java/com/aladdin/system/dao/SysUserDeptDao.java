package com.aladdin.system.dao;

import com.aladdin.common.db.base.BaseDao;
import com.aladdin.system.entity.SysUserDept;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 用户部门关联DAO
 * 关联表为重建式写入：删除用物理删（逻辑删会与 uk_user_dept 唯一键冲突），
 * 新增用 ON DUPLICATE KEY UPDATE（flex 原生 insert 不支持 upsert），故保留注解 SQL。
 *
 * @author cles
 * @date 2026/09/23
 */
public interface SysUserDeptDao extends BaseDao<SysUserDept> {

    /** 物理删除用户的全量部门关联 */
    @Delete("DELETE FROM sys_user_dept WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 插入用户部门关联（唯一键冲突时仅更新主部门标记） */
    @Insert("INSERT INTO sys_user_dept (user_id, dept_id, is_primary, sys001, sys003, sys005, sys006) " +
            "VALUES (#{userId}, #{deptId}, #{isPrimary}, #{sys001}, #{sys003}, 1, 'system') " +
            "ON DUPLICATE KEY UPDATE is_primary = VALUES(is_primary)")
    int insertOnDuplicate(@Param("userId") Long userId,
                          @Param("deptId") Long deptId,
                          @Param("isPrimary") Integer isPrimary,
                          @Param("sys001") LocalDateTime sys001,
                          @Param("sys003") Long sys003);
}
