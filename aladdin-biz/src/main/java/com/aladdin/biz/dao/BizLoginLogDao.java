package com.aladdin.biz.dao;

import com.aladdin.biz.entity.BizLoginLog;
import com.aladdin.common.db.base.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户登录日志DAO
 *
 * @author cles
 * @date 2026/09/20
 */
public interface BizLoginLogDao extends BaseDao<BizLoginLog> {

    /** 登录日志列表(关联用户昵称) */
    @Select("<script>" +
            "SELECT l.*, u.nickname AS nick_name FROM biz_login_log l " +
            "LEFT JOIN sys_user u ON l.user_id = u.id " +
            "WHERE l.sys005 = 1 " +
            "<if test='userId != null'> AND l.user_id = #{userId}</if>" +
            "<if test='loginName != null and loginName != \"\"'> AND l.login_name LIKE CONCAT('%', #{loginName}, '%')</if>" +
            " ORDER BY l.login_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> selectLogsWithUser(@Param("userId") Long userId,
                                                 @Param("loginName") String loginName,
                                                 @Param("offset") long offset,
                                                 @Param("limit") long limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM biz_login_log l WHERE l.sys005 = 1 " +
            "<if test='userId != null'> AND l.user_id = #{userId}</if>" +
            "<if test='loginName != null and loginName != \"\"'> AND l.login_name LIKE CONCAT('%', #{loginName}, '%')</if>" +
            "</script>")
    long countLogs(@Param("userId") Long userId, @Param("loginName") String loginName);
}
